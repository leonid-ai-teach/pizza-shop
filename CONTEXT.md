# Glossar & Sprachkonvention

Kanonische Begriffe des Projekts. Referenziert aus
[`docs/Pizza_Shop_Master_Prompt.md`](docs/Pizza_Shop_Master_Prompt.md).

## Konvention

**Bezeichner im Code sind englisch, sichtbarer Oberflächentext ist deutsch.**

Klassen, Felder, Enum-Konstanten, API-Pfade und JSON-Felder verwenden ausschließlich
englische Begriffe (`Order`, `OrderStatus.NEW`, `/api/orders`). Deutsch erscheint nur
dort, wo ein Mensch es liest: in Angular-Templates und in Fehlermeldungen für Endnutzer.

Die Übersetzungstabellen für Enum-Werte liegen zentral in
`frontend/src/app/core/models/order-labels.ts` — beim Anzeigen eines Status oder einer
Bestellart bitte diese wiederverwenden, statt Strings erneut zu übersetzen.

## Begriffe

| Code (englisch) | UI / Fachsprache (deutsch) | Bedeutung |
| :--- | :--- | :--- |
| `Pizza` | Pizza | Katalogeintrag mit Name, Beschreibung, Grundpreis, Sortierreihenfolge |
| `Topping` | Topping | Zusätzliche Zutat mit eigenem Preis |
| `PizzaTopping` | — | Zuordnung, welche Toppings auf welcher Pizza wählbar sind |
| `Order` | Bestellung | Eine abgeschickte Bestellung |
| `OrderItem` | Bestellposition | Eine konfigurierte Pizza samt Menge innerhalb einer Bestellung |
| `OrderItemTopping` | — | Ein gewähltes Topping einer Position, inklusive Preis-Snapshot |
| `CustomerData` | Kundendaten | Kontaktdaten, bei Lieferung zusätzlich die Adresse |
| `AdminAccess` | Admin-Freigabe | Eintrag der Allowlist freigeschalteter Google-Konten |
| `orderNumber` | Bestellnummer | Fortlaufende Nummer ab 100000, die der Kunde nennt |
| `publicToken` | — | Nicht erratbarer Schlüssel der Bestätigungsseite (ersetzt die ID in öffentlichen URLs) |

## Enum-Werte

**`OrderStatus`** — Lebenszyklus einer Bestellung:

| Wert | UI-Label | Übergänge |
| :--- | :--- | :--- |
| `NEW` | Neu | → `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | In Bearbeitung | → `DONE`, `CANCELLED` |
| `DONE` | Fertiggestellt | Endzustand |
| `CANCELLED` | Storniert | Endzustand |

**`OrderType`** — `DELIVERY` (Lieferung) · `PICKUP` (Abholung)

**`PaymentStatus`** — `NOT_REQUIRED` (keine Online-Zahlung in V1, siehe
[ADR-0002](docs/adr/0002-payment-status-field-not-entity.md))
