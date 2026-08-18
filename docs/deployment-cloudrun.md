# Deployment auf Google Cloud Run — Schritt für Schritt

Der dritte Weg neben Oracle Cloud ([`deployment.md`](deployment.md)) und Northflank
([`deployment-northflank.md`](deployment-northflank.md), dort ungeeignet befunden). Cloud Run
skaliert bei Inaktivität auf null Instanzen herunter — dadurch bleibt eine kleine App wie diese
dauerhaft im echten Freikontingent.

Zwei Bausteine, zwei Anbieter:

| Baustein | Läuft bei | Warum |
| :--- | :--- | :--- |
| Datenbank | **Neon** (externes, serverloses PostgreSQL) | Cloud-Run-Instanzen sind zustandslos und verschwinden bei Null-Traffic komplett — eine PostgreSQL darin würde bei jedem Scale-to-Zero ihre Daten verlieren. Googles verwalteter Cloud-SQL-Dienst wäre die Alternative, kostet aber ohne Freikontingent ab ca. 8 €/Monat. |
| Backend **und** Frontend | **Cloud Run**, ein einziger Dienst | [`Dockerfile.cloudrun`](../Dockerfile.cloudrun) bettet die fertig gebaute Angular-SPA direkt ins Spring-Boot-JAR ein — Spring liefert sie selbst aus. Ein Container, kein nginx, kein zweiter Dienst. |

**Warum kein Firebase Hosting davor:** Naheliegend wäre gewesen, wie bei Northflank einen
Frontend-Dienst vor die API zu stellen — bei Cloud Run böte sich dafür Firebase Hosting mit einer
Rewrite-Regel an. Beim Ausprobieren stellte sich aber heraus, dass Firebase Hosting aus
Caching-Gründen **alle Cookies bis auf ein einziges, fest benanntes `__session`-Cookie verwirft**,
bevor die Anfrage bei Cloud Run ankommt (offiziell dokumentiert:
https://firebase.google.com/docs/hosting/manage-cache). Unsere Sitzungsverwaltung nutzt aber zwei
Cookies (`JSESSIONID`, `XSRF-TOKEN`) — beide wären auf dem Weg verloren gegangen, die Anmeldung
wäre nie über eine Folgeanfrage hinaus gültig geblieben. Ein einzelner Cloud-Run-Dienst umgeht das
Problem, statt es zu umschiffen.

Für die Technik dahinter (Cookies, `X-Forwarded-Proto`) siehe
[Entwickler-Doku](entwicklerdoku.md#betrieb-im-netz).

---

## Alle Links auf einen Blick

| Wofür | Link |
| :--- | :--- |
| Neon-Konto anlegen | https://console.neon.tech/signup |
| Neon-Dashboard | https://console.neon.tech/app/projects |
| GCP-Projekt anlegen | https://console.cloud.google.com/projectcreate |
| GCP-Abrechnungskonto | https://console.cloud.google.com/billing |
| Budget-Warnung einrichten (empfohlen) | https://console.cloud.google.com/billing/budgets |
| Cloud Run API aktivieren | https://console.cloud.google.com/apis/library/run.googleapis.com |
| Artifact Registry API aktivieren | https://console.cloud.google.com/apis/library/artifactregistry.googleapis.com |
| Artifact-Registry-Repository anlegen | https://console.cloud.google.com/artifacts |
| Dienstkonten (Service Accounts) | https://console.cloud.google.com/iam-admin/serviceaccounts |
| Cloud-Run-Dienste ansehen | https://console.cloud.google.com/run |
| GitHub Secrets anlegen | https://github.com/leonid-ai-teach/pizza-shop/settings/secrets/actions/new |
| Workflow manuell auslösen | https://github.com/leonid-ai-teach/pizza-shop/actions/workflows/deploy-cloudrun.yml |
| Cloud-Run-Freikontingent | https://cloud.google.com/run/pricing |
| Neon-Freikontingent | https://neon.tech/pricing |

---

## Was am Ende läuft

```
Browser ──HTTPS──> Cloud Run: pizza-shop-backend (Spring Boot, liefert API UND Angular-SPA)
                          │                                │
                          │ /api/**                        │ alles andere
                          ▼                                ▼
                    REST-Controller                statische Angular-Dateien
                    (aus dem JAR)                   (eingebettet im selben JAR)
                          │
                          ▼
                    Neon (PostgreSQL, extern)

git push ──> ci-cd.yml testet automatisch
Actions → "Run workflow" bei deploy-cloudrun.yml ──> baut Dockerfile.cloudrun, rollt aus
```

Erreichbar über die automatisch von Google vergebene Adresse
`https://pizza-shop-backend-xxxxxxxxxx-xx.a.run.app` — kostenlos, produktionsreif, aber keine
eigene Domain. Eine eigene Domain bräuchte entweder Firebase Hosting (bringt das Cookie-Problem
von oben zurück) oder einen kostenpflichtigen Load Balancer; Cloud Runs eigenes Domain-Mapping ist
laut Google selbst noch "Preview" und nicht für den Produktivbetrieb empfohlen.

---

## Schritt 1: Neon-Konto und Datenbank

1. **https://console.neon.tech/signup** — Anmeldung mit GitHub ist am schnellsten.
2. Neues Projekt anlegen. Region: **AWS Frankfurt (eu-central-1)** wählen — liegt am nächsten an
   der Cloud-Run-Region aus Schritt 4.
3. Im Dashboard unter **Connection Details** die **gepoolte** Verbindung anzeigen lassen (Schalter
   „Pooled connection" bzw. Endpunkt mit `-pooler` im Namen) — wichtig, weil Cloud Run bei Bedarf
   mehrere Instanzen gleichzeitig startet, die sich sonst zu viele einzelne Datenbankverbindungen
   teilen müssten.
4. Der angezeigte Connection-String sieht etwa so aus:
   ```
   postgresql://BENUTZER:PASSWORT@ep-xxxxx-pooler.eu-central-1.aws.neon.tech/DBNAME?sslmode=require
   ```
   Diesen String in drei Teile zerlegen — Spring braucht Adresse, Benutzername und Passwort
   **getrennt**, weil `spring.datasource.username`/`.password` einen im Verbindungsstring
   enthaltenen Benutzernamen/Passwort immer überschreiben:

   | Secret (Schritt 6) | Wert |
   | :--- | :--- |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-xxxxx-pooler.eu-central-1.aws.neon.tech/DBNAME?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | `BENUTZER` |
   | `SPRING_DATASOURCE_PASSWORD` | `PASSWORT` |

Neons kostenlose Stufe legt die Rechenleistung nach ein paar Minuten Inaktivität schlafen und
weckt sie bei der nächsten Anfrage automatisch wieder auf (ca. 1 Sekunde Verzögerung) —
Freikontingent: https://neon.tech/pricing

---

## Schritt 2: Google-Cloud-Projekt anlegen

**https://console.cloud.google.com/projectcreate** — Name z. B. `pizza-shop`. Die dabei erzeugte
**Projekt-ID** (nicht der Name!) notieren — sie steht direkt unter dem Namensfeld und wird ab hier
an mehreren Stellen gebraucht.

> **Achtung, Kreditkarte nötig:** Cloud Run setzt ein mit Zahlungsmittel verknüpftes
> Abrechnungskonto voraus — auch wenn man innerhalb des Freikontingents bleibt und nichts
> berechnet wird. Unter **https://console.cloud.google.com/billing** ein Konto anlegen bzw. mit
> dem Projekt verknüpfen, falls noch keins existiert. Um sicher nichts zu übersehen, direkt eine
> Budget-Warnung einrichten: **https://console.cloud.google.com/billing/budgets** → „Budget
> erstellen" → z. B. 1 € Schwellenwert mit E-Mail-Benachrichtigung.

## Schritt 3: Benötigte APIs aktivieren

Für das gerade angelegte Projekt (oben in der Konsole als aktives Projekt auswählen, falls
gefragt) jeweils auf **Aktivieren** klicken:

- **https://console.cloud.google.com/apis/library/run.googleapis.com**
- **https://console.cloud.google.com/apis/library/artifactregistry.googleapis.com**

## Schritt 4: Artifact-Registry-Repository anlegen

**https://console.cloud.google.com/artifacts** → **Repository erstellen**

| Feld | Wert |
| :--- | :--- |
| Name | `pizza-shop` |
| Format | Docker |
| Modus | Standard |
| Region | **europe-west3** (Frankfurt) — muss zur Cloud-Run-Region passen, siehe Workflow |

## Schritt 5: Dienstkonto für GitHub Actions

**https://console.cloud.google.com/iam-admin/serviceaccounts** → **Dienstkonto erstellen**

1. Name: `github-deploy`.
2. Rollen zuweisen (drei Stück, alle nötig):
   - **Cloud Run Admin** (`roles/run.admin`)
   - **Artifact Registry-Autor** / Artifact Registry Writer (`roles/artifactregistry.writer`)
   - **Dienstkontonutzer** / Service Account User (`roles/iam.serviceAccountUser`)
3. Dienstkonto öffnen → Tab **Keys** → **Add Key** → **JSON** → Datei wird heruntergeladen.

Diese JSON-Datei ist ein Geheimnis (voller Zugriff im Rahmen der drei Rollen). Direkt in Schritt 6
als GitHub-Secret einfügen und die heruntergeladene Datei danach an einem geschützten Ort ablegen
oder löschen — sie darf nirgendwo offen liegen bleiben.

## Schritt 6: Secrets in GitHub hinterlegen

Anlegen unter **https://github.com/leonid-ai-teach/pizza-shop/settings/secrets/actions/new** —
sechsmal, für jedes Secret einmal:

| Name | Inhalt |
| :--- | :--- |
| `GCP_SA_KEY` | kompletter Inhalt der JSON-Datei aus Schritt 5 |
| `GCP_PROJECT_ID` | die Projekt-ID aus Schritt 2 |
| `SPRING_DATASOURCE_URL` | aus Schritt 1 |
| `SPRING_DATASOURCE_USERNAME` | aus Schritt 1 |
| `SPRING_DATASOURCE_PASSWORD` | aus Schritt 1 |
| `ADMIN_BOOTSTRAP_EMAIL` | deine E-Mail |
| `ADMIN_BOOTSTRAP_PASSWORD` | ein Passwort mit mindestens 10 Zeichen — in PowerShell erzeugen: `-join ((48..57)+(65..90)+(97..122)|Get-Random -Count 24|%{[char]$_})` |

## Schritt 7: Ausrollen und testen

1. **https://github.com/leonid-ai-teach/pizza-shop/actions/workflows/deploy-cloudrun.yml** →
   **Run workflow** → auf `master` ausführen.
2. Der Lauf baut [`Dockerfile.cloudrun`](../Dockerfile.cloudrun) (Angular + Spring Boot in einem
   Image), pusht es nach Artifact Registry und rollt es nach Cloud Run aus.
3. Die Adresse steht danach in der Konsole: **https://console.cloud.google.com/run** →
   `pizza-shop-backend` → oben auf der Seite.

> **Prüfen:** Die Cloud-Run-Adresse öffnen — die Speisekarte muss laden. Danach `/admin` mit
> `ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD` aus Schritt 6 anmelden.

---

## Updates ausrollen

Bewusst **kein** automatisches Ausrollen bei jedem Push: Solange noch nicht entschieden ist, ob
Cloud Run dauerhaft genutzt wird, soll kein Push versehentlich zwei Ziele gleichzeitig bespielen
(Oracle über `ci-cd.yml`, Cloud Run über `deploy-cloudrun.yml`). Stattdessen manuell über
**Run workflow** unter dem Link aus Schritt 7. Steht die Entscheidung für Cloud Run fest, genügt
in `.github/workflows/deploy-cloudrun.yml` der Wechsel von

```yaml
on:
  workflow_dispatch:
```

zu

```yaml
on:
  push:
    branches: [master]
```

Flyway wandert beim Start des Backends automatisch auf den neuen Schemastand — genau wie bei den
anderen beiden Wegen.

---

## Wenn es klemmt

| Symptom | Ursache |
| :--- | :--- |
| Workflow bricht bei "Nach Cloud Run ausrollen" mit `PERMISSION_DENIED` ab | Dienstkonto hat nicht alle drei Rollen aus Schritt 5 — insbesondere `Service Account User` wird oft vergessen |
| `docker push` schlägt mit `unauthorized`/`denied` fehl | Artifact Registry API nicht aktiviert (Schritt 3), oder Region im Workflow (`europe-west3`) passt nicht zur Region des Repositorys (Schritt 4) |
| Seite lädt, aber `/api/pizzas` liefert 404 | Sollte mit `Dockerfile.cloudrun` nicht mehr vorkommen (ein Prozess, kein Routing dazwischen) — falls doch, `docker build -f Dockerfile.cloudrun .` lokal nachvollziehen |
| Direkter Aufruf einer Angular-Route (z. B. `/admin/login` per Lesezeichen oder Neuladen) liefert 404 statt der Seite | `GlobalExceptionHandler.handleMissingResource` prüfen — fängt `NoResourceFoundException` ab und liefert `index.html` aus, außer der Pfad beginnt mit `/api/` |
| Admin-Login setzt keine Sitzung | Workflow prüfen — `SERVER_FORWARD_HEADERS_STRATEGY=native` muss unter den `env_vars` beim Cloud-Run-Deploy-Schritt stehen |
| Flyway-Fehler beim Start, Verbindung schlägt fehl | `SPRING_DATASOURCE_URL` enthält noch Benutzername/Passwort statt sie getrennt zu setzen (Schritt 1), oder `sslmode=require` fehlt |
| Erste Anfrage nach längerer Pause dauert spürbar | normal — sowohl Cloud Run (JVM-Kaltstart) als auch Neon (Autosuspend) fahren bei Inaktivität herunter |
| "Budget exceeded"-Mail von Google | die Budget-Warnung aus Schritt 2 hat angeschlagen — meist reicht ein Blick auf https://console.cloud.google.com/billing, ob eine Cloud-Run-Instanz dauerhaft läuft statt zu skalieren |

---

## Warum dieser Weg neben Oracle und Northflank

Cloud Run bietet ein echtes Freikontingent ohne künstliche Größenbeschränkung wie bei Northflank
(2 Mio. Anfragen, 180.000 vCPU-Sekunden, 360.000 GB-Sekunden pro Monat —
https://cloud.google.com/run/pricing) und braucht anders als Oracle keine Wartezeit auf
Kapazität. Der Preis: ein Kaltstart nach Inaktivität (JVM-Start dauert ein paar Sekunden), ein
verknüpftes Zahlungsmittel bei Google (auch ohne Berechnung), und — anders als beim
Docker-Compose-Aufbau der anderen beiden Wege — kein eigenständiger Frontend-Dienst, weil das an
dieser Stelle (Firebase Hosting) mehr Probleme gebracht hätte, als es gelöst hat. Verglichen mit
Oracle: keine eigene Maschine zu pflegen, keine Sicherheitsupdates am Betriebssystem, dafür
weniger Kontrolle und vorerst keine eigene Domain.
