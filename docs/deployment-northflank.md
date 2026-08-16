> **Status: nicht geeignet für den Backend-Dienst dieses Projekts, ohne zu bezahlen.**
> Beim Durchgehen dieser Anleitung stellte sich heraus, dass der kostenlose
> Sandbox-Deploymentplan (`nf-compute-10`) jedem Dienst nur **0,1 vCPU und 256 MB RAM**
> zuteilt. Für eine Spring-Boot-Anwendung mit Hibernate, Flyway, Spring Security und
> Tomcat reicht das nicht: Der Start dauerte über 4 Minuten (starke CPU-Drosselung) und
> der Container wurde zwischendurch mit allen Anzeichen eines OOM-Kills abgeschossen
> (17 Minuten ganz ohne Log-Ausgabe, kein sauberes Herunterfahren). Der nächste Plan mit
> genug Speicher (`nf-compute-100-1`, 1 vCPU / 1 GB) kostet ab da $18/Monat — teurer als
> eine eigene VPS mit voller Kontrolle.
>
> Frontend und Datenbank liefen dagegen problemlos im Sandbox-Plan; nur der
> JVM-lastige Backend-Dienst sprengt den Rahmen. Diese Anleitung bleibt als Referenz
> stehen — für ein leichteres Backend, oder falls Northflank die Sandbox-Ressourcen
> später aufstockt, ist sie unverändert gültig. Für diesen Pizza-Shop-Stack aktuell
> empfohlen: [Hetzner](deployment.md#warum-diese-variante) oder Warten auf Oracle-Kapazität
> ([`deployment.md`](deployment.md), Stand gesichert unter dem Git-Tag `oracle-oci-attempt`).

# Deployment auf Northflank Free — Schritt für Schritt

Der zweite Weg neben Oracle Cloud (siehe [`deployment.md`](deployment.md)): Northflank baut die
zwei Images direkt aus dem GitHub-Repository, betreibt eine PostgreSQL-Datenbank als Addon und
terminiert HTTPS selbst. Kein eigener Server, kein Caddy, kein SSH, kein GitHub-Actions-Workflow
nötig — ein Push auf `master` baut und rollt automatisch aus.

Der **Developer-Sandbox-Plan** ist dauerhaft kostenlos und bringt genau das, was der Stack
braucht: 2 Dienste, 1 Datenbank, immer an (kein Einschlafen wie bei Render). Passt exakt zu
unseren zwei Diensten (Backend, Frontend) plus einer Postgres.

Für die Technik dahinter siehe [Entwickler-Doku](entwicklerdoku.md), für den Oracle-Weg (falls
dort später doch Kapazität frei wird — der Stand ist unter dem Git-Tag `oracle-oci-attempt`
gesichert) [`deployment.md`](deployment.md).

---

## Alle Links auf einen Blick

| Wofür | Link |
| :--- | :--- |
| Konto anlegen | https://northflank.com/signup |
| Dashboard | https://app.northflank.com/ |
| Was der Sandbox-Plan umfasst | https://northflank.com/pricing |
| API-Tokens (nur falls du später automatisieren willst) | https://app.northflank.com/ → Account Settings → API → Tokens |
| Dokumentation: Dienst aus Git bauen | https://northflank.com/docs/v1/application/build/build-code-from-a-git-repository |
| Dokumentation: Datenbank-Zugangsdaten verbinden | https://northflank.com/docs/v1/application/databases-and-persistence/connect-database-secrets-to-workloads |
| Dokumentation: Eigene Domain verbinden | https://northflank.com/docs/v1/application/getting-started/add-a-and-verify-domain |
| Northflank-Status (bei Störungen) | https://status.northflank.com/ |

---

## Was am Ende läuft

```
Browser ──HTTPS──> Northflank-Edge (TLS, *.northflank.app oder eigene Domain)
                          │
                          ▼
                    frontend-Dienst (nginx + Angular, Port 80, öffentlich)
                          │  /api  (intern, per Dienstname erreichbar)
                          ▼
                    backend-Dienst (Spring Boot, Port 8080, NUR intern)
                          │
                          ▼
                    pizza-db (PostgreSQL-Addon)

git push ──> Northflank baut beide Dockerfiles selbst und rollt automatisch aus
```

Wichtig: **Der Backend-Dienst muss exakt `backend` heißen.** Northflank macht jeden Dienst
unter seinem Namen erreichbar (`<dienstname>:<port>`) — genau wie Docker Compose. Unser
[`frontend/nginx.conf`](../frontend/nginx.conf) reicht `/api` schon an `http://backend:8080`
weiter; bei diesem Namen ist am Code **nichts** anzupassen.

---

## Schritt 1: Konto anlegen

**https://northflank.com/signup** — Anmeldung mit GitHub ist am schnellsten, da Northflank
ohnehin gleich Zugriff auf das Repository braucht.

## Schritt 2: Projekt anlegen

Im Dashboard **Create new → Project** → einen Namen vergeben, z. B. `pizza-shop`. Ein Projekt
ist der Rahmen, in dem alle drei Bausteine (Backend, Frontend, Datenbank) zusammen im internen
Netz hängen.

## Schritt 3: PostgreSQL-Addon anlegen

Im Projekt **Create new → Addon → PostgreSQL**.

| Feld | Wert |
| :--- | :--- |
| Name | **`pizza-db`** (der Name bestimmt die Namen der Umgebungsvariablen weiter unten) |
| Version | die aktuellste angebotene |
| Plan | der kleinste/kostenlose |

Dokumentation: https://northflank.com/docs/v1/application/databases-and-persistence/deploy-databases-on-northflank/deploy-postgresql-on-northflank

## Schritt 4: Backend-Dienst anlegen

**Create new → Service**, Typ **Combined service** (baut, deployt und vernetzt in einem Zug).

| Feld | Wert |
| :--- | :--- |
| Name | **`backend`** — muss exakt so heißen, siehe oben |
| Repository | dein GitHub-Repo (Northflank fragt beim ersten Mal nach Zugriff auf GitHub — bestätigen) |
| Branch | `master` |
| Build-Methode | **Dockerfile** |
| Dockerfile-Pfad | `backend/Dockerfile` |
| Build-Kontext | `backend` |
| Networking | Port **8080**, **Private** (kein öffentlicher Zugriff — nur `frontend` soll ihn erreichen) |

Danach bei **Environment** die Variablen setzen. Zuerst die Datenbank verbinden: im Abschnitt
*Addons* das Addon `pizza-db` auswählen und **HOST**, **PORT**, **DATABASE**, **USERNAME**,
**PASSWORD** anhaken — das legt automatisch Variablen wie `NF_PIZZA_DB_HOST` an
([Dokumentation](https://northflank.com/docs/v1/application/databases-and-persistence/connect-database-secrets-to-workloads)).
Danach diese Laufzeit-Variablen ergänzen — `${...}` referenziert die eben angelegten:

| Name | Wert |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${NF_PIZZA_DB_HOST}:${NF_PIZZA_DB_PORT}/${NF_PIZZA_DB_DATABASE}` |
| `SPRING_DATASOURCE_USERNAME` | `${NF_PIZZA_DB_USERNAME}` |
| `SPRING_DATASOURCE_PASSWORD` | `${NF_PIZZA_DB_PASSWORD}` |
| `ADMIN_BOOTSTRAP_EMAIL` | deine E-Mail |
| `ADMIN_BOOTSTRAP_PASSWORD` | ein Passwort mit mindestens 10 Zeichen — frei ausdenken oder in PowerShell erzeugen: `-join ((48..57)+(65..90)+(97..122)|Get-Random -Count 24|%{[char]$_})` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `native` |

Der letzte Punkt ist kein Kann: Northflanks Edge terminiert TLS genau wie der Caddy im
Oracle-Weg, und ohne `native` bekäme das `JSESSIONID`-Cookie kein `Secure`-Flag (Begründung im
[Entwickler-Doku-Abschnitt "Betrieb im Netz"](entwicklerdoku.md#betrieb-im-netz) — die
Erklärung gilt unabhängig davon, welcher Reverse Proxy vorne steht). `FRONTEND_URL` fehlt hier
absichtlich noch; die trägst du in Schritt 6 nach, sobald die Adresse des Frontend-Diensts
feststeht.

**Create service** — der erste Build startet sofort.

## Schritt 5: Frontend-Dienst anlegen

Wieder **Create new → Service → Combined service**.

| Feld | Wert |
| :--- | :--- |
| Name | frei wählbar, z. B. `frontend` |
| Repository | dasselbe Repo |
| Branch | `master` |
| Dockerfile-Pfad | `frontend/Dockerfile` |
| Build-Kontext | `frontend` |
| Networking | Port **80**, **Public** |

Keine Umgebungsvariablen nötig — die SPA spricht ausschließlich über denselben Origin mit dem
Backend, das übernimmt `frontend/nginx.conf`.

**Create service.** Nach dem ersten erfolgreichen Build zeigt der Dienst unter *Networking*
eine Adresse wie `https://frontend--xxxxx.code.run` — schon mit gültigem Zertifikat, ganz ohne
eigene Domain.

> **Prüfen:** Diese Adresse öffnen. Die Speisekarte muss laden.

## Schritt 6: FRONTEND_URL nachtragen

Zurück beim `backend`-Dienst → *Environment* → `FRONTEND_URL` ergänzen mit der Adresse aus
Schritt 5 (z. B. `https://frontend--xxxxx.code.run`, **ohne** Schrägstrich am Ende) → speichern.
Northflank rollt den Dienst mit der neuen Variable automatisch neu aus.

## Schritt 7: Testen

- `https://<frontend-adresse>/admin` öffnen, mit `ADMIN_BOOTSTRAP_EMAIL` /
  `ADMIN_BOOTSTRAP_PASSWORD` aus Schritt 4 anmelden.
- Baut die Anmeldung eine Sitzung auf, aber der nächste Klick landet wieder auf dem Login: dann
  hat das Cookie kein `Secure`-Flag bekommen — `SERVER_FORWARD_HEADERS_STRATEGY=native` beim
  `backend`-Dienst prüfen (Schritt 4).

## Schritt 8 (optional): Eigene Domain

*Frontend-Dienst → Networking → Domains → Add domain* — die dortige Anleitung führt durch den
DNS-Eintrag. Zertifikat kommt automatisch von Let's Encrypt.
Dokumentation: https://northflank.com/docs/v1/application/getting-started/add-a-and-verify-domain

Danach `FRONTEND_URL` beim `backend`-Dienst auf die eigene Domain umstellen (Schritt 6).

---

## Updates ausrollen

Ein `git push` auf `master` genügt — beide Dienste haben CI/CD ab Werk aktiviert und bauen die
neuesten Commits automatisch. Der [GitHub-Actions-Workflow](../.github/workflows/ci-cd.yml)
bleibt trotzdem sinnvoll: Er lässt weiterhin bei jedem Push die 87 Backend- und 54
Frontend-Tests laufen, bevor Northflank überhaupt zu bauen anfängt. Nur die Bauen- und
Ausrollen-Jobs darin sind für diesen Weg ohne Wirkung — die brauchte der Oracle-Weg mit seinem
eigenen Server.

Flyway wandert beim Start des Backends automatisch auf den neuen Schemastand.

## Sicherungen

*Addon `pizza-db` → Backups* — Northflank legt automatische Sicherungen an; Rhythmus und
Aufbewahrungsdauer stellst du dort ein. Für eine zusätzliche eigene Kopie: *Addon → Connect* zeigt
die Zugangsdaten für einen externen `pg_dump`.

---

## Wenn es klemmt

| Symptom | Ursache |
| :--- | :--- |
| Frontend lädt, aber `/api/pizzas` liefert nichts | Backend-Dienst heißt nicht exakt `backend`, oder sein Build ist fehlgeschlagen — Logs im Dienst prüfen |
| Backend-Build schlägt fehl | Dockerfile-Pfad/Build-Kontext prüfen (`backend/Dockerfile` bzw. `backend`) |
| Admin-Login setzt keine Sitzung | `SERVER_FORWARD_HEADERS_STRATEGY=native` fehlt beim Backend (Schritt 4) |
| 500 beim Start, Flyway-Fehler in den Logs | Datenbank-Variablen falsch verknüpft — die vier `NF_PIZZA_DB_*`-Werte im Backend prüfen |
| „Service limit reached" beim Anlegen | Sandbox-Plan ist auf 2 Dienste begrenzt — ein alter/ungenutzter Dienst blockiert den dritten |

---

## Warum dieser Weg neben Oracle

Northflank Free ist die einzige der geprüften Optionen, die **ohne eigenen Server und ohne
Wartezeit auf Kapazität** dauerhaft kostenlos bleibt (Details und die anderen geprüften
Anbieter stehen im Abschnitt ["Warum diese Variante"](deployment.md#warum-diese-variante) in
der Oracle-Anleitung). Der Preis dafür: fest bei 2 Diensten und 1 Datenbank, keine eigene
Kontrolle über die Maschine, und die freie Stufe kann sich ändern — genau wie bei Oracle.
