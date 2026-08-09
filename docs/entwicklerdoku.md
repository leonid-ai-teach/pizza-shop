# Entwickler-Doku

Technische Dokumentation für alle, die am Code arbeiten. Für Einrichtung und Start siehe
[README](../README.md), für Domänenbegriffe [`CONTEXT.md`](../CONTEXT.md).

---

## Architektur

Zwei eigenständig deploybare Teile. Das Frontend hält keine Datenbankverbindung und kennt
nur die REST-API.

```
Angular SPA  ──HTTP/JSON──>  Controller ──> Service ──> Repository ──> PostgreSQL
 (Browser)                    (kein Zustand,  (Fachlogik)  (Spring Data
                               keine Logik)                    JPA)
```

**Die wichtigste Regel:** In Controllern steht keine Fachlogik. Sie nehmen Requests
entgegen, validieren per Bean Validation und delegieren. Alles, was eine Entscheidung
trifft — Preisberechnung, Statusübergänge, Freigabeprüfungen — gehört in einen Service.

Entitäten verlassen nie die Anwendung: Jede Antwort ist ein DTO aus `dto/`, erzeugt von
einem Mapper aus `mapper/`.

### Modulübersicht

**Backend** (`backend/src/main/java/com/pizzashop/`)

| Paket | Inhalt |
| :--- | :--- |
| `controller/` | HTTP-Schicht; je ein Controller für öffentliche und Admin-Sicht |
| `service/` | gesamte Fachlogik (`OrderService`, `OrderAdminService`, `PizzaAdminService`, …) |
| `repository/` | Spring-Data-Interfaces |
| `entity/` | JPA-Entitäten und Enums |
| `dto/` | Request-/Response-Records |
| `mapper/` | Entität → DTO |
| `security/` | `SecurityConfig`, Google-OIDC-Anbindung, Allowlist-Prüfung, Dev-Login |
| `exception/` | `ApiException`-Hierarchie und `GlobalExceptionHandler` |
| `config/` | `AdminBootstrapRunner` (erster Admin beim Start) |

**Frontend** (`frontend/src/app/`)

| Ordner | Inhalt |
| :--- | :--- |
| `core/` | TypeScript-Modelle und HTTP-Services |
| `shop/` | Speisekarte und Pizza-Konfiguration |
| `cart/` | `CartService` (Signal-Store) und Warenkorbseite |
| `checkout/` | Kasse und Bestellbestätigung |
| `admin/` | Admin-Bereich, per `loadChildren` lazy geladen |

Zustand im Frontend läuft über Angular Signals; der Warenkorb lebt ausschließlich im
Speicher und ist nach einem Reload leer.

---

## Datenmodell

```
Pizza ──< PizzaTopping >── Topping
  │                           │
  └──< OrderItem >── Order    │
         │                    │
         └──< OrderItemTopping ┘
```

Zwei Invarianten tragen das Modell. Wer sie bricht, bricht die Anwendung fachlich:

### 1. Preis-Snapshotting

Beim Anlegen einer Bestellung werden Preise **kopiert**, nicht referenziert:
`OrderItem.basePrice`, `OrderItemTopping.toppingPrice` und `OrderItem.itemTotalPrice`
halten den Stand von damals. Ändert eine Admin später den Katalogpreis, bleiben bereits
aufgegebene Bestellungen unverändert.

Die Positionsformel lautet `(Grundpreis + Summe der Toppingpreise) × Menge` und wird
**immer serverseitig** gerechnet. Vom Client geschickte Preise werden nie übernommen —
das Frontend berechnet nur die Anzeige (`frontend/src/app/cart/cart-item.model.ts`).

### 2. Soft-Delete

Pizzen und Toppings werden nie gelöscht, sondern über `active = false` deaktiviert.
Ein Hard-Delete-Endpunkt existiert bewusst nicht: Historische Bestellungen verweisen
weiter auf diese Zeilen. Deaktivierte Einträge verschwinden aus der Kundensicht, bleiben
in der Adminliste aber sichtbar.

---

## API

Alle Fehlerantworten haben dasselbe Format:

```json
{ "timestamp": "...", "status": 400, "error": "VALIDATION_ERROR", "message": "..." }
```

Fehlercodes: `VALIDATION_ERROR` (400), `NOT_FOUND` (404), `CONCURRENT_MODIFICATION` (409),
`INTERNAL_ERROR` (500).

### Öffentlich

| Methode | Pfad | Zweck |
| :--- | :--- | :--- |
| `GET` | `/api/pizzas` | Speisekarte (nur aktive, nach `sortOrder`) |
| `GET` | `/api/pizzas/{id}` | Pizza-Details inkl. wählbarer Toppings |
| `GET` | `/api/toppings` | alle aktiven Toppings |
| `POST` | `/api/orders` | Bestellung aufgeben |
| `GET` | `/api/orders/{publicToken}` | Bestellbestätigung — **Token, nicht ID** (siehe unten) |

### Admin (Authentifizierung erforderlich)

| Methode | Pfad | Zweck |
| :--- | :--- | :--- |
| `GET` | `/api/admin/me` | Identität der aktuellen Sitzung; nutzt das Frontend als Guard |
| `GET` `POST` | `/api/admin/admins` | Allowlist lesen / neue Admin einladen |
| `GET` | `/api/admin/orders` | Bestellungen, optional `?status=` |
| `GET` | `/api/admin/orders/summary` | Zähler für das Dashboard |
| `GET` | `/api/admin/orders/{id}` | Bestelldetails |
| `PATCH` | `/api/admin/orders/{id}/status` | Statuswechsel |
| `GET` `POST` | `/api/admin/pizzas` | Katalog lesen (inkl. inaktiver) / anlegen |
| `PUT` `PATCH` | `/api/admin/pizzas/{id}` · `/{id}/active` | bearbeiten / aktiv schalten |
| `GET` `POST` | `/api/admin/toppings` | analog zu Pizzen |
| `PUT` `PATCH` | `/api/admin/toppings/{id}` · `/{id}/active` | analog zu Pizzen |
| `POST` | `/api/admin/logout` | Sitzung beenden |

`/api/admin/orders/summary` und `/api/admin/me` stehen nicht im Master-Prompt. Ersteres,
damit das Dashboard nicht die gesamte Bestellliste laden muss; Letzteres, weil die SPA
serverseitig prüfen muss, ob eine Sitzung gültig ist.

`PUT /api/admin/pizzas/{id}` trägt die **vollständige** Topping-Zuordnung: Der Service
gleicht die Zuordnungszeilen auf genau diese Menge ab. Ein weggelassenes Topping wird
also entfernt, nicht ignoriert.

---

## Sicherheit

### Anmeldung und Freigabe

Der Admin-Bereich nutzt Google OAuth2/OIDC; eigene Passwörter gibt es nicht
([ADR-0001](adr/0001-google-oauth-admin-auth.md)). Zugang bekommt nur, wessen
E-Mail-Adresse **vorab** in `admin_access` steht — `AllowlistOidcUserService` weist jedes
andere Konto direkt beim Login ab. Einen Zwischenzustand „wartet auf Freigabe" gibt es
nicht.

E-Mail-Adressen werden beim Speichern und Vergleichen kleingeschrieben
(`AdminAccessService.normalize`), damit abweichende Schreibweisen des Providers niemanden
aussperren.

Den ersten Admin legt `AdminBootstrapRunner` aus `ADMIN_BOOTSTRAP_EMAIL` an — ohne ihn
könnte niemand die erste Einladung aussprechen.

### CSRF

Die Admin-API authentifiziert per Session-Cookie und ist damit CSRF-angreifbar. Aktiviert
ist Spring Securitys `csrf().spa()`, das ein für JavaScript lesbares `XSRF-TOKEN`-Cookie
setzt — genau das Format, das Angulars `HttpClient` von sich aus als `X-XSRF-TOKEN`
zurückschickt. Es ist also kein eigener Interceptor nötig.

Die öffentlichen Endpunkte sind bewusst ausgenommen: Sie tragen keine Sitzungsrechte, die
ein Angreifer missbrauchen könnte.

### Bestellbestätigung über Token

`GET /api/orders/{publicToken}` ist unauthentifiziert — der Kunde hat keinen Account. Mit
der fortlaufenden ID im Pfad könnte man `/api/orders/1,2,3…` durchprobieren und Name,
Telefon, E-Mail und Lieferadresse **aller** Kunden abgreifen. Deshalb trägt jede
Bestellung einen zufälligen `publicToken`, der die ID in öffentlichen URLs ersetzt.

Die Admin-Endpunkte adressieren weiterhin über die ID; dort schützt die Anmeldung.

### Statusübergänge

Erlaubte Wechsel stehen in `OrderStatus.canTransitionTo`. `DONE` und `CANCELLED` sind
Endzustände. `Order.changeStatusTo` weist alles andere ab, der Service übersetzt das in
einen `VALIDATION_ERROR`.

`Order` trägt zusätzlich ein `@Version`-Feld: Ändern zwei Mitarbeitende dieselbe
Bestellung gleichzeitig, gewinnt nicht mehr stillschweigend der letzte Schreibvorgang —
der zweite bekommt `409 CONCURRENT_MODIFICATION`.

### Dev-Login

`DevLoginController` meldet unter `/dev-login` ohne Google als Bootstrap-Admin an. Die
Klasse ist mit `@Profile("localdev")` annotiert und existiert in anderen Profilen gar
nicht. Sie erzeugt bewusst denselben `OidcUser` wie ein echter Google-Login, damit es
keinen zweiten Authentifizierungspfad gibt, der auseinanderlaufen kann.

**Niemals in einer deployten Umgebung aktivieren.**

---

## Datenbank & Migrationen

Flyway, aufgeteilt in zwei Verzeichnisse unter `backend/src/main/resources/db/migration/`:

| Datei | Ordner | Inhalt |
| :--- | :--- | :--- |
| `V1__init_schema.sql` | `schema/` | Tabellen und Bestellnummern-Sequenz |
| `V2__seed_data.sql` | `seed/` | Beispiel-Pizzen und -Toppings |
| `V3__admin_access.sql` | `schema/` | Allowlist-Tabelle |
| `V4__order_optimistic_locking.sql` | `schema/` | `version`-Spalte |
| `V5__order_public_token.sql` | `schema/` | `public_token`-Spalte |

> **Stolperfalle:** Die Versionsnummern sind über beide Ordner verschränkt. Tests laden
> nur `classpath:db/migration/schema`, dort fehlt also V2. Das ist beabsichtigt — Tests
> legen ihre Daten selbst an — und funktioniert, weil Flyway Lücken auf einer frischen
> Datenbank akzeptiert. Beim Anlegen neuer Migrationen die Nummerierung trotzdem über
> **beide** Ordner hinweg fortführen, damit die Reihenfolge eindeutig bleibt.

Die Seed-Daten sind ein handgepflegter, repräsentativer Ersatz und **nicht** das echte
Menü von Pizzapazzia — die Seite rendert clientseitig und war nicht abrufbar. Der
Kommentar in `V2__seed_data.sql` hält das fest.

---

## Tests

| Ebene | Werkzeug | Naht |
| :--- | :--- | :--- |
| Backend | `@SpringBootTest` + MockMvc | die echte HTTP-API, inkl. Security-Filterkette |
| Backend | einfache Unit-Tests | Zustandsautomat in `OrderStatusTransitionTest` |
| Frontend | Angular TestBed + `HttpTestingController` | Komponente rendern, nur HTTP mocken |

Getestet wird beobachtbares Verhalten, nicht Implementierungsdetails: Requests rein,
Antwort und gespeicherter Zustand raus. Repositories und Services werden in den
Integrationstests **nicht** gemockt.

Admin-Tests simulieren Sitzungen über `AdminTestSupport.allowlistedAdmin(...)` bzw.
`nonAllowlistedUser(...)`; für schreibende Zugriffe ist zusätzlich `.with(csrf())` nötig.

```bash
cd backend  && ./mvnw test -Dtest=OrderAdminApiTest   # einzelne Klasse
cd frontend && npm test -- --watch=false
```

Die Backend-Tests laufen gegen H2 im PostgreSQL-Modus, nicht gegen echtes PostgreSQL via
Testcontainers wie in den Specs vorgesehen — auf der Entwicklungsmaschine war kein Docker
verfügbar. Damit bleiben PostgreSQL-spezifische Eigenheiten ungetestet; siehe
[offene Punkte](#offene-punkte).

---

## Konfiguration

Alles über Umgebungsvariablen, keine Geheimnisse im Code. Vorlage:
[`.env.example`](../.env.example).

| Variable | Zweck |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Datenbankzugang |
| `ADMIN_BOOTSTRAP_EMAIL` | erster freigeschalteter Admin |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` | Google-Client-ID |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | Google-Secret |
| `FRONTEND_URL` | Origin der SPA; steuert CORS und Redirects nach dem Login |

Ohne Google-Credentials startet die Anwendung trotzdem — die Google-Anmeldung wird dann
übersprungen und eine Warnung geloggt. Eine leere Registrierung in `application.yml` würde
den Start dagegen mit „client id must not be empty" abbrechen; deshalb steht dort bewusst
kein `registration`-Block.

### Gegen echtes PostgreSQL fahren

Ohne `-Plocaldev` starten und die drei `SPRING_DATASOURCE_*`-Variablen setzen. Flyway legt
das Schema beim ersten Start selbst an.

### Google OAuth einrichten

In der Google Cloud Console unter *APIs & Services → Credentials* eine OAuth-Client-ID vom
Typ *Web application* erzeugen und als Redirect-URI **den Origin eintragen, den der
Browser sieht** — nicht den Backend-Port:

- lokal: `http://localhost:4200/login/oauth2/code/google`
- produktiv (SPA und API auf einem Origin): `https://<domain>/login/oauth2/code/google`

Im lokalen Setup läuft der OAuth-Verkehr über den Angular-Dev-Server-Proxy
(`frontend/proxy.conf.json`), der den `Host`-Header nicht umschreibt. Spring bildet die
Callback-URL deshalb gegen `:4200`, nicht gegen `:8080`. Ein auf `:8080` registrierter
Eintrag führt zu `redirect_uri_mismatch`.

---

## Offene Punkte

Bewusst offen gelassen oder durch die Entwicklungsumgebung erzwungen:

| Punkt | Hintergrund |
| :--- | :--- |
| **Docker / `docker-compose.yml`** | Master-Prompt §19 fordert einen Ein-Kommando-Start. Nicht umgesetzt, auf der Maschine war kein Docker installiert. |
| **Echter Google-Login ungetestet** | Nur der `dev-login`-Bypass wurde im Browser durchgespielt. Der eigentliche OAuth-Flow braucht echte Credentials. |
| **Tests gegen H2 statt Testcontainers** | Beide Specs verlangen echtes PostgreSQL. Ohne Docker nicht möglich; PostgreSQL-spezifisches Verhalten bleibt daher ungeprüft. |
| **Seed-Daten nicht vom echten Menü** | pizzapazzia.de rendert clientseitig, der Inhalt war nicht abrufbar. |
| **Fünf Komponenten ohne Test** | `menu`, `order-confirmation`, `admin-shell`, `admin-login`, `admin-toppings`. Die Abläufe wurden manuell im Browser geprüft, aber nicht automatisiert abgesichert. |
| **Kein automatisierter E2E-Test** | Master-Prompt Phase 7. Der Bestellablauf wurde per Playwright nur manuell verifiziert. |
| **Admin-Entzug fehlt** | Nur das Einladen ist umgesetzt; das Entfernen einer Freigabe war in [`specs/admin-area.md`](specs/admin-area.md) ausdrücklich außerhalb des Umfangs. |

Bewusste V1-Grenzen (kein Nachholbedarf, sondern Entscheidung): keine Online-Zahlung
([ADR-0002](adr/0002-payment-status-field-not-entity.md)), kein Bild-Upload — `imagePath`
ist ein reines URL-Feld —, keine Benachrichtigungen an Kundinnen und Kunden.
