-- Adds the optimistic locking version column to the products table so the
-- @Version annotation on Product has a backing column. Existing rows start
-- at version 0 so the first update on each row succeeds unconditionally.
ALTER TABLE products
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
