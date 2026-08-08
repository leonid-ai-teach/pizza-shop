# Spec: Customer Ordering Flow (V1)

## Problem Statement

A customer wants to order pizza for delivery or pickup without creating an account or paying online. Today there is no online ordering channel at all — the menu, cart, and order submission don't exist yet. The customer needs to browse the current menu, build a pizza with their own choice of toppings, see the price update as they configure it, collect multiple pizzas into an order, and submit that order with their contact details, receiving a clear confirmation with an order number.

## Solution

Build the public-facing (unauthenticated) ordering flow: a menu page backed by real database data, a pizza configuration view with live price calculation, a cart, and a checkout that adapts its required fields to delivery vs. pickup. Submission creates an `Order` with snapshotted prices (so later catalog price changes never retroactively affect past orders) and returns a confirmation with a durable, sequential order number. All pricing is recomputed and validated server-side — the frontend price is never trusted.

## User Stories

1. As a customer, I want to see the pizza menu on the shop's homepage, so that I know what's available to order.
2. As a customer, I want only active pizzas to appear on the menu, so that I don't try to order something no longer offered.
3. As a customer, I want pizzas displayed in the shop's chosen sort order, so that the menu matches how the shop wants it presented.
4. As a customer, I want to open a pizza to see its name, description, base price, and available toppings, so that I can decide how to configure it.
5. As a customer, I want to select additional toppings for a pizza, so that I can customize it to my taste.
6. As a customer, I want to see the toppings available for a pizza limited to those the shop has actually linked to it, so that I can't order an invalid combination.
7. As a customer, I want only active toppings offered, so that I don't select something no longer available.
8. As a customer, I want the price to update live as I add or remove toppings, so that I always know what I'm about to pay before adding it to my cart.
9. As a customer, I want to set a quantity for a configured pizza before adding it to my cart, so that I don't have to repeat the same configuration multiple times.
10. As a customer, I want to see the calculated price as (base price + sum of topping prices) × quantity, so that the math is transparent.
11. As a customer, I want to add a configured pizza to my cart, so that I can order more than one item.
12. As a customer, I want to see all items in my cart with their pizza, chosen toppings, quantity, unit price, and line total, so that I can review my order before checkout.
13. As a customer, I want to increase or decrease the quantity of a cart item, so that I can adjust my order without re-adding it.
14. As a customer, I want to remove an item from my cart, so that I can change my mind about a pizza.
15. As a customer, I want to re-open a cart item's configuration and change its toppings, so that I can correct a mistake without deleting and re-adding it.
16. As a customer, I want to see the running total of my whole cart at all times, so that I know what I'm about to pay.
17. As a customer, I want to proceed to checkout from my cart, so that I can complete my order.
18. As a customer, I want to be blocked from checking out with an empty cart, so that I don't submit a meaningless order.
19. As a customer, I want to choose between delivery and pickup at checkout, so that I get the order fulfilled the way I want.
20. As a customer choosing pickup, I want to only be asked for name, phone, and email, so that I'm not forced to enter an address I don't need.
21. As a customer choosing delivery, I want to additionally provide street, house number, postal code, and city, so that the shop knows where to deliver.
22. As a customer, I want address fields to disappear immediately when I switch from delivery to pickup, so that the form only ever shows what's relevant.
23. As a customer, I want clear validation messages if I omit a required field for my chosen order type, so that I can fix my submission before it's rejected.
24. As a customer, I want to submit my order without any online payment step, so that I can complete checkout immediately in V1.
25. As a customer, I want my submitted order's payment status to be recorded as not-required, so that the shop's system reflects that no payment is expected in V1.
26. As a customer, I want to receive a confirmation screen after successful submission, showing my order number, order details (pizzas, toppings, quantities), order type, total price, and status, so that I have proof my order was received.
27. As a customer, I want my order number to be a distinct, permanently unique, incrementing number (e.g. #100025), so that I can reference it unambiguously (e.g. when calling the shop).
28. As a customer, I want the prices in my confirmation to reflect exactly what was shown to me at checkout, so that there are no surprise charges.
29. As a customer, I want the server to reject my order if I selected a pizza or topping that has since become inactive, so that the shop never fulfills an order for something no longer offered.
30. As a customer, I want the server to reject my order if it references a pizza/topping ID that doesn't exist, so that malformed or tampered requests can't corrupt the shop's data.
31. As a customer, I want the server to calculate the total price itself rather than trust what my browser sends, so that price tampering isn't possible.
32. As a customer, I want a later change to a pizza's or topping's price in the shop's catalog to never change the price of an order I already placed, so that my receipt stays accurate historically.
33. As a customer, I want to retrieve my order confirmation again by its ID (e.g. via a link), so that I can check my order details after leaving the confirmation page.
34. As a customer, I want validation errors to come back in a consistent, structured format, so that the frontend can display them clearly regardless of which field failed.
35. As a customer, I want the shop's menu and topping data to come from the real database rather than being hardcoded, so that what I see always matches what the shop currently offers.

## Implementation Decisions

- **Backend modules (customer-facing slice only):**
  - `PizzaController` — `GET /api/pizzas`, `GET /api/pizzas/{id}` (public, active pizzas only).
  - `ToppingController` — `GET /api/toppings` (public, active toppings only).
  - `OrderController` — `POST /api/orders`, `GET /api/orders/{id}` (public).
  - `PizzaService`, `OrderService` — business logic only, no logic in controllers.
  - `PizzaRepository`, `ToppingRepository`, `OrderRepository`, `OrderItemRepository`.
  - DTOs: `PizzaResponse`, `ToppingResponse`, `CreateOrderRequest`, `OrderResponse`, `OrderItemResponse`. Entities are never returned directly from controllers.
  - Central error handling via `@RestControllerAdvice`, uniform error body (`timestamp`, `status`, `error`, `message`) per master prompt §15.
- **Entities touched:** `Pizza`, `Topping`, `PizzaTopping` (mapping table), `Order`, `OrderItem`, as defined in master prompt §4. `AdminAccess` and admin CRUD are untouched by this spec.
- **Price calculation:** line total = (pizza base price + sum of selected topping prices) × quantity, computed server-side in `OrderService`, never accepted from the client.
- **Price snapshotting:** `OrderItem.basePrice` and `OrderItem.toppingPrices` are captured at order-creation time and are immutable afterward; later catalog price edits never retroactively change existing orders.
- **Order number:** global, continuously incrementing DB sequence starting at `100000`, never reset, assigned at order creation.
- **`customerData`:** nullable `@Embeddable` on `Order` (first name, last name, phone, email, optionally street, house number, postal code, city). Address subfields are conditionally required only when `orderType = DELIVERY`; they are `null` and not validated when `orderType = PICKUP`.
- **`paymentStatus`:** set to `NOT_REQUIRED` on order creation, per [ADR-0002](../adr/0002-payment-status-field-not-entity.md) — no separate `Payment` entity in V1.
- **Order status:** new orders are created with status `NEW`. Status transitions themselves belong to the admin spec, not this one.
- **Server-side integrity checks on order creation:** every referenced pizza/topping ID must exist and have `active = true`, and every topping must actually be linked to the ordered pizza via `PizzaTopping`; otherwise the request is rejected with a validation error.
- **Frontend modules:** `shop` (menu + pizza detail/config), `cart`, `checkout`, using Angular Router, Reactive Forms, and `HttpClient` exclusively for backend communication — no hardcoded menu/topping data in the Angular app.
- **Seed data:** a one-time, manually curated Flyway seed (~8–10 pizzas, ~8–10 toppings) transcribed from the Pizzapazzia menu with real names/price levels — no runtime scraping, no requirement for full menu parity.

## Testing Decisions

- A good test here exercises observable behavior (HTTP request in → HTTP response / persisted state out, or rendered UI ← simulated user interaction), not internal method calls or private service wiring.
- **Backend — primary seam:** `@SpringBootTest` + `MockMvc` integration tests against the real `/api/pizzas`, `/api/toppings`, and `/api/orders` endpoints, running against a real Postgres instance (Testcontainers). Cover: menu/topping listing (active-only, sort order), successful order creation with correct price computation and snapshotting, order retrieval, rejection of inactive/nonexistent/unlinked pizza-topping combinations, conditional address validation by `orderType`, and the uniform error response shape. No repository or service mocking in these tests — the seam is the API boundary, per master prompt §18 ("REST API / Integrationstests (`@SpringBootTest`, `MockMvc`)").
- **Backend — supplementary unit tests:** narrow `OrderService` unit tests only for price-calculation edge cases that would be tedious to enumerate purely through the API (e.g. many topping combinations, quantity edge cases).
- **Frontend seam:** Angular `TestBed` component tests for the pizza configuration view, cart, and checkout, with `HttpTestingController` mocking only the HTTP boundary. Assert on rendered price totals, cart item add/update/remove behavior, checkout field visibility switching with `orderType`, and the exact shape of the outgoing `CreateOrderRequest` payload.
- No prior art exists in the repo yet (pre-code, planning stage) — these seams are derived directly from master prompt §18 and standard Spring Boot / Angular testing idioms, not from existing project tests.

## Out of Scope

- The entire admin area: Google OAuth login, `AdminAccess` allowlist/invite flow, admin dashboard, order management/status transitions, and pizza/topping CRUD (separate spec; see [ADR-0001](../adr/0001-google-oauth-admin-auth.md)).
- Online payment of any kind (explicitly deferred to V2).
- Editing or cancelling an order after submission from the customer side.
- Docker/deployment configuration (Phase 6 of the master prompt's phased plan).
- Notifications/emails to the customer.
- Visual/UI design polish beyond functional responsiveness across desktop, tablet, and phone.
- Rate limiting, anti-abuse, or fraud protection on order submission.
- Internationalization beyond the existing German-UI / English-code-identifier convention already established.

## Further Notes

- Menu seed data is a one-time manual transcription of a representative subset of the Pizzapazzia menu (https://pizzapazzia.de/menu) into a Flyway seed file — not a live integration and not full menu parity.
- Domain vocabulary follows the master prompt's convention: English class/field/enum names throughout the code, German only as UI-facing text (see [`docs/Pizza_Shop_Master_Prompt.md`](../Pizza_Shop_Master_Prompt.md) §"Namenskonvention"). No `CONTEXT.md` exists yet in this repo despite being referenced by the master prompt — worth creating alongside or before implementation.
- [ADR-0002](../adr/0002-payment-status-field-not-entity.md) governs the `paymentStatus` field decision referenced above; [ADR-0001](../adr/0001-google-oauth-admin-auth.md) is not directly relevant to this spec but is linked for context since it shapes the `Order`/admin boundary.
