# Spec: Admin Area (V1)

## Problem Statement

Shop staff need a protected way to see and act on incoming orders, and to keep the menu (pizzas and toppings) up to date, without the shop having to manage its own username/password accounts. Today there is no admin area, no order management, and no way to change the menu except by hand in the database.

## Solution

Build a `/admin` area protected by Google login (OAuth2), where access is granted only to email addresses an existing admin has explicitly pre-approved — there's no self-registration and no unapproved waiting state. Once logged in, an admin sees a dashboard with order counts, can list and filter orders, view full order detail, and move an order through its status lifecycle. Admins can also manage the pizza and topping catalog (create, edit, price, activate/deactivate, sort order, topping-to-pizza assignment), with already-ordered items protected from physical deletion via soft-delete (`active = false`).

## User Stories

1. As an admin, I want to log in to `/admin` using my Google account, so that I don't need a separate shop-specific password.
2. As an admin, I want to be denied access if my Google account's email isn't on the allowlist, so that only approved staff can reach admin functionality.
3. As an admin, I want any unauthenticated request to an admin API endpoint to be rejected, so that the admin API can't be reached by bypassing the login screen.
4. As the very first admin, I want my access to be provisioned via an environment variable (`ADMIN_BOOTSTRAP_EMAIL`) or Flyway seed, so that there's no chicken-and-egg problem when no admin yet exists to invite me.
5. As an existing admin, I want to invite a new admin by entering their email address into an allowlist, so that they can log in the first time already approved, with no pending/unconfirmed state.
6. As an existing admin, I want to see the list of currently approved admin emails (with who approved them and when), so that I have visibility into who has access.
7. As an admin, I want to see a dashboard when I log in, showing counts of new, in-progress, and completed orders, so that I immediately understand today's workload.
8. As an admin, I want quick access from the dashboard to incoming orders, so that I can start working without extra navigation.
9. As an admin, I want to see a list of all orders, so that I have a full picture of order activity.
10. As an admin, I want to filter the order list (e.g. by status), so that I can focus on what needs attention right now.
11. As an admin, I want to open an order's detail view and see all customer data, delivery address (if applicable), ordered pizzas, chosen toppings, and prices, so that I have everything needed to fulfill it.
12. As an admin, I want to move an order from `NEW` to `IN_PROGRESS`, so that I can reflect that the kitchen has started working on it.
13. As an admin, I want to move an order from `IN_PROGRESS` to `DONE`, so that I can reflect that it's been fulfilled.
14. As an admin, I want to cancel an order while it's `NEW` or `IN_PROGRESS`, so that I can handle situations where an order can't be completed.
15. As an admin, I want the system to reject any status change that isn't a valid transition (e.g. `DONE` → anything, `CANCELLED` → anything, `NEW` → `DONE` directly), so that the order lifecycle stays consistent and I can't accidentally corrupt order state.
16. As an admin, I want to see the shop's full pizza list (including inactive ones), so that I can manage the whole catalog, not just what customers currently see.
17. As an admin, I want to create a new pizza with a name, description, price, image link, and sort position, so that I can add it to the menu.
18. As an admin, I want to edit an existing pizza's name, description, price, image link, or sort position, so that I can keep the menu accurate and well-ordered.
19. As an admin, I want the pizza's image to be a plain URL text field pointing to an externally hosted image, so that I don't need any file upload/storage tooling in V1.
20. As an admin, I want to activate or deactivate a pizza, so that I can temporarily or permanently remove it from the customer-facing menu without losing its history.
21. As an admin, I want deactivating a pizza that's already been ordered to never physically delete it, so that historical orders referencing it stay intact.
22. As an admin, I want to see the shop's full topping list (including inactive ones), so that I can manage the whole set, not just what's currently offered.
23. As an admin, I want to create a new topping with a name, description, and price, so that I can offer it to customers.
24. As an admin, I want to edit an existing topping's name, description, or price, so that I can keep pricing accurate.
25. As an admin, I want to activate or deactivate a topping, so that I can control what's currently offerable without deleting its order history.
26. As an admin, I want to choose which toppings are available for a given pizza, so that customers only see valid combinations when configuring that pizza.
27. As an admin, I want a price change I make to a pizza or topping to never retroactively change the price shown on past orders, so that historical order records stay accurate (prices are snapshotted at order time, not looked up live).
28. As an admin, I want validation errors from admin actions (e.g. invalid price, missing required field) to come back in the same consistent structured format as the rest of the API, so that the admin UI can display them predictably.

## Implementation Decisions

- **Backend modules:**
  - `SecurityConfig` — Spring Security + OAuth2 Client (Google), securing all `/api/admin/**` routes; unauthenticated requests rejected with 401/redirect to login as appropriate for the request type.
  - An OAuth2 user service (e.g. custom `OAuth2UserService`) that, on every Google login, checks the authenticated email against `AdminAccess`; logins for emails not present in the allowlist are rejected — there is no local user record created for them.
  - `AdminAccess` entity/repository: `email` (PK), `approvedAt`, `approvedBy`. Bootstrap admin inserted via `ADMIN_BOOTSTRAP_EMAIL` env var (Flyway seed or startup check), solving the chicken-and-egg problem of inviting the first admin.
  - `AdminController` (admin identity) — `GET /api/admin/admins`, `POST /api/admin/admins` (invite-by-email; only existing admins can call this, enforced by the security layer, not by a separate role since there is only one role).
  - `OrderAdminController` — `GET /api/admin/orders` (with filtering), `GET /api/admin/orders/{id}`, `PATCH /api/admin/orders/{id}/status`.
  - `PizzaAdminController` — `GET /api/admin/pizzas`, `POST /api/admin/pizzas`, `PUT /api/admin/pizzas/{id}`, `PATCH /api/admin/pizzas/{id}/active`.
  - `ToppingAdminController` — `GET /api/admin/toppings`, `POST /api/admin/toppings`, `PUT /api/admin/toppings/{id}`, `PATCH /api/admin/toppings/{id}/active`.
  - All business logic (status transition rules, soft-delete enforcement, allowlist checks beyond authentication) lives in corresponding `*Service` classes, not in controllers.
  - **Decision (not explicit in the master prompt's endpoint table):** pizza-to-topping assignment is edited as part of `PUT /api/admin/pizzas/{id}` — the update request carries the full set of associated topping IDs, and the service reconciles the `PizzaTopping` mapping rows accordingly. No separate assignment endpoint in V1.
  - **Decision:** dashboard KPI counts (`NEW` / `IN_PROGRESS` / `DONE`) are served by a dedicated lightweight summary read (e.g. `GET /api/admin/orders/summary`) rather than being computed client-side from the full order list, since the full list may be paginated/filtered independently of the dashboard counts.
  - DTOs: `UpdateOrderStatusRequest`, `CreatePizzaRequest`, `UpdatePizzaRequest`, `CreateToppingRequest`, `UpdateToppingRequest`, plus new `InviteAdminRequest` / `AdminAccessResponse` (not listed in master prompt §16, added here since the admin-invite flow requires them). Entities are never returned directly.
- **Order status transition rules:** enforced centrally in `OrderService` (shared with the customer-ordering spec's `Order` entity) — legal transitions are `NEW → IN_PROGRESS`, `NEW → CANCELLED`, `IN_PROGRESS → DONE`, `IN_PROGRESS → CANCELLED`; `DONE` and `CANCELLED` are terminal. Any other requested transition is rejected via the standard validation error format.
- **Soft-delete:** deactivation (`active = false`) is the only removal mechanism for `Pizza`/`Topping` once referenced by any `OrderItem`; there is no hard-delete endpoint in V1 regardless of order history, keeping the rule simple and uniform.
- **Price integrity:** admin price edits only ever affect the live `Pizza`/`Topping` rows; `OrderItem.basePrice`/`toppingPrices` snapshots (defined in the customer-ordering-flow spec) are never touched by admin actions.
- **Frontend module:** `admin` — login/callback handling, a route guard redirecting unauthenticated users away from `/admin/**`, dashboard, order list + detail, pizza management, topping management, and admin (allowlist) management views, using Angular Router, Reactive Forms, and `HttpClient`.
- **Out-of-band setup (no automation):** Google OAuth2 Client ID/Secret are created manually once in Google Cloud Console and supplied via `.env`/environment variables (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`); this spec does not include automating that step.

## Testing Decisions

- A good test here exercises observable behavior at the API/security boundary or the rendered admin UI — not internal service method calls in isolation.
- **Backend — primary seam:** `@SpringBootTest` + `MockMvc` integration tests against the real `/api/admin/**` endpoints, running against a real Postgres (Testcontainers), using Spring Security's test support (e.g. `oidcLogin()`/mutators) to simulate authenticated sessions for both allowlisted and non-allowlisted emails. Cover: rejection of unauthenticated and non-allowlisted requests, admin invite flow (including that only an already-authenticated admin can invite), order listing/filtering/detail, every legal and illegal status transition, pizza/topping CRUD including activate/deactivate, soft-delete enforcement once an item has order history, and pizza↔topping assignment reconciliation. This mirrors the seam used in the [customer ordering flow spec](./customer-ordering-flow.md) — same seam type, now exercising the security filter chain as well.
- **Backend — supplementary unit tests:** narrow `OrderService` unit tests for the status-transition state machine specifically, since exhaustively enumerating every (current status × requested status) pair through full `MockMvc` round-trips would be repetitive; the integration tests then only need to cover representative legal/illegal cases end-to-end.
- **Frontend seam:** Angular `TestBed` component tests for the dashboard, order list/detail, and pizza/topping management forms, with `HttpTestingController` mocking the HTTP boundary — asserting on rendered KPI counts, filter behavior, status-transition UI (e.g. disabled/hidden actions for terminal statuses), and outgoing request payloads for CRUD actions. A separate, focused test for the route guard verifies redirect-when-unauthenticated behavior without needing a full login round-trip.
- No prior art exists in the repo yet — seams follow master prompt §18 and are kept consistent with the customer-ordering-flow spec's seam choices so the two specs don't introduce divergent testing styles.

## Out of Scope

- The customer-facing ordering flow itself (menu browsing, cart, checkout, order submission) — see [customer-ordering-flow.md](./customer-ordering-flow.md).
- Any role beyond the single "Admin" role — no permission tiers, no read-only staff accounts.
- Revoking/removing an admin's access (only the invite/add path is specified; de-provisioning is not covered here).
- Any UI for changing `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` — these remain manually configured environment variables.
- File upload/image hosting for pizza images (URL text field only, per master prompt §13).
- Online payment (not applicable — payment status is `NOT_REQUIRED` for all V1 orders, admin or otherwise).
- Notifications/emails to customers on status change.
- Reporting/analytics beyond the three dashboard counts (`NEW`/`IN_PROGRESS`/`DONE`).
- Docker/deployment configuration.

## Further Notes

- This spec assumes [customer-ordering-flow.md](./customer-ordering-flow.md) has established the `Order`, `OrderItem`, `Pizza`, `Topping`, and `PizzaTopping` entities and their price-snapshotting behavior; this spec only adds `AdminAccess` and the admin-facing controllers/services around those existing entities.
- See [ADR-0001](../adr/0001-google-oauth-admin-auth.md) for the reasoning behind Google-OAuth-only auth and the invite-allowlist model (no self-registration, no pending state), and [ADR-0002](../adr/0002-payment-status-field-not-entity.md) for why `Order` carries a `paymentStatus` field rather than a separate `Payment` entity in V1.
- The pizza-topping-assignment and dashboard-summary decisions above go slightly beyond the master prompt's explicit REST API table (§14); flagging this in case the actual shape (e.g. a dedicated assignment endpoint instead of folding it into `PUT /api/admin/pizzas/{id}`) turns out to matter for a specific frontend design.
