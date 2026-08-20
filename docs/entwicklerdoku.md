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
| `security/` | `SecurityConfig`, Anmeldung, Admin-Benutzer aus `admin_access` |
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
| `POST` | `/api/admin/login` | Anmeldung mit E-Mail und Passwort — **der einzige offene Pfad hier** |
| `GET` | `/api/admin/me` | Identität der aktuellen Sitzung; nutzt das Frontend als Guard |
| `PUT` | `/api/admin/me/password` | eigenes Passwort ändern |
| `GET` `POST` | `/api/admin/admins` | Admins lesen / neuen Admin einladen |
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

Der Admin-Bereich meldet mit E-Mail und Passwort an: `POST /api/admin/login` prüft die Daten und
legt eine Server-Session an ([ADR-0003](adr/0003-admin-password-auth.md), das
[ADR-0001](adr/0001-google-oauth-admin-auth.md) und dessen Google-Login ablöst).

Die Tabelle `admin_access` ist zugleich Einladungsliste **und** Benutzertabelle — es gibt nur
eine Rolle, eine eigene `User`-Entität trüge keine zusätzliche Information.
`AdminUserDetailsService` liest sie aus; wer nicht darinsteht, kommt nicht hinein. Einen
Zwischenzustand „wartet auf Freigabe" gibt es nicht, und eine Selbstregistrierung ebenso wenig.

Gehasht wird mit Springs `DelegatingPasswordEncoder`, der das Verfahren im Hash vermerkt
(`{bcrypt}$2a$10$…`). Ein späterer Wechsel entwertet damit nicht alle Passwörter auf einmal.

**Eine Zeile ohne `password_hash` kann sich nicht anmelden.** Das betrifft Bestände aus der
Google-Zeit und ist Absicht: An keiner Stelle entsteht ein Standardpasswort.
`AdminUserDetailsService` weist solche Zeilen ab wie eine unbekannte Adresse.

E-Mail-Adressen werden beim Speichern und Vergleichen kleingeschrieben
(`AdminAccessService.normalize`), damit abweichende Schreibweisen niemanden aussperren — die
Anmeldung ist dadurch unempfindlich gegen Groß- und Kleinschreibung.

Den ersten Admin legt `AdminBootstrapRunner` aus `ADMIN_BOOTSTRAP_EMAIL` **und**
`ADMIN_BOOTSTRAP_PASSWORD` an — ohne ihn könnte niemand die erste Einladung aussprechen. Fehlt
einer der beiden Werte, passiert nichts und es wird gewarnt. Ein bereits gesetztes Passwort
wird nie überschrieben, sonst würde jeder Neustart eine bewusste Änderung zurückdrehen — was
das im Docker-Betrieb bedeutet, steht unter
[`ADMIN_BOOTSTRAP_PASSWORD` gilt nur beim Anlegen](#admin_bootstrap_password-gilt-nur-beim-anlegen).

Neue Admins legt ein bestehender Admin unter `/admin/admins` samt erstem Passwort an und gibt
es außerhalb der Anwendung weiter — es gibt keinen Mailversand. Die eingeladene Person ändert
es danach selbst über `PUT /api/admin/me/password`. Fremde Passwörter kann niemand setzen, was
zugleich heißt: Für ein vergessenes Passwort gibt es keinen Self-Service, im Zweifel bleibt der
Weg über `ADMIN_BOOTSTRAP_*` oder die Datenbank.

Was fehlt: eine Sperre nach zu vielen Fehlversuchen. Solange die Anwendung nur lokal läuft, ist
das verschmerzbar; vor einem öffentlichen Deployment gehört sie nachgezogen.

### CSRF

Die Admin-API authentifiziert per Session-Cookie und ist damit CSRF-angreifbar. Aktiviert
ist Spring Securitys `csrf().spa()`, das ein für JavaScript lesbares `XSRF-TOKEN`-Cookie
setzt — genau das Format, das Angulars `HttpClient` von sich aus als `X-XSRF-TOKEN`
zurückschickt. Es ist also kein eigener Interceptor nötig.

Die öffentlichen Endpunkte sind bewusst ausgenommen: Sie tragen keine Sitzungsrechte, die
ein Angreifer missbrauchen könnte. Dasselbe gilt für `/api/admin/login` — der Aufruf läuft,
bevor eine Session existiert, es gibt also keine Autorität, auf der ein fremdes Formular
mitreiten könnte. Ein Token dort zu verlangen hieße, ihn vor der Anmeldung mit einem
Extra-Request zu besorgen.

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

### Sitzung beim Anmelden

`AdminLoginController` authentifiziert von Hand über den `AuthenticationManager`, statt Spring
Securitys Formular-Filter zu benutzen — die SPA braucht einen Statuscode, keinen HTML-Redirect.
Das hat eine Konsequenz, die leicht übersehen wird: Der Filter bringt seine
`SessionAuthenticationStrategy` mit, der Controller nicht. Deshalb ruft er selbst
`request.changeSessionId()` auf. Ohne das überlebte eine vom Angreifer untergeschobene
Session-ID den Rechtewechsel (Session Fixation).

Ob die E-Mail unbekannt oder das Passwort falsch war, unterscheidet die Antwort bewusst nicht:
Sonst verriete das Anmeldeformular, welche Adressen Admin-Konten sind.

---

## Datenbank & Migrationen

Flyway, aufgeteilt in zwei Verzeichnisse unter `backend/src/main/resources/db/migration/`:

| Datei | Ordner | Inhalt |
| :--- | :--- | :--- |
| `V1__init_schema.sql` | `schema/` | Tabellen und Bestellnummern-Sequenz |
| `V2__seed_data.sql` | `seed/` | Beispiel-Pizzen und -Toppings |
| `V3__admin_access.sql` | `schema/` | Admin-Tabelle |
| `V4__order_optimistic_locking.sql` | `schema/` | `version`-Spalte |
| `V5__order_public_token.sql` | `schema/` | `public_token`-Spalte |
| `V6__admin_password.sql` | `schema/` | `password_hash`-Spalte |

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
`nonAllowlistedUser(...)`; für schreibende Zugriffe ist zusätzlich `.with(csrf())` nötig. Die
Anmeldung selbst — Passwortprüfung, Sessionaufbau, Passwortwechsel — deckt `AdminLoginApiTest`
ab, der als einziger tatsächlich über `/api/admin/login` geht.

```bash
cd backend  && ./mvnw test -Dtest=OrderAdminApiTest   # einzelne Klasse
cd frontend && npm test -- --watch=false
```

### Die Testdatenbank

Die Backend-Tests laufen gegen ein echtes PostgreSQL, das Testcontainers startet — **ein
laufender Docker-Daemon ist damit Voraussetzung.** Eine In-Memory-Datenbank würde genau die
Dinge nicht prüfen, an denen dieses Modell hängt: die Bestellnummern-Sequenz, `GENERATED BY
DEFAULT AS IDENTITY` und das Verhalten von `NUMERIC` bei der Preisberechnung.

Der Container steckt in `PostgresTestcontainerConfiguration`, die jede Integrationstestklasse
per `@Import` einbindet. Weil dadurch alle Klassen dieselbe Kontext-Konfiguration tragen,
cached Spring **einen** Kontext und startet **einen** Container für den gesamten Lauf — der
Aufschlag beträgt rund zwei Sekunden, nicht zwei Sekunden pro Klasse.

Dass der Container über den ganzen Lauf lebt, ist unkritisch: alle Integrationstests sind
`@Transactional` und rollen ihre Daten zurück. Die einzige Ausnahme ist die Sequenz
`order_number_seq`, denn Sequenzen kennen kein Rollback. Die Tests prüfen Bestellnummern
deshalb bewusst relativ (`>= 100000`, monoton steigend) und nie auf einen absoluten Wert —
wer das ändert, macht die Tests von der Ausführungsreihenfolge abhängig.

Die Image-Version ist absichtlich dieselbe wie in `docker-compose.yml`.

---

## Konfiguration

Alles über Umgebungsvariablen, keine Geheimnisse im Code. Vorlage:
[`.env.example`](../.env.example).

| Variable | Zweck |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Datenbankzugang |
| `ADMIN_BOOTSTRAP_EMAIL` | erster Admin |
| `ADMIN_BOOTSTRAP_PASSWORD` | dessen Passwort — greift **nur beim Anlegen**, siehe unten |
| `FRONTEND_URL` | Origin der SPA; steuert CORS |

Drei weitere Variablen liest ausschließlich `docker-compose.yml` und reicht sie passend an
die Container weiter:

| Variable | Zweck |
| :--- | :--- |
| `POSTGRES_DB` / `_USER` / `_PASSWORD` | Datenbank im Stack; compose baut daraus die `SPRING_DATASOURCE_*` |
| `APP_PORT` | Host-Port des nginx |
| `PUBLIC_URL` | Adresse, unter der der Browser den Stack sieht; wird zu `FRONTEND_URL` — muss zu `APP_PORT` passen |

Fehlt eines der beiden `ADMIN_BOOTSTRAP_*`, startet die Anwendung trotzdem, legt aber keinen
Admin an und warnt — dann kommt niemand in die Verwaltung. Das Profil `localdev` bringt für
beide eigene Vorgaben mit (`dev-admin@pizzashop.local` / `localdev-passwort`), damit der
Docker-freie Start ohne Konfiguration auskommt.

### `ADMIN_BOOTSTRAP_PASSWORD` gilt nur beim Anlegen

Die häufigste Stolperfalle im Docker-Betrieb: Man kommt mit dem Passwort aus der `.env` nicht
mehr hinein.

`AdminBootstrapRunner` legt die Zeile an oder ergänzt einen fehlenden Hash — einen
**vorhandenen** Hash überschreibt er nie (`AdminAccessService.ensureBootstrapAdmin`). Sonst
drehte jeder Neustart eine bewusste Passwortänderung zurück, und ein Admin könnte sein Passwort
gar nicht dauerhaft ändern.

Zusammen mit dem Volume `pizza-shop_pgdata` heißt das: Wer sein Passwort einmal über
`PUT /api/admin/me/password` geändert hat, meldet sich fortan mit dem neuen an — auch nach
`docker compose up --build`, denn der Build erneuert die Images, nicht die Datenbank. Der Wert
in der `.env` ist ab diesem Moment historisch. `docker compose logs backend` zeigt trotzdem
weiterhin `Bootstrap admin ensured for …`; die Zeile bestätigt nur, dass der Admin existiert,
nicht welches Passwort gilt.

Zurück auf den `.env`-Wert, ohne Bestelldaten zu verlieren:

```bash
docker compose exec -T db psql -U pizzashop -d pizzashop \
  -c "UPDATE admin_access SET password_hash = NULL WHERE email = 'DEINE-ADRESSE';"
docker compose restart backend
```

Genau dafür ist die Spalte nullable: Eine Zeile ohne Hash kann sich nicht anmelden, es entsteht
also nie ein Standardpasswort, und der nächste Start repariert sie aus der Umgebung. Ein
Passwort direkt per SQL zu setzen geht ebenfalls, verlangt aber einen `{bcrypt}`-Hash im
Format des `DelegatingPasswordEncoder` — der Umweg über `NULL` ist einfacher und weniger
fehleranfällig.

### Gegen echtes PostgreSQL fahren

Ohne `-Plocaldev` starten und die drei `SPRING_DATASOURCE_*`-Variablen setzen. Flyway legt
das Schema beim ersten Start selbst an.

### Der Docker-Stack

`docker compose up` startet PostgreSQL, das Backend und einen nginx, der die gebaute SPA
ausliefert. Beide Anwendungen entstehen in Multi-Stage-Builds (Maven → JRE bzw. Node → nginx).

Veröffentlicht wird ausschließlich der nginx auf `${APP_PORT}`; Backend und Datenbank haben
bewusst kein `ports:` und sind nur im Compose-Netz erreichbar. Der nginx reicht `/api` ans
Backend durch, sodass SPA und API auf **einem** Origin liegen. Das räumt zwei Reibungspunkte
weg: CORS entfällt, und das `XSRF-TOKEN`-Cookie braucht keine Sonderregel.

Zwei Details sind kein Zufall:

- `proxy_set_header Host $http_host` in `frontend/nginx.conf` — nicht `$host`. `$host` verwirft
  den Port, Spring würde absolute URLs dann gegen `http://localhost/` statt
  `http://localhost:8080/` bilden.
- Das Volume in `docker-compose.yml` hängt an `/var/lib/postgresql`, **nicht** an
  `/var/lib/postgresql/data`. Seit PostgreSQL 18 legt das Image die Daten in einem
  Unterverzeichnis je Hauptversion ab, damit spätere `pg_upgrade`-Läufe nicht über die
  Mount-Grenze stolpern; mit dem alten Pfad startet der Container gar nicht erst.

### Betrieb im Netz

`docker-compose.prod.yml` legt einen [Caddy](https://caddyserver.com/) davor, der TLS
terminiert und die Zertifikate selbst besorgt; der nginx verliert dabei seinen Host-Port.
Anleitung: [`deployment-oracle-inaktiv.md`](deployment/deployment-oracle-inaktiv.md).

Damit endet TLS vor der Anwendung — und daran hängt ein Detail, das leicht übersehen wird:

- `frontend/nginx.conf` reicht ein vorhandenes `X-Forwarded-Proto` unverändert weiter (`map`
  auf `$forwarded_proto`) statt `$scheme` einzusetzen. Sonst käme beim Backend `http` an,
  obwohl der Browser `https` gesprochen hat.
- Der Betrieb hinter dem Caddy schaltet auf `SERVER_FORWARD_HEADERS_STRATEGY=native` um. Mit
  `framework` bleibt das `JSESSIONID`-Cookie **ohne** `Secure`-Flag: Springs
  `ForwardedHeaderFilter` richtet nur die Sicht der Anwendung, das Sitzungscookie erzeugt aber
  Tomcat, und der entscheidet nach seiner eigenen. `native` setzt schon dort an
  (`RemoteIpValve`), womit beide Cookies — `JSESSIONID` und `XSRF-TOKEN` — `Secure` tragen.

Beide Punkte gelten unabhängig davon, welcher TLS-Terminator vorne steht — dieselbe Begründung
trägt auch [Northflanks Edge](deployment/deployment-northflank-verworfen.md) und [Google Cloud
Run](deployment/deployment-cloudrun.md). Bei Cloud Run entfällt allerdings der erste Punkt: Dort liefert
Spring Boot die Angular-SPA gleich selbst mit aus ([`Dockerfile.cloudrun`](../Dockerfile.cloudrun)),
ganz ohne den nginx aus diesem Repository — das `X-Forwarded-Proto`-Passthrough betrifft dort
niemanden, nur `native` bleibt nötig, weil Cloud Runs eigenes Front-End TLS weiterhin vor dem
Container terminiert.

---

## Offene Punkte

Bewusst offen gelassen oder durch die Entwicklungsumgebung erzwungen:

| Punkt | Hintergrund |
| :--- | :--- |
| **Keine Sperre nach Fehlversuchen** | Das Anmeldeformular lässt sich beliebig oft durchprobieren. Solange die Anwendung nur lokal läuft, verschmerzbar — vor einem öffentlichen Deployment nachziehen ([ADR-0003](adr/0003-admin-password-auth.md)). |
| **Kein Passwort-Reset** | Wer sein Passwort vergisst, kommt nur über [`ADMIN_BOOTSTRAP_*` bzw. die Datenbank](#admin_bootstrap_password-gilt-nur-beim-anlegen) zurück; es gibt keinen Mailversand, über den ein Reset laufen könnte. |
| **Seed-Daten nicht vom echten Menü** | pizzapazzia.de rendert clientseitig, der Inhalt war nicht abrufbar. |
| **Vier Komponenten ohne Test** | `menu`, `order-confirmation`, `admin-shell`, `admin-toppings`. Die Abläufe wurden manuell im Browser geprüft, aber nicht automatisiert abgesichert. |
| **Kein automatisierter E2E-Test** | Master-Prompt Phase 7. Der Bestellablauf wurde per Playwright nur manuell verifiziert. |
| **Admin-Entzug fehlt** | Nur das Einladen ist umgesetzt; das Entfernen einer Freigabe war in [`specs/admin-area.md`](specs/admin-area.md) ausdrücklich außerhalb des Umfangs. |

Bewusste V1-Grenzen (kein Nachholbedarf, sondern Entscheidung): keine Online-Zahlung
([ADR-0002](adr/0002-payment-status-field-not-entity.md)), kein Bild-Upload — `imagePath`
ist ein reines URL-Feld —, keine Benachrichtigungen an Kundinnen und Kunden.
