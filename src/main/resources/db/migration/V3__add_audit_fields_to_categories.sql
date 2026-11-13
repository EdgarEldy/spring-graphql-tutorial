-- Adds JPA audit columns to the categories table so @CreatedDate and
-- @LastModifiedDate on the Category entity have backing columns. The
-- migration uses a subquery default to back-fill existing rows.
ALTER TABLE categories
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Remove the synthetic defaults once existing rows are migrated so
-- Hibernate's ddl-auto: validate finds the columns without a default.
ALTER TABLE categories
    ALTER COLUMN created_at DROP DEFAULT,
    ALTER COLUMN updated_at DROP DEFAULT;
