-- Order confirmations are public (no customer login), so they cannot be addressed by the
-- sequential primary key: anyone could walk /api/orders/1..N and harvest every customer's
-- name, phone, email and delivery address. An unguessable token scopes the link to whoever
-- actually placed the order.
ALTER TABLE orders
    ADD COLUMN public_token VARCHAR(36);

-- gen_random_uuid() is built in to PostgreSQL 13+ and supported by H2 2.x, so this backfill
-- runs unchanged against both the production database and the test one.
UPDATE orders
SET public_token = gen_random_uuid()
WHERE public_token IS NULL;

ALTER TABLE orders
    ALTER COLUMN public_token SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT uk_orders_public_token UNIQUE (public_token);
