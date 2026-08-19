# Architektur des Cloud-Run-Deployments

Technischer Bericht zum dritten Deployment-Weg (neben Oracle Cloud und dem verworfenen
Northflank-Versuch): was läuft, wie es zusammenhängt, und welche Entscheidungen unterwegs
gefallen sind. Für die Schritt-für-Schritt-Anleitung siehe
[`deployment-cloudrun.md`](deployment-cloudrun.md); dieser Bericht erklärt das **Warum**.

---

## Überblick

```
Browser ──HTTPS──> pizza-shop.duckdns.org (DNS: DuckDNS, Zertifikat: Google-verwaltet)
                          │
                          ▼
                    Cloud Run: pizza-shop-backend (europe-west1)
                    ein Container, ein Prozess (Spring Boot)
                          │
                    ┌─────┴─────┐
                    │           │
              /api/**       alles andere
                    │           │
                    ▼           ▼
            REST-Controller   statische Angular-Dateien
            (aus dem JAR)     (eingebettet im selben JAR)
                    │
                    ▼
            Neon (PostgreSQL, extern, eu-central-1)
```

Drei externe Bausteine, keiner davon zufällig gewählt:

| Baustein | Anbieter | Warum dort und nicht anders |
| :--- | :--- | :--- |
| Rechenzeit | Cloud Run | skaliert bei Nulltraffic auf null Instanzen, echtes Freikontingent |
| Datenbank | Neon (PostgreSQL) | Cloud-Run-Instanzen sind zustandslos; eine DB im selben Container würde bei jedem Scale-to-Zero ihre Daten verlieren |
| Domain/DNS | DuckDNS | kostenlose Subdomain, unterstützt die A/AAAA-Einträge, die Cloud Runs Domain-Mapping verlangt |

---

## Eine wichtige Abzweigung: warum kein separater Frontend-Dienst

Der naheliegende erste Entwurf folgte demselben Muster wie Oracle und Northflank: ein
Frontend-Dienst liefert die Angular-SPA aus und reicht `/api/**` an einen Backend-Dienst weiter.
Bei Cloud Run böte sich dafür **Firebase Hosting** mit einer Rewrite-Regel an — genau das wurde
zuerst gebaut und in `firebase.json` konfiguriert.

Beim Testen zeigte sich ein hartes, dokumentiertes Limit: Firebase Hosting verwirft beim
Weiterleiten an Cloud Run **alle Cookies außer einem einzigen, fest benannten `__session`-Cookie**
(https://firebase.google.com/docs/hosting/manage-cache) — aus CDN-Cache-Gründen, nicht
konfigurierbar. Die Anwendung nutzt aber zwei Cookies (`JSESSIONID` für die Sitzung,
`XSRF-TOKEN` für CSRF-Schutz). Ergebnis: der Login-Request selbst gelang (`204 No Content`,
Cookies wurden korrekt gesetzt), aber jede Folgeanfrage verlor die Sitzung — verifiziert per
`curl` mit Cookie-Jar direkt gegen Cloud Run (funktionierte) versus über die Firebase-Hosting-
Adresse (schlug fehl, neue `JSESSIONID` bei jeder Anfrage).

Zwei denkbare Auswege wurden verworfen:

- **`JSESSIONID` auf `__session` umbenennen.** Hätte den Login repariert, aber `XSRF-TOKEN` bliebe
  weiterhin blockiert — alle anderen Admin-Aktionen (Pizzen/Toppings bearbeiten, Admins einladen)
  wären weiterhin kaputt geblieben. Keine echte Lösung, nur eine Verschiebung des Problems.
- **Auf tokenbasierte Authentifizierung umstellen** (Googles offiziell empfohlenes Muster für
  genau diese Kombination: ein Token im `Authorization`-Header statt Cookies, da Header von
  Firebase Hostings Cookie-Filter nicht betroffen sind). Architektonisch sauber, aber ein Umbau
  der gesamten, bereits mit 87 Backend-Tests abgesicherten Session-basierten Authentifizierung
  nur wegen eines Hosting-Details — unverhältnismäßig für dieses Projekt.

Stattdessen: **Firebase Hosting ganz weglassen.** [`Dockerfile.cloudrun`](../Dockerfile.cloudrun)
baut die Angular-SPA und bettet das Ergebnis direkt in das Spring-Boot-JAR ein
(`src/main/resources/static`) — Spring liefert sie selbst aus, genau wie es sonst nginx täte. Ein
Container, kein Routing dazwischen, keine CDN-Schicht, die Cookies filtern könnte. Die bestehende
Sitzungslogik blieb dadurch **unverändert**.

Zwei Code-Anpassungen waren dafür nötig:

1. **`GlobalExceptionHandler.handleMissingResource`** fängt `NoResourceFoundException` ab: Spring
   liefert nur Pfade aus, die als Datei existieren. Ein direkter Aufruf/Neuladen einer
   Angular-Route wie `/admin/login` (keine echte Datei) würde sonst als 404 durchfallen — der
   Handler liefert stattdessen `index.html` mit `200 OK` aus, außer der Pfad beginnt mit `/api/`
   (dort bleibt es ein echtes JSON-404).
2. **`SecurityConfig`** brauchte eine neue Regel `GET /** → permitAll()`. Ohne separaten
   Frontend-Dienst laufen jetzt auch Anfragen nach `/`, den JS/CSS-Dateien und jeder Angular-Route
   durch Spring Securitys Filterkette — vorher sah Spring diese Pfade nie (nginx/Firebase Hosting
   fingen sie ab). Sicherheitsrelevant ist das nicht: die Regel liefert nur öffentliche, statische
   Inhalte aus; die eigentliche Absicherung bleibt bei `/api/admin/**` mit
   `hasAuthority(AdminPrincipals.ROLE_ADMIN)`, unverändert vor dieser Regel platziert.

---

## Regionswahl: europe-west1 statt europe-west3

Der Dienst läuft in **europe-west1** (Belgien), nicht im naheliegenderen `europe-west3`
(Frankfurt, wie bei Neon und ursprünglich auch hier). Grund: Cloud Runs natives Domain-Mapping
unterstützt nur eine Teilmenge der Regionen, Frankfurt gehört nicht dazu
(https://docs.cloud.google.com/run/docs/mapping-custom-domains). Ohne diesen Umzug wäre nur die
technische `*.run.app`-Adresse nutzbar gewesen, keine eigene Domain. Die zusätzliche Latenz zu
Neon in Frankfurt ist bei einer App dieser Größe nicht relevant.

## Eigene Domain: Cloud-Run-Domain-Mapping + DuckDNS

Cloud Run selbst stellt für eine zugeordnete Domain vier A- und vier AAAA-Einträge bereit
(Anycast-Redundanz über Googles Netz). DuckDNS ist als klassischer Dynamic-DNS-Dienst nur für
**einen** A- und einen AAAA-Eintrag pro Subdomain ausgelegt — ein einzelner davon genügt aber
für volle Funktionalität, nur Googles interne Lastverteilung über mehrere Kanten-IPs entfällt.
Domain-Besitz wird einmalig über einen TXT-Eintrag bei Google Search Console bestätigt; danach
stellt Google automatisch ein verwaltetes TLS-Zertifikat aus (Provisionierung dauerte in der
Praxis wenige Minuten).

Cloud Runs Domain-Mapping ist laut Google selbst noch **„Preview"**, offiziell nicht für
Produktivbetrieb mit hohem Traffic empfohlen (mögliche Latenz-Einschränkungen) — für ein Projekt
dieser Größe unproblematisch.

## Mehrere Instanzen und Sitzungen

Spring hält die HTTP-Sitzung standardmäßig nur im Arbeitsspeicher der jeweiligen Instanz (keine
geteilte Session-Datenbank). Cloud Run kann bei Bedarf mehrere Instanzen parallel starten — eine
Folgeanfrage nach dem Login könnte dann auf einer Instanz landen, die die Sitzung nicht kennt.
Zwei Gegenmaßnahmen im Workflow, bewusst beide statt nur einer:

- `--session-affinity`: routet wiederkehrende Anfragen bevorzugt zur selben Instanz (best effort,
  keine Garantie laut Google-Dokumentation).
- `--max-instances=1`: macht das Problem bei der Größe dieser App strukturell unmöglich statt nur
  unwahrscheinlich, ohne Kosten (eine Instanz reicht für den erwarteten Traffic bei Weitem).

Bei spürbar mehr Traffic wäre ein geteilter Sitzungsspeicher (z. B. Spring Session mit der
Neon-Datenbank als Backend) die robustere, aber aufwändigere Lösung — für dieses Projekt aktuell
nicht nötig.

---

## CI/CD

[`deploy-cloudrun.yml`](../.github/workflows/deploy-cloudrun.yml): ein Job, `workflow_dispatch`
(manueller Auslöser). Baut `Dockerfile.cloudrun` aus dem Repository-Root (braucht sowohl
`backend/` als auch `frontend/`), pusht nach Artifact Registry, rollt nach Cloud Run aus.

Bewusst **kein** automatischer Trigger bei jedem Push: Der Oracle-Weg (`ci-cd.yml`) reagiert
bereits auf jeden Push zu `master`. Ein zusätzlicher automatischer Trigger hier würde ohne
Absprache zwei Deployment-Ziele gleichzeitig bespielen. `ci-cd.yml`s eigener Ausroll-Schritt
überspringt sich inzwischen selbst, wenn `DEPLOY_HOST` nicht gesetzt ist — solange also keine
Oracle-Maschine existiert, kollidiert ohnehin nichts.

---

## Betriebskosten in der Praxis

Zwei Kostenpunkte tauchten während der Einrichtung unerwartet auf, beide behoben:

- **Artifact Registry: automatisches Schwachstellen-Scannen** (Container Scanning API) scannt
  jedes neu gepushte Image einmalig, kostenpflichtig seit einiger Zeit. Bei den vielen Testläufen
  während der Fehlersuche kamen so ca. 1,60 € zusammen. Abgeschaltet über die API-Verwaltung
  (`containerscanning.googleapis.com` deaktivieren).
- **Artifact-Registry-Speicher**: jeder Push legt ein komplettes neues Image plus ein separates
  Attestation-Manifest an, Freikontingent sind 0,5 GB pro Abrechnungskonto. Eine
  Bereinigungsrichtlinie (5 neueste Versionen behalten, ungetaggte sofort löschen, getaggte nach
  30 Tagen) hält das dauerhaft im Rahmen.

Ansonsten bewegt sich der laufende Betrieb im echten Google-Cloud-Freikontingent (2 Mio.
Anfragen, 180.000 vCPU-Sekunden, 360.000 GB-Sekunden pro Monat,
https://cloud.google.com/run/pricing) — für den erwarteten Traffic dieser App dauerhaft
kostenlos, abgesehen vom anfänglichen Google-Cloud-Startguthaben, das die Testphase ohnehin
abgedeckt hätte.

---

## Offene Punkte / bekannte Grenzen

- **Kaltstart nach Inaktivität**: sowohl Cloud Run (JVM-Start) als auch Neon (Autosuspend) fahren
  bei längerer Pause herunter, die erste Anfrage danach dauert spürbar länger.
- **Nur eine Instanz** (`--max-instances=1`): ausreichend für den aktuellen Zweck, aber eine
  bewusste Grenze, keine automatische Lastverteilung bei Lastspitzen.
- **Domain-Mapping ist Google-seitig „Preview"**: siehe oben, für diese Größenordnung
  unproblematisch, aber nicht das, was Google für Produktivsysteme mit hohem Traffic empfiehlt.
- **Manueller Deploy-Trigger**: kein automatisches Ausrollen bei Push, solange nicht entschieden
  ist, ob dieser Weg dauerhaft genutzt wird (siehe CI/CD oben).

Verwandte Dokumente: [Schritt-für-Schritt-Anleitung](deployment-cloudrun.md),
[Entwickler-Doku zu Cookies/`X-Forwarded-Proto`](entwicklerdoku.md#betrieb-im-netz),
[Oracle-Weg](deployment.md), [Northflank-Versuch (verworfen)](deployment-northflank.md).
