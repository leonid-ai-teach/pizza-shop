-- Guards against lost updates when two staff change the same order's status concurrently:
-- without it both can read NEW, both pass the transition check, and the later commit wins.
ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
