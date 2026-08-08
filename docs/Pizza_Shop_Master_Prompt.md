# Master-Prompt: Pizza-Shop-Webapplikation mit Spring Boot und Angular

---

## 1. Ziel des Projekts
Entwickle eine vollständige Webapplikation für einen Pizza-Lieferservice bzw. Pizza-Shop.

### Kundenfunktionen
- Pizza-Menü ansehen
- Pizzen auswählen
- Pizzen mit zusätzlichen Toppings konfigurieren
- Mehrere Pizzen in einen Warenkorb legen
- Eine Bestellung aufgeben
- Zwischen **Lieferung** und **Abholung** wählen
- Bestellung ohne Online-Zahlung absenden

### Geschützter Administrationsbereich (Mitarbeiter)
- Eingegangene Bestellungen ansehen und bearbeiten
- Bestellstatus ändern
- Pizzen verwalten (anlegen, bearbeiten, aktivieren/deaktivieren, Preise ändern, Sortierreihenfolge anpassen)
- Toppings verwalten (anlegen, bearbeiten, aktivieren/deaktivieren, Preise ändern)

> Die Anwendung soll als moderne, saubere und erweiterbare Full-Stack-Webapplikation entwickelt werden.

---

## 2. Technologiestack & Backend-Architektur

### Backend Tech Stack
- **Sprache:** Java (Aktuelle stabile LTS-Version)
- **Framework:** Spring Boot (Aktuelle stabile Version)
- **Module:** Spring MVC, Spring Data JPA, Spring Security (für Admin-Bereich)
- **ORM / Persistenz:** Hibernate / PostgreSQL
- **Build-Tool:** Maven
- **Schnittstellen & Validierung:** REST API, Bean Validation

### Backend Architecture
Das Backend wird als eigenständige REST-Anwendung (modularer Monolith) implementiert.

```
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

- **Prinzip:** Keine Businesslogik in den REST-Controllern! Die gesamte Businesslogik gehört strikt in die Service-Klassen.

### Namenskonvention

Klassen, Felder und Enum-Konstanten sind durchgängig **Englisch** (`Order`, `OrderStatus.NEW`, `OrderType.DELIVERY`, ...). Deutsch taucht ausschließlich als UI-Text im Angular-Frontend auf, nie als Code-Bezeichner. Kanonische Begriffe siehe [`CONTEXT.md`](../CONTEXT.md).

---

## 3. Frontend

- **Framework:** Angular & TypeScript
- **Routing & Forms:** Angular Router, Reactive Forms
- **HTTP:** HttpClient
- **Styling:** Modernes, responsives CSS (Desktop, Tablet, Smartphone)

 Das Frontend wird als eigenständige Angular-Anwendung entwickelt und kommuniziert exklusiv über die REST API mit dem Backend.

---

## 4. Datenbank & Datenmodell (PostgreSQL & JPA)

Sauber normalisierte Struktur. Redundanzen und physisches Löschen genutzter Daten werden vermieden.

### Entitäten & Attribute

1. **Pizza**
   - `id` (PK)
   - `name`
   - `description`
   - `price`
   - `imagePath` (optional)
   - `active` (boolean)
   - `sortOrder` (int)

2. **Topping**
   - `id` (PK)
   - `name`
   - `description`
   - `price`
   - `active` (boolean)

3. **PizzaTopping (Verknüpfung)**
   - Abbildung, welche Toppings für welche Pizza verfügbar sind (Many-To-Many Beziehung / Mapping Table).

4. **Bestellung (Order)**
   - `id` (PK)
   - `orderNumber` (Bestellnummer, global fortlaufende DB-Sequence, Startwert 100000, kein Reset)
   - `createdAt` (Erstellungszeitpunkt)
   - `status` (`NEW`, `IN_PROGRESS`, `DONE`, `CANCELLED`) — `CANCELLED` nur aus `NEW`/`IN_PROGRESS` erreichbar; `DONE` und `CANCELLED` sind Endzustände ohne weitere Übergänge
   - `orderType` (`DELIVERY`, `PICKUP`)
   - `paymentStatus` (`NOT_REQUIRED`) — schlankes Feld statt eigener `Payment`-Entität in V1, siehe [ADR-0002](../docs/adr/0002-payment-status-field-not-entity.md)
   - `customerData` (Vorname, Nachname, Telefon, E-Mail, optional: Straße, Hausnummer, PLZ, Ort) — als nullable `@Embeddable`; Adressfelder sind bei `orderType = PICKUP` `null` und werden bedingt validiert (nur bei `DELIVERY` Pflicht)
   - `totalPrice`

5. **Bestellposition (OrderItem)**
   - `id` (PK)
   - `order` (FK zu Order)
   - `pizza` (FK zu Pizza)
   - `quantity` (Menge)
   - `basePrice` (Snapshot!)
   - `selectedToppings` (Liste der Toppings)
   - `toppingPrices` (Snapshot der Topping-Preise!)
   - `itemTotalPrice` (Gesamtpreis der Position)

6. **AdminAccess**
   - `email` (PK)
   - `approvedAt`
   - `approvedBy`
   - Allowlist freigegebener Google-Konten für den Admin-Bereich, siehe [ADR-0001](../docs/adr/0001-google-oauth-admin-auth.md)

> **Wichtig (Preis-Snapshotting):**  
> Die Preise müssen beim Erstellen der Bestellung als **Snapshot** gespeichert werden. Nachträgliche Preisänderungen im Katalog dürfen bereits aufgegebene Bestellungen nicht beeinflussen!

---

## 5. Kundenbereich & Speisekarte

- Die Startseite dient als Pizza-Shop.
- Kunde kann zunächst **ohne Login** bestellen.
- Die Speisekarte wird anhand des Menüs von [Pizzapazzia Menu](https://pizzapazzia.de/menu) erstellt.

> **Datenintegrität:**  
> Es dürfen keine Daten frei erfunden oder fest im Angular-Code hinterlegt werden. Alle Daten kommen dynamisch aus der Datenbank.
>
> **Umsetzung:** Kein Runtime-Scraping. Stattdessen einmalige, manuelle Übertragung einer **repräsentativen Auswahl** (ca. 8–10 Pizzen, 8–10 Toppings) mit echten Namen und echtem Preisniveau von [Pizzapazzia Menu](https://pizzapazzia.de/menu) in eine strukturierte **Seed-Datei** (Flyway SQL / JSON). Kein vollständiger 1:1-Abgleich des gesamten Menüs nötig.

---

## 6. Pizza-Auswahl & Dynamische Preisberechnung

Bei Auswahl einer Pizza wird eine Detailansicht/Modal geöffnet:
- Anzeige von Name, Beschreibung, Grundpreis und verfügbaren Toppings.
- Kunde wählt zusätzliche Toppings aus.
- **Dynamische Preisberechnung:**

> **Gesamtpreis Position** = (Grundpreis Pizza + Summe aller Toppingpreise) × Menge

### Beispiel
| Komponente | Preis |
| :--- | :--- |
| Pizza Margherita | 8,50 € |
| + Extra Käse | +1,00 € |
| + Salami | +1,50 € |
| **Einzelpreis gesamt** | **11,00 €** |
| **Menge: 2** | **22,00 €** |

---

## 7. Warenkorb

Der Warenkorb verwaltet die konfigurierten Positionen:
- Auflistung: Pizza, gewählte Toppings, Menge, Einzelpreis, Positionspreis.
- Aktionen: Menge erhöhen/reduzieren, Position entfernen, Pizza erneut konfigurieren.
- Gesamtsumme aller Positionen wird stets übersichtlich dargestellt.

---

## 8. Bestellprozess (Checkout)

Wahl der Bestellart: **Lieferung** oder **Abholung**.

- **Lieferung:** Abfrage von Vorname, Nachname, Telefon, E-Mail, Straße, Hausnummer, PLZ, Ort.
- **Abholung:** Abfrage von Kontaktdaten (Name, Telefon, E-Mail). Unnötige Adressfelder werden dynamisch ausgeblendet.

---

## 9. Zahlungsabwicklung

- **Version 1:** KEINE Online-Zahlung. Erfassung der Bestellung ohne Bezahlvorgang.
- **Entkopplung:** In V1 genügt ein schlankes `paymentStatus`-Feld auf `Order` (kein eigenes `Payment`-Aggregat) — siehe [ADR-0002](../docs/adr/0002-payment-status-field-not-entity.md). Eine echte Entkopplung in eine eigenständige `Payment`-Entität folgt erst mit einer künftigen Zahlungsanbieter-Integration (V2).
- Initialer Zahlungsstatus: `NOT_REQUIRED`.

---

## 10. Bestellabschluss

Nach erfolgreichem Absenden erscheint eine Bestätigungsseite:
- Bestellnummer (z. B. `#100025`)
- Bestelldetails & Bestellart
- Gesamtpreis
- Status (z. B. `NEU`)
- Hinweistext zur erfolgreichen Erfassung.

---

## 11. Administrationsbereich & Sicherheit

- Route: `/admin`
- Geschützt über **Spring Security + OAuth2 Client (Login mit Google)**. Keine lokale Passwortverwaltung. Details siehe [ADR-0001](../docs/adr/0001-google-oauth-admin-auth.md).
  - Nur eine Rolle: **Admin**. Zugriff wird über die `AdminAccess`-Allowlist (E-Mail) gesteuert, nicht über ein selbstregistrierbares Nutzerkonto.
  - **Freigabe-Workflow:** Ein bestehender Admin trägt die E-Mail-Adresse eines neuen Admins vorab in die Allowlist ein (Einladung), bevor sich diese Person das erste Mal einloggt. Es gibt keinen unbestätigten Zwischenzustand.
  - **Bootstrap:** Der allererste Admin wird über eine Umgebungsvariable (`ADMIN_BOOTSTRAP_EMAIL`) bzw. einen Flyway-Seed hinterlegt.
  - Google OAuth2 Client-ID/Secret werden einmalig manuell in der Google Cloud Console erzeugt und über `.env`/Umgebungsvariablen bereitgestellt (keine Automatisierung dieses Schritts).
- Kein unauthentifizierter Zugriff auf Admin-Endpunkte oder Admin-APIs.
- **Serverseitige Sicherheit:**
  - Der Server berechnet Preise strikt selbst (Frontend-Preise werden niemals getraut!).
  - Validierung von Pizza-/Topping-IDs und deren Aktivitätsstatus (`active = true`).

---

## 12. Dashboard & Bestellverwaltung

### Admin-Dashboard
- Kennzahlen: Neue Bestellungen, In Bearbeitung, Fertiggestellt.
- Schnellzugriff auf eingehende Bestellungen.

### Bestellverwaltung
- Liste aller Bestellungen mit Filtermöglichkeit.
- Detailansicht mit allen Kundendaten, Lieferadresse, Pizzen, Toppings, Preisen.
- **Statusübergänge:** `NEW` -> `IN_PROGRESS` -> `DONE`, sowie `NEW`/`IN_PROGRESS` -> `CANCELLED`. `DONE` und `CANCELLED` sind Endzustaende ohne weitere Uebergaenge (ungueltige Statusuebergaenge werden blockiert).

---

## 13. Verwaltung von Pizzen & Toppings (CRUD)

- **Pizzen:** Anlegen, Bearbeiten, Preis ändern, Beschreibung ändern, Aktivieren/Deaktivieren, Sortierung anpassen.
  - `imagePath` ist ein reines URL-Textfeld (Admin trägt einen extern gehosteten Bildlink ein). Kein Datei-Upload/Storage in V1.
- **Toppings:** Anlegen, Bearbeiten, Preis ändern, Aktivieren/Deaktivieren, Zuordnung zu Pizzen.
- **Soft-Delete:** Ist eine Pizza/ein Topping bereits in Bestellungen vorhanden, darf kein physischer Delete erfolgen. Stattdessen wird `active = false` gesetzt.

---

## 14. REST API Struktur

| Methode | Endpunkt | Beschreibung | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/pizzas` | Speisekarte abrufen | Public |
| `GET` | `/api/pizzas/{id}` | Pizza-Details abrufen | Public |
| `GET` | `/api/toppings` | Öffentliche Toppings | Public |
| `POST` | `/api/orders` | Neue Bestellung aufgeben | Public |
| `GET` | `/api/orders/{id}` | Bestellbestätigung abrufen | Public |
| `GET` | `/api/admin/orders` | Alle Bestellungen einsehen | Admin |
| `GET` | `/api/admin/orders/{id}` | Bestelldetails | Admin |
| `PATCH` | `/api/admin/orders/{id}/status` | Bestellstatus aktualisieren | Admin |
| `GET` | `/api/admin/pizzas` | Admin Pizza-Liste | Admin |
| `POST` | `/api/admin/pizzas` | Pizza erstellen | Admin |
| `PUT` | `/api/admin/pizzas/{id}` | Pizza bearbeiten | Admin |
| `PATCH` | `/api/admin/pizzas/{id}/active` | Pizza aktiv/inaktiv schalten | Admin |
| `GET` | `/api/admin/toppings` | Admin Topping-Liste | Admin |
| `POST` | `/api/admin/toppings` | Topping erstellen | Admin |
| `PUT` | `/api/admin/toppings/{id}` | Topping bearbeiten | Admin |
| `PATCH` | `/api/admin/toppings/{id}/active` | Topping aktiv/inaktiv schalten | Admin |
| `GET` | `/api/admin/admins` | Allowlist freigegebener Admin-E-Mails einsehen | Admin |
| `POST` | `/api/admin/admins` | Neuen Admin per E-Mail einladen (Allowlist-Eintrag) | Admin |

---

## 15. Validierung & Fehlerbehandlung

- Serverseitige Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@Min`, etc.).
- Zentrale Fehlerbehandlung via `@RestControllerAdvice`.
- **Einheitliches Fehler-Format:**

```json
{
  "timestamp": "2026-08-08T13:35:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Die Bestellung enthält ein ungültiges Topping."
}
```

---

## 16. DTOs (Data Transfer Objects)

Entities werden niemals direkt nach außen gegeben.
- `PizzaResponse`, `ToppingResponse`
- `CreateOrderRequest`, `OrderResponse`, `OrderItemResponse`
- `UpdateOrderStatusRequest`
- `CreatePizzaRequest`, `UpdatePizzaRequest`
- `CreateToppingRequest`, `UpdateToppingRequest`

---

## 17. Datenbankmigration & Seeding

- Migrationstool: **Flyway** oder **Liquibase**.
- Initiales Schema für sauberen Neuaufbau.
- Seed-Skript für Testdaten & Menüdaten von Pizza Pazzia.

---

## 18. Teststrategie

- **Backend:**
  - Unit Tests für Services (insb. Preisberechnung & Snapshotting)
  - Repository-Tests
  - REST API / Integrationstests (`@SpringBootTest`, `MockMvc`)
  - Validierungs- und Statusänderungstests
- **Frontend:**
  - Service-Tests
  - Komponententests (Warenkorb, Checkout, Preisberechnung)

---

## 19. Docker & Umgebungskonfiguration

- `docker-compose.yml` zum einzeiligen Start von:
  - PostgreSQL Database
  - Spring Boot Backend
  - Angular Frontend
- Konfiguration strikt über Umgebungsvariablen (`application.yml` & `.env`), keine Hardcoded-Secrets!
- Erforderliche Variablen u. a.: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (manuell in der Google Cloud Console erzeugt), `ADMIN_BOOTSTRAP_EMAIL` (erster freigeschalteter Admin, siehe [ADR-0001](../docs/adr/0001-google-oauth-admin-auth.md)).

---

## 20. Projektstruktur

```
pizza-shop/
 ├── backend/
 │    ├── src/main/java/com/pizzashop/
 │    │    ├── controller/
 │    │    ├── service/
 │    │    ├── repository/
 │    │    ├── entity/
 │    │    ├── dto/
 │    │    ├── mapper/
 │    │    ├── exception/
 │    │    ├── security/
 │    │    └── config/
 │    └── src/test/...
 ├── frontend/
 │    └── src/app/
 │         ├── core/
 │         ├── shared/
 │         ├── shop/
 │         ├── cart/
 │         ├── checkout/
 │         └── admin/
 ├── docs/
 │    ├── adr/
 │    │    ├── 0001-google-oauth-admin-auth.md
 │    │    └── 0002-payment-status-field-not-entity.md
 │    └── Pizza_Shop_Master_Prompt.md
 ├── CONTEXT.md
 ├── docker-compose.yml
 └── README.md
```

---

## 21. Entwicklungsphasen (Vorgehensmodell)

1. **Phase 1 – Analyse:** Anforderungsanalyse, Defaults & technische Anfragen klären.
2. **Phase 2 – Architektur:** Datenmodell, ERD, REST-Konzept & Struktur freigeben lassen.
3. **Phase 3 – Backend:** Spring Boot, JPA, Security, REST, Validation, Flyway & Tests.
4. **Phase 4 – Frontend:** Angular App, Routing, Cart, Checkout, Admin Dashboard.
5. **Phase 5 – Integration:** CORS, Ende-zu-Ende Schnittstellenanbindung.
6. **Phase 6 – Dockerization:** Multi-Stage Builds & `docker-compose`.
7. **Phase 7 – E2E Test & Release:** Vollständiger Durchstich des Bestellprozesses.