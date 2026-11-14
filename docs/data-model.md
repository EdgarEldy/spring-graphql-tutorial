# Data Model

## Two domains in one schema

The database schema covers two separate domains: **Identity/RBAC** and **E-commerce**.
They share no foreign keys: a user account has nothing to do with a customer record.

Both domains were introduced together in a single migration (`V1__init_schema.sql`)
rather than being split across multiple files. This matches the project structure where
all entities exist from the start, unlike a step-by-step SBT tutorial where each
feature branch adds its own table.

## Identity / RBAC tables

| Table | Description |
|---|---|
| `app_users` | User accounts (`email`, `password_hash`, `first_name`, `last_name`, `enabled`, `account_locked`) |
| `roles` | Named roles (`role_name`, unique) |
| `permissions` | Fine-grained permissions (`resource`, `action`; unique pair) |
| `role_user` | Many-to-many join: users to roles |
| `role_permission` | Many-to-many join: roles to permissions |
| `activation_tokens` | Single-use tokens emailed on registration |
| `blacklisted_tokens` | Access token JTIs that have been logged out (`jti UUID`, `expires_at`) |
| `password_reset_tokens` | Single-use tokens emailed on password-reset request |

Refresh tokens are **not** stored in the database. They are stateless signed JWTs
(distinguished by a `token_type: "refresh"` claim). Only the access token's `jti` is
blacklisted on logout.

## E-commerce tables

| Table | Description |
|---|---|
| `categories` | Product categories (`category_name` unique; `created_at`, `updated_at` from JPA auditing) |
| `products` | Sellable items (`product_name`, `unit_price`, `category_id` FK, `version` for optimistic locking) |
| `customers` | Buyer records (`first_name`, `last_name`, `telephone`, `email` unique case-insensitive, `address`) |
| `orders` | Placed orders (`customer_id` FK, `product_id` FK, `quantity`, `total`) |

Database constraints beyond NOT NULL:

| Table | Constraint | Rule |
|---|---|---|
| `products` | `CHECK` | `unit_price > 0` |
| `orders` | `CHECK` | `quantity > 0` |
| `orders` | `CHECK` | `total > 0` |
| `categories` | `UNIQUE` | `category_name` |
| `products` | `UNIQUE` | `product_name` per category (pending) |
| `permissions` | `UNIQUE` | `(resource, action)` pair |

## JPA entity design choices

**All associations are `FetchType.LAZY`** - this is intentional. No relationship is
ever loaded eagerly by Hibernate. Field resolvers (`@SchemaMapping`) in combination with
DataLoaders are responsible for loading related data on demand, one batched SQL per
relationship type. This avoids N+1 without `@EntityGraph` or `JOIN FETCH`.

**`@Version` on `Product`** - enables optimistic locking. `createProduct` sets `version`
to 0. Each `updateProduct` increments it. If two clients update the same product
concurrently with the same version, `OptimisticLockingFailureException` is thrown and
mapped to a `CONFLICT` classification by `GraphQlExceptionResolver`.

**`@CreatedDate` / `@LastModifiedDate` on `Category`** - JPA auditing fields, populated
automatically by `JpaAuditingConfig` (`@EnableJpaAuditing`). Exposed as `createdAt` and
`updatedAt` in the `Category` GraphQL type. `Product`, `Customer`, and `Order` do not
have auditing fields.

## Flyway migration history

| Version | File | Description |
|---|---|---|
| V1 | `V1__init_schema.sql` | Complete initial schema (both domains) |
| V2 | *(absent)* | Reserved |
| V3 | `V3__add_audit_fields_to_categories.sql` | `created_at` / `updated_at` columns on `categories` |
| V4 | `V4__add_version_to_products.sql` | `version` column on `products` |

V2 is intentionally absent (there was no second migration step). The numbering mirrors
SBT's convention where V3 and V4 added audit fields and the version column to the same
tables - but SGT consolidated all identity and RBAC tables that SBT added across V2-V9
into a single V1 file.
