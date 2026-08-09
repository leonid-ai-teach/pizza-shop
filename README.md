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
| JDK | 21 oder neuer | Maven kommt als Wrapper (`mvnw`) mit, keine Installation nötig |
| Node.js | 20 oder neuer | inkl. npm |

Für den lokalen Start sind **weder Docker noch PostgreSQL nötig** — das Entwicklungsprofil
läuft gegen eine In-Memory-Datenbank.

---

## Schnellstart

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

```
http://localhost:4200/dev-login
```

Damit wird man ohne Google als Admin angemeldet und landet direkt im Dashboard.

> ⚠️ Dieser Endpunkt existiert **ausschließlich** im Profil `localdev` und darf niemals in
> eine deployte Umgebung gelangen — er gibt jedem Admin-Rechte, der die URL aufrufen kann.
> Ohne das Profil ist er schlicht nicht vorhanden (404).

Der reguläre Weg ist die Anmeldung mit Google unter `/admin`. Dafür werden echte
OAuth-Credentials benötigt, siehe [`.env.example`](.env.example) und
[Entwickler-Doku](docs/entwicklerdoku.md#google-oauth-einrichten).

---

## Tests

```bash
cd backend  && ./mvnw test              # 77 Tests (MockMvc-Integrationstests + State Machine)
cd frontend && npm test -- --watch=false # 47 Tests (Angular TestBed)
```

Der Backend-Build (`./mvnw clean verify`) läuft bewusst **ohne** `-Plocaldev`; die Tests
bringen ihre eigene Datenbank mit.

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
│       ├── security/       Spring Security, Google-OIDC, Allowlist
│       ├── exception/      zentrale Fehlerbehandlung
│       └── config/         Startup-Aufgaben
├── frontend/               Angular-SPA
│   └── src/app/
│       ├── core/           Modelle und API-Services
│       ├── shop/           Speisekarte, Pizza-Konfiguration
│       ├── cart/           Warenkorb
│       ├── checkout/       Kasse, Bestellbestätigung
│       └── admin/          Admin-Bereich (lazy geladen)
├── docs/
│   ├── entwicklerdoku.md   technische Doku für Mitentwickler
│   ├── adr/                Architekturentscheidungen
│   └── specs/              Feature-Specs
├── CONTEXT.md              Glossar der Domänenbegriffe
└── .env.example            benötigte Umgebungsvariablen
```

**Sprachkonvention:** Bezeichner im Code sind durchgehend englisch, sichtbare Oberflächen-
texte deutsch. Details in [`CONTEXT.md`](CONTEXT.md).

---

## Was noch offen ist

Der Funktionsumfang von V1 steht, diese Punkte fehlen aber noch:

- **Docker / `docker-compose.yml`** — nicht vorhanden
- **Echter Google-Login nie durchlaufen** — bisher nur der lokale `dev-login`-Bypass getestet
- **Backend-Tests laufen gegen H2**, nicht gegen PostgreSQL via Testcontainers wie in den Specs vorgesehen
- **Kein automatisierter End-to-End-Test** — der Bestellablauf wurde nur manuell im Browser geprüft
- **Fünf Frontend-Komponenten ohne Test**

Die vollständige Liste mit Begründungen steht in der
[Entwickler-Doku](docs/entwicklerdoku.md#offene-punkte).
