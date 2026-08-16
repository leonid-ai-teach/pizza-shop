# Pizza Shop

Webanwendung für einen Pizza-Lieferservice: ein öffentlicher Kundenbereich (Speisekarte,
Pizza-Konfiguration, Warenkorb, Bestellung mit Lieferung oder Abholung) und ein geschützter
Admin-Bereich (Bestellverwaltung, Pizzen- und Topping-Pflege, Admin-Freigaben).

**Stack:** Spring Boot 4.1 / Java 21 / PostgreSQL / Flyway im Backend, Angular 22 im Frontend.
Beide Teile sind eigenständig; das Frontend spricht ausschließlich über die REST-API.

---

## Voraussetzungen

| Werkzeug | Version | Anmerkung |
| :--- | :--- | :--- |
| Docker | mit Compose v2 | für den Ein-Kommando-Start und für die Backend-Tests |
| JDK | 21 oder neuer | nur für den Start ohne Docker; Maven kommt als Wrapper (`mvnw`) mit |
| Node.js | 20 oder neuer | nur für den Start ohne Docker; inkl. npm |

Es gibt zwei Wege: den kompletten Stack in Docker oder die beiden Teile direkt auf der
Maschine. Der zweite braucht **kein** Docker und **kein** PostgreSQL — das Entwicklungsprofil
läuft gegen eine In-Memory-Datenbank.

---

## Start mit Docker

```bash
cp .env.example .env     # einmalig, Werte anpassen
docker compose up
```

Danach `http://localhost:8080` öffnen. Der Stack besteht aus PostgreSQL, dem Backend und
einem nginx, der die Angular-App ausliefert und zugleich `/api` ans Backend weiterreicht. Weil
dadurch alles unter einem Origin liegt, entfällt CORS.

Nach außen sichtbar ist nur der nginx; Backend und Datenbank hängen im internen Netz. Die
Bestelldaten liegen im Volume `pizza-shop_pgdata` und überleben `docker compose down` —
`docker compose down -v` löscht sie mit.

In der `.env` gehören `ADMIN_BOOTSTRAP_EMAIL` und `ADMIN_BOOTSTRAP_PASSWORD` gesetzt — das ist
der erste Admin, mit dem man sich unter `/admin` anmeldet (siehe [`.env.example`](.env.example)).

### Wenn die Anmeldung im Admin-Bereich fehlschlägt

`ADMIN_BOOTSTRAP_PASSWORD` greift **nur beim Anlegen** der Admin-Zeile. Ändert jemand sein
Passwort später in der Verwaltung, bleibt diese Änderung bestehen — auch über
`docker compose up --build` hinweg, denn der Build erneuert die Images, nicht die Datenbank.
Der Wert in der `.env` ist dann veraltet, und das Anmeldeformular meldet
`Email or password is not correct.`

Das ist Absicht: Würde der Bootstrap ein vorhandenes Passwort überschreiben, drehte jeder
Neustart eine bewusste Änderung zurück. Zurück auf den `.env`-Wert kommt man, indem man den
Hash leert — der Startvorgang füllt ihn dann neu, Bestelldaten bleiben erhalten:

```bash
docker compose exec -T db psql -U pizzashop -d pizzashop \
  -c "UPDATE admin_access SET password_hash = NULL WHERE email = 'DEINE-ADRESSE';"
docker compose restart backend
```

Eine Zeile ohne Hash kann sich nicht anmelden, es entsteht also zu keinem Zeitpunkt ein
Standardpasswort. Alternativ setzt `docker compose down -v` den ganzen Stack zurück — das
löscht allerdings auch alle Bestellungen.

---

## Start ohne Docker

Zwei Terminals:

```bash
# Terminal 1 – Backend auf http://localhost:8080
cd backend
./mvnw spring-boot:run -Plocaldev
```

```bash
# Terminal 2 – Frontend auf http://localhost:4200
cd frontend
npm install
npm start
```

Dann `http://localhost:4200` öffnen. Die Speisekarte wird beim ersten Start automatisch
mit Beispieldaten befüllt.

`-Plocaldev` startet die Anwendung gegen eine In-Memory-H2 im PostgreSQL-Modus und setzt
zugleich das passende Spring-Profil. **Die Daten sind nach jedem Neustart wieder weg** —
das ist so gewollt. Wie man gegen ein echtes PostgreSQL fährt, steht in der
[Entwickler-Doku](docs/entwicklerdoku.md#konfiguration).

### Admin-Bereich lokal öffnen

`http://localhost:4200/admin` aufrufen und mit den Werten anmelden, die
[`application-localdev.yml`](backend/src/main/resources/application-localdev.yml) vorgibt:

| | |
| :--- | :--- |
| E-Mail | `dev-admin@pizzashop.local` |
| Passwort | `localdev-passwort` |

Diese Zugangsdaten stehen im Repository, weil das Profil `localdev` gegen eine In-Memory-Datenbank
läuft und niemals in einer deployten Umgebung aktiv ist. Überall sonst kommt der erste Admin aus
`ADMIN_BOOTSTRAP_EMAIL` und `ADMIN_BOOTSTRAP_PASSWORD`.

---

## Tests

```bash
cd backend  && ./mvnw test              # 87 Tests (MockMvc-Integrationstests + State Machine)
cd frontend && npm test -- --watch=false # 54 Tests (Angular TestBed)
```

Der Backend-Build (`./mvnw clean verify`) läuft bewusst **ohne** `-Plocaldev`; die Tests
bringen ihre eigene Datenbank mit: Testcontainers startet dafür ein echtes PostgreSQL.
**Dafür muss ein Docker-Daemon laufen.**

---

## Deployment

**[Eigene Maschine (Oracle Cloud Always Free)](docs/deployment.md)** — derselbe
`docker compose`, davor ein Caddy für HTTPS:

```bash
cp .env.prod.example .env        # auf dem Server, Domain und Passwörter eintragen
docker compose up -d --build     # welche Dateien gelten, steht als COMPOSE_FILE in der .env
```

Ein `git push` auf `master` testet, baut die Images für ARM und rollt sie per SSH aus
([`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)). Erfordert eine eigene Maschine
und deutlich mehr Einrichtung — dafür volle Kontrolle. In stark nachgefragten Regionen kann die
Kapazität für die kostenlose ARM-Maschine tagelang ausgebucht sein (Details und ein
Wiederholungsansatz stehen in der Anleitung).

Ausprobiert und für diesen Stack **nicht geeignet**: Northflank Free
([`docs/deployment-northflank.md`](docs/deployment-northflank.md)) — das kostenlose Kontingent
reicht nicht für den JVM-lastigen Backend-Dienst (0,1 vCPU / 256 MB, Details im Dokument).

---

## Projektstruktur

```
pizza-shop/
├── backend/                REST-API (Spring Boot)
│   └── src/main/java/com/pizzashop/
│       ├── controller/     HTTP-Schicht, keine Fachlogik
│       ├── service/        gesamte Fachlogik
│       ├── repository/     Spring Data JPA
│       ├── entity/         JPA-Entitäten
│       ├── dto/            Request-/Response-Typen (Entitäten gehen nie nach außen)
│       ├── mapper/         Entität → DTO
│       ├── security/       Spring Security, Anmeldung, Admin-Benutzer
│       ├── exception/      zentrale Fehlerbehandlung
│       └── config/         Startup-Aufgaben
│   └── Dockerfile          Multi-Stage-Build (Maven → JRE)
├── frontend/               Angular-SPA
│   ├── Dockerfile          Multi-Stage-Build (Node → nginx)
│   ├── nginx.conf          SPA-Auslieferung + Reverse Proxy zum Backend
│   └── src/app/
│       ├── core/           Modelle und API-Services
│       ├── shop/           Speisekarte, Pizza-Konfiguration
│       ├── cart/           Warenkorb
│       ├── checkout/       Kasse, Bestellbestätigung
│       └── admin/          Admin-Bereich (lazy geladen)
├── .github/workflows/      Tests, Images bauen, ausrollen (GitHub Actions)
├── deploy/                 Betrieb im Netz
│   ├── Caddyfile           TLS-Terminierung, Sicherheits-Header
│   ├── remote-update.sh    ein Ausrollvorgang auf der Maschine
│   └── backup.sh           tägliche Datenbanksicherung
├── docs/
│   ├── entwicklerdoku.md   technische Doku für Mitentwickler
│   ├── deployment.md       Anleitung für den Betrieb im Netz
│   ├── adr/                Architekturentscheidungen
│   └── specs/              Feature-Specs
├── docker-compose.yml      kompletter Stack: PostgreSQL, Backend, Frontend
├── docker-compose.prod.yml Aufsatz für den Betrieb: Caddy davor, keine offenen Ports
├── docker-compose.registry.yml  Aufsatz: Images ziehen statt bauen
├── CONTEXT.md              Glossar der Domänenbegriffe
├── .env.example            benötigte Umgebungsvariablen
└── .env.prod.example       dieselben Werte für den Betrieb im Netz
```

**Sprachkonvention:** Bezeichner im Code sind durchgehend englisch, sichtbare Oberflächen-
texte deutsch. Details in [`CONTEXT.md`](CONTEXT.md).

---

## Was noch offen ist

Der Funktionsumfang von V1 steht, diese Punkte fehlen aber noch:

- **Kein automatisierter End-to-End-Test** — der Bestellablauf wurde nur manuell im Browser geprüft
- **Vier Frontend-Komponenten ohne Test**
- **Keine Sperre nach zu vielen Fehlversuchen** beim Admin-Login

Die vollständige Liste mit Begründungen steht in der
[Entwickler-Doku](docs/entwicklerdoku.md#offene-punkte).
