# spring-graphql-tutorial

A complete tutorial for building a **GraphQL API** with **Spring for GraphQL**, on top of **Spring Boot 4.1.x** (Spring Framework 7, Java 17), exposing an identity/RBAC domain and an e-commerce domain behind a single GraphQL schema.

This document is the **complete specification** of the project: it is meant to be followed step by step to implement each branch.

## Table of contents

- [GraphQL vs. REST: the actual difference](#graphql-vs-rest-the-actual-difference)
- [How GraphQL works with Spring](#how-graphql-works-with-spring)
- [Two independent domains, one schema](#two-independent-domains-one-schema)
- [Tech stack](#tech-stack)
- [Data model](#data-model)
- [Branching strategy](#branching-strategy)
- [Project structure](#project-structure)
- [Standard response format](#standard-response-format)
- [feature/core-architecture](#featurecore-architecture)
- [feature/auth](#featureauth)
- [feature/categories](#featurecategories)
- [feature/products](#featureproducts)
- [feature/customers](#featurecustomers)
- [feature/orders](#featureorders)
- [Order of work](#order-of-work)
- [Code conventions](#code-conventions)
- [Concepts covered](#concepts-covered)
- [How to follow this tutorial](#how-to-follow-this-tutorial)

## GraphQL vs. REST: the actual difference

The two are often contrasted as "different ways to write endpoints", which understates what actually changes.

**REST models a resource, GraphQL models a graph of types.** In REST, each endpoint returns a *fixed* shape decided by the server (`GET /products/{id}` always returns the same fields, whether the client needs all of them or one). In GraphQL, the server exposes a schema - a graph of typed fields, each independently resolvable - and the client's *query* decides which fields come back. Asking for a product's `name` and `unitPrice` only, versus asking for those plus its `category.name`, is the same field on the server (`product`), with two different queries.

**One endpoint, not many.** A REST API is a set of URLs (`/categories`, `/products`, `/customers`, `/orders`, each with its own shape). A GraphQL API is a single endpoint (conventionally `POST /graphql`); what changes between requests is the query body, not the URL.

**No more over-fetching or under-fetching.** Over-fetching: a REST client that only needs a product's name still receives the full `ProductResponse` DTO. Under-fetching: a REST client that needs a role and every permission attached to it typically needs two round trips (`GET /roles/{id}`, then `GET /roles/{id}/permissions`) or a bespoke endpoint built for that one screen. A single GraphQL query can ask for exactly that shape in one round trip, and the server resolves each field only when it's actually requested.

**The trade-off, stated honestly.** GraphQL moves complexity from "how many endpoints do we maintain" to "how do we make sure a client can't ask for something that resolves thousands of database rows in one request" - this is precisely what makes the N+1 problem (see [How GraphQL works with Spring](#how-graphql-works-with-spring)) a first-class concern in GraphQL in a way it usually isn't in REST, where each endpoint's query pattern is fixed and known in advance.

## How GraphQL works with Spring

Spring for GraphQL (`spring-boot-starter-graphql`) sits on top of **GraphQL Java** the same way Spring MVC sits on top of the Servlet API: it doesn't reimplement GraphQL, it provides Spring-idiomatic wiring around an existing engine.

- **Schema-first, not code-first**: the schema is written by hand in `.graphqls` files (SDL - Schema Definition Language), and Java code is written to match it. The schema is the contract; the code implements it, not the other way around.
- **Controllers are still Spring beans**, just with different annotations: `@QueryMapping` and `@MutationMapping` replace `@GetMapping`/`@PostMapping` for the root `Query`/`Mutation` types, `@SchemaMapping` resolves a field on any other type (e.g. `Role.permissions`), and `@SubscriptionMapping` returns a reactive `Flux` for real-time fields. Standard Spring mechanisms - dependency injection, `@PreAuthorize`, validation - apply to these methods exactly as they would to a REST controller.
- **Every field is independently resolvable**, which is the direct cause of the N+1 problem: if a query asks for 50 users and each user's `roles` field, GraphQL Java calls the `roles` resolver once per user by default - 50 separate database queries for something a single `JOIN` could answer. `DataLoader` (via Spring for GraphQL's `BatchLoaderRegistry`) exists specifically to batch these into one query per field, per request, regardless of how many objects need that field resolved.
- **Errors don't map to HTTP status codes.** A GraphQL response is always `200 OK` at the transport level (barring transport-level failures) - success and failure are both expressed inside the response body, via the `data` and `errors` fields. This is why this tutorial defines its own consistent error-classification convention (see [Standard response format](#standard-response-format)) rather than relying on HTTP status codes the way a REST API typically would.

## Two independent domains, one schema

The identity/RBAC domain and the e-commerce domain are **not** linked at the data level - there is no foreign key between `customers` and `users`, each keeps the exact shape of its source EER. A `Customer` is a standalone record; it does not resolve to a `User`.

What combining them behind one GraphQL schema still buys: a single endpoint, a single authentication mechanism, and a single query capable of asking for unrelated things in one round trip (e.g. the current admin's own profile *and* the product catalog, in one request) - the benefit of one schema doesn't require the underlying domains to share data, only to share an API surface.

## Tech stack

| Component | Choice |
|---|---|
| Framework | Spring Boot 4.1.x (Spring Framework 7) |
| Language | Java 17 (LTS) |
| Build | Maven |
| GraphQL | Spring for GraphQL (`spring-boot-starter-graphql`), built on GraphQL Java |
| Database | PostgreSQL 16 (via Docker Compose) |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| N+1 batching | `DataLoader` (via GraphQL Java's `BatchLoaderRegistry`, wired through Spring for GraphQL) |
| Security | Spring Security 7 + JWT, propagated into the GraphQL execution context |
| Real-time | GraphQL subscriptions over WebSocket |
| Schema exploration | GraphiQL (enabled in the `dev` profile) |
| Monitoring | Spring Boot Actuator |
| Tests | JUnit 5, Mockito, `HttpGraphQlTester`/`WebSocketGraphQlTester`, Testcontainers |
| CI/CD | GitHub Actions |
| Containerization | Docker, docker-compose |

## Data model

Two independent schemas, one shared database, no cross-domain foreign key.

```
users (id, first_name, last_name, email, password, enabled, account_locked)
    │ N──N (via role_user)
roles (id, role_name)
    │ N──N (via role_permission)
permissions (id, resource, action)

activation_tokens (id, user_id, token, created_at, expires_at, validated_at)
blacklisted_tokens (id, user_id, token, jti, blacklisted_at, created_at, expires_at, validated_at)
password_reset_tokens (id, user_id, token, type, expiry_date)

categories (id, category_name)
    │ 1
    │
    │ N
products (id, category_id, product_name, unit_price)
    │ 1
    │
    │ N
orders (id, customer_id, product_id, quantity, total)
    │ N
    │
    │ 1
customers (id, first_name, last_name, telephone, email, address)
```

## Branching strategy

| Branch | Role |
|---|---|
| `master` | Stable, production-ready code. No direct commits, only merges from `develop`. |
| `develop` | Integration branch. |
| `feature/core-architecture` | Project structure, GraphQL configuration, schema plumbing, exception mapping, Docker, CI. |
| `feature/auth` | Identity/RBAC: authentication, user/role/permission CRUD, role-to-user and permission-to-role assignment. |
| `feature/categories` | `Category` queries and mutations. |
| `feature/products` | `Product` queries and mutations, with `DataLoader` batching on `Product.category`. |
| `feature/customers` | `Customer` queries and mutations. |
| `feature/orders` | `Order` queries/mutations, `DataLoader` batching, and an `orderCreated` subscription. |

## Project structure

```
spring-graphql-tutorial/
├── src/
│   ├── main/
│   │   ├── java/com/edgareldy/springgraphqltutorial/
│   │   │   ├── SpringGraphqlTutorialApplication.java
│   │   │   ├── config/
│   │   │   │   ├── GraphQlConfig.java             (RuntimeWiringConfigurer, scalar registration)
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── DataLoaderConfig.java           (BatchLoaderRegistry registrations)
│   │   │   ├── entity/
│   │   │   │   ├── User.java, Role.java, Permission.java
│   │   │   │   ├── ActivationToken.java, BlacklistedToken.java, PasswordResetToken.java
│   │   │   │   ├── Category.java, Product.java, Customer.java, Order.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java, RoleRepository.java, PermissionRepository.java
│   │   │   │   ├── CategoryRepository.java, ProductRepository.java,
│   │   │   │   │   CustomerRepository.java, OrderRepository.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java, RoleService.java, PermissionService.java,
│   │   │   │   │   CategoryService.java, ProductService.java,
│   │   │   │   │   CustomerService.java, OrderService.java   (contracts)
│   │   │   │   └── impl/ (one *ServiceImpl per interface)
│   │   │   ├── graphql/
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java          (@MutationMapping: register, login, activateAccount, ...)
│   │   │   │   │   ├── UserController.java           (@QueryMapping, @MutationMapping: user CRUD, role assignment)
│   │   │   │   │   ├── RoleController.java            (@QueryMapping, @MutationMapping: role CRUD, permission assignment)
│   │   │   │   │   ├── CategoryController.java
│   │   │   │   │   ├── ProductController.java
│   │   │   │   │   ├── CustomerController.java
│   │   │   │   │   └── OrderController.java          (@QueryMapping, @MutationMapping, @SubscriptionMapping)
│   │   │   │   ├── resolver/
│   │   │   │   │   ├── UserRolesResolver.java          (@SchemaMapping: User.roles, via DataLoader)
│   │   │   │   │   ├── RolePermissionsResolver.java     (@SchemaMapping: Role.permissions, via DataLoader)
│   │   │   │   │   ├── ProductCategoryResolver.java   (@SchemaMapping: Product.category, via DataLoader)
│   │   │   │   │   └── OrderFieldResolver.java          (@SchemaMapping: Order.customer, Order.product)
│   │   │   │   └── exception/
│   │   │   │       └── GraphQlExceptionResolver.java   (DataFetcherExceptionResolver, maps exceptions to structured GraphQL errors)
│   │   │   ├── security/
│   │   │   │   ├── JwtService.java
│   │   │   │   └── JwtContextInterceptor.java          (WebGraphQlInterceptor: extracts the JWT, puts the current user in the GraphQL context)
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java
│   │   │       └── BusinessRuleException.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── graphql/
│   │       │   └── schema.graphqls                    (types, queries, mutations, subscriptions)
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   └── test/
│       └── java/com/edgareldy/springgraphqltutorial/
│           ├── graphql/       (HttpGraphQlTester / WebSocketGraphQlTester)
│           ├── service/       (Mockito)
│           └── repository/    (@DataJpaTest)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

## Standard response format

GraphQL already has its own standard envelope (`data`, `errors`, `extensions`) - see [How GraphQL works with Spring](#how-graphql-works-with-spring) for why this project does not additionally wrap results in a custom object. The convention that needs standardizing here is **how every error is shaped**, kept consistent through one central resolver.

```java
@Component
public class GraphQlExceptionResolver implements DataFetcherExceptionResolver {
    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable exception, DataFetchingEnvironment env) {
        return Mono.just(List.of(GraphqlErrorBuilder.newError(env)
                .message(exception.getMessage())
                .errorType(classify(exception))          // NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR
                .extensions(Map.of("classification", classify(exception).toString()))
                .build()));
    }
}
```

- Every exception surfaces as a GraphQL error with a consistent `extensions.classification` value (`NOT_FOUND`, `BAD_REQUEST`, `FORBIDDEN`, `INTERNAL_ERROR`), so clients can branch on it the same way they would branch on an HTTP status code in a REST API
- `ResourceNotFoundException` → `NOT_FOUND`, validation failures → `BAD_REQUEST`, `BusinessRuleException` → `BAD_REQUEST` with a business-specific message, anything unmapped → `INTERNAL_ERROR` with a generic message (never leaking the raw exception to the client)
- Partial responses are expected and normal in GraphQL: a query can return `data` for the fields that resolved successfully alongside `errors` for the ones that failed - this is not an error state to work around, it's how the protocol is designed to behave

## feature/core-architecture

### Tasks

- [x] Initialize the project (Maven, Java 17, Spring Boot 4.1.x)
- [x] Dependencies: `spring-boot-starter-graphql`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-websocket` (for subscriptions), `spring-boot-starter-actuator`, `flyway-core`, `postgresql`, `lombok`
- [x] Test dependencies: `spring-boot-starter-test` (includes `HttpGraphQlTester`), `testcontainers`
- [x] `schema.graphqls`: base `Query`, `Mutation`, `Subscription` root types (empty, extended by each following branch), scalar declarations if needed
- [x] `GraphQlConfig`: `RuntimeWiringConfigurer` for any custom scalar
- [x] `GraphQlExceptionResolver`
- [x] `DataLoaderConfig` skeleton - no loaders registered yet, later branches add their own
- [x] Flyway script `V1__init_schema.sql` (all tables from both domains)
- [x] GraphiQL enabled at `/graphiql` in the `dev` profile only
- [x] `docker-compose.yml` (app + PostgreSQL), `.github/workflows/ci.yml`

## feature/auth

Authentication, plus full administration of users, roles, and permissions.

### Schema - authentication (root `Mutation` type)

| Operation | Description |
|---|---|
| `register(input: RegisterInput!): AuthPayload!` | Creates a disabled user + activation token |
| `activateAccount(token: String!): Boolean!` | Enables the account |
| `login(input: LoginInput!): AuthPayload!` | Returns a JWT |
| `logout: Boolean!` | Blacklists the current JWT |
| `requestPasswordReset(email: String!): Boolean!` | Generates a password-reset token |
| `resetPassword(token: String!, newPassword: String!): Boolean!` | Consumes the token, updates the password |
| `me: User` | The currently authenticated user |

### Schema - user administration (ADMIN)

| Operation | Description |
|---|---|
| `users(page: Int, size: Int): UserPage!` | Paginated list |
| `user(id: ID!): User` | Detail, including resolved `roles` |
| `createUser(input: CreateUserInput!): User!` | Create an account directly (bypasses the public activation flow) |
| `updateUser(id: ID!, input: UpdateUserInput!): User!` | Update profile fields |
| `lockUser(id: ID!): User!` | Sets `account_locked = true` |
| `unlockUser(id: ID!): User!` | Sets `account_locked = false` |
| `deleteUser(id: ID!): Boolean!` | Delete |
| `assignRoleToUser(userId: ID!, roleId: ID!): User!` | Adds a row to `role_user` |
| `removeRoleFromUser(userId: ID!, roleId: ID!): User!` | Removes it |

### Schema - role/permission administration (ADMIN)

| Operation | Description |
|---|---|
| `roles: [Role!]!` | List, including resolved `permissions` |
| `role(id: ID!): Role` | Detail |
| `permissions: [Permission!]!` | List |
| `createRole(input: RoleInput!): Role!` | Create |
| `updateRole(id: ID!, input: RoleInput!): Role!` | Update |
| `deleteRole(id: ID!): Boolean!` | Delete |
| `createPermission(input: PermissionInput!): Permission!` | Create |
| `deletePermission(id: ID!): Boolean!` | Delete |
| `assignPermissionToRole(roleId: ID!, permissionId: ID!): Role!` | Adds a row to `role_permission` |
| `removePermissionFromRole(roleId: ID!, permissionId: ID!): Role!` | Removes it |

### Tasks

- [x] `User`, `Role`, `Permission` entities (`@ManyToMany` via `role_user`/`role_permission`), `ActivationToken`, `BlacklistedToken`, `PasswordResetToken`
- [x] `UserRepository`, `RoleRepository`, `PermissionRepository`
- [x] `UserService`, `RoleService`, `PermissionService` (interfaces) + implementations: `RoleService`/`UserService` own the assignment operations (`assignRoleToUser`, `assignPermissionToRole`, and their inverses), since assigning a role is a relationship change on the aggregate, not a separate entity to manage
- [x] `JwtService`: signs/validates tokens
- [x] `JwtContextInterceptor` (`WebGraphQlInterceptor`): reads the `Authorization` header on every GraphQL request (including the WebSocket handshake for subscriptions), resolves the current user, and adds it to the `GraphQLContext`
- [x] `AuthController`: authentication mutations, delegating to `UserService`
- [x] `UserController`: user administration queries/mutations, including the two assignment mutations
- [x] `RoleController`: role/permission administration queries/mutations, including the two assignment mutations
- [x] `UserRolesResolver` (`@SchemaMapping(typeName = "User", field = "roles")`) and `RolePermissionsResolver` (`@SchemaMapping(typeName = "Role", field = "permissions")`), both batched via `DataLoader` - listing users with their roles, or roles with their permissions, is exactly the N+1-prone pattern this tutorial's `DataLoader` convention exists for
- [x] `DataLoaderConfig` updated with both loaders
- [x] Authorization via Spring Security method annotations (`@PreAuthorize`) directly on controller methods - administration mutations require `ROLE_ADMIN`, authentication mutations stay public or require only an authenticated session as appropriate
- [x] Extend `schema.graphqls` with `User`, `Role`, `Permission`, `UserPage`, `RegisterInput`, `LoginInput`, `AuthPayload`, `CreateUserInput`, `UpdateUserInput`, `RoleInput`, `PermissionInput`
- [x] Tests: `HttpGraphQlTester` covering register → activate → login, the full admin flow (create a role, create a permission, assign the permission to the role, create a user, assign the role to the user), a `DataLoader` batching test on `User.roles`, and a `@PreAuthorize`-protected mutation rejecting a non-admin request

## feature/categories

### Schema

| Operation | Description |
|---|---|
| `categories(page: Int, size: Int): CategoryPage!` | Paginated list |
| `category(id: ID!): Category` | Detail |
| `createCategory(input: CategoryInput!): Category!` | Create (ADMIN) |
| `updateCategory(id: ID!, input: CategoryInput!): Category!` | Update (ADMIN) |
| `deleteCategory(id: ID!): Boolean!` | Delete (ADMIN) |

### Tasks

- [x] `Category` entity, repository, contract/implementation service
- [x] `CategoryController` (`@QueryMapping`/`@MutationMapping`)
- [x] Business rule: deleting a category that still has products is rejected (`BusinessRuleException` → `BAD_REQUEST`)
- [x] Extend `schema.graphqls`: `Category`, `CategoryPage`, `CategoryInput`
- [x] Tests: `HttpGraphQlTester` for every query and mutation, including the rejection case above

## feature/products

Depends on `feature/categories` existing, since every product references one.

### Schema

| Operation | Description |
|---|---|
| `products(categoryId: ID, page: Int, size: Int): ProductPage!` | Paginated list, optional category filter |
| `product(id: ID!): Product` | Detail |
| `createProduct(input: ProductInput!): Product!` | Create (ADMIN) |
| `updateProduct(id: ID!, input: ProductInput!): Product!` | Update (ADMIN) |
| `deleteProduct(id: ID!): Boolean!` | Delete (ADMIN) |

### Tasks

- [ ] `Product` entity, repository, contract/implementation service
- [ ] `ProductController` (`@QueryMapping`/`@MutationMapping`)
- [ ] `ProductCategoryResolver` (`@SchemaMapping(typeName = "Product", field = "category")`): resolves each product's category through a registered `DataLoader`, so listing 50 products triggers **one** batched category query instead of 50 individual ones
- [ ] `DataLoaderConfig` updated: `BatchLoaderRegistry.forTypePair(Long.class, Category.class).registerMappedBatchLoader(...)`
- [ ] Extend `schema.graphqls`: `Product`, `ProductPage`, `ProductInput`
- [ ] Tests: `HttpGraphQlTester` for queries/mutations, and a dedicated test asserting the `DataLoader` actually batches (assert the number of SQL queries issued for a list of N products stays constant, not proportional to N)

## feature/customers

### Schema

| Operation | Description |
|---|---|
| `customers(page: Int, size: Int): CustomerPage!` | Paginated list |
| `customer(id: ID!): Customer` | Detail |
| `createCustomer(input: CustomerInput!): Customer!` | Create |
| `updateCustomer(id: ID!, input: CustomerInput!): Customer!` | Update |
| `deleteCustomer(id: ID!): Boolean!` | Delete |

### Tasks

- [ ] `Customer` entity, repository, contract/implementation service
- [ ] `CustomerController`
- [ ] Extend `schema.graphqls`: `Customer`, `CustomerPage`, `CustomerInput`
- [ ] Tests

## feature/orders

### Schema

| Operation | Description |
|---|---|
| `orders(customerId: ID, page: Int, size: Int): OrderPage!` | Paginated list |
| `order(id: ID!): Order` | Detail |
| `createOrder(input: OrderInput!): Order!` | Create (computes `total`) |
| `orderCreated: Order!` | **Subscription**: streams every new order as it's created |

### Tasks

- [ ] `Order` entity, `OrderRepository`, `OrderService` (interface) + implementation: computes `total = quantity * product.unitPrice`
- [ ] `OrderController`: `@QueryMapping`/`@MutationMapping` for queries/mutations, `@SubscriptionMapping` returning a `Flux<Order>` for `orderCreated`
- [ ] `OrderFieldResolver` (`@SchemaMapping` for `Order.customer` and `Order.product`), both batched via `DataLoader`
- [ ] `Sinks.Many<Order>` bean: `OrderService` emits into it after persisting a new order, `orderCreated`'s `Flux` is `sink.asFlux()`
- [ ] Extend `schema.graphqls`: `Order`, `OrderPage`, `OrderInput`, and the `orderCreated` field on the `Subscription` type
- [ ] Tests: `HttpGraphQlTester` for queries/mutations, `WebSocketGraphQlTester` subscribing to `orderCreated` and asserting an event arrives after a `createOrder` mutation, and a `DataLoader` batching test for `Order.customer`/`Order.product` symmetric to the one in `feature/products`

## Order of work

1. `feature/core-architecture` → Pull Request to `develop`
2. `feature/auth` (depends on `core-architecture`) → Pull Request to `develop`
3. `feature/categories` (depends on `core-architecture`) → Pull Request to `develop`
4. `feature/products` (depends on `categories`) → Pull Request to `develop`
5. `feature/customers` (depends on `core-architecture`) → Pull Request to `develop`
6. `feature/orders` (depends on `products`, `customers`) → Pull Request to `develop`
7. `develop` → `master`

## Code conventions

- Root package: `com.edgareldy.springgraphqltutorial`
- **Contract/implementation services**: interface at the root of `service/`, implementation in `service/impl/`
- GraphQL controllers (`@QueryMapping`/`@MutationMapping`/`@SubscriptionMapping`) never contain business logic - they delegate to a service
- Any relationship field that resolves a collection or a related entity (`User.roles`, `Role.permissions`, `Product.category`, `Order.customer`, `Order.product`) is resolved through a `DataLoader`, never a direct repository call inside a `@SchemaMapping` method - a direct call reintroduces the N+1 problem GraphQL is otherwise prone to
- A relationship assignment (`assignRoleToUser`, `assignPermissionToRole`) is always a mutation on the owning service (`RoleService`/`UserService`), never a field-level side effect inside a resolver
- Every exception is mapped to a structured GraphQL error with a consistent `extensions.classification`, never left to surface as a raw stack trace
- Schema-first: `schema.graphqls` is the source of truth, Java types are written to match it, not the other way around

## Concepts covered

- Schema-first GraphQL with Spring for GraphQL (`.graphqls` files, `@QueryMapping`/`@MutationMapping`/`@SubscriptionMapping`/`@SchemaMapping`)
- The N+1 problem and solving it with `DataLoader`/`BatchLoaderRegistry`
- GraphQL subscriptions over WebSocket for real-time updates
- Structured, consistent error handling (`DataFetcherExceptionResolver`, `extensions.classification`)
- Propagating authentication into the GraphQL execution context (`WebGraphQlInterceptor`)
- Reusing standard Spring Security method annotations (`@PreAuthorize`) directly on GraphQL controller methods
- Modeling many-to-many relationship management (role-to-user, permission-to-role) as explicit mutations
- Combining independent domains behind a single schema without forcing a data-level relationship between them
- Testing GraphQL APIs (`HttpGraphQlTester`, `WebSocketGraphQlTester`)
- Containerization (Docker, docker-compose)
- Continuous integration (GitHub Actions)

## How to follow this tutorial

1. Clone the repository and check out `develop`
2. Follow the branches in order: `feature/core-architecture` → `feature/auth` → `feature/categories` → `feature/products` → `feature/customers` → `feature/orders`
3. Run `docker-compose up`, then open GraphiQL at `http://localhost:8080/graphiql` to explore the schema and run queries/mutations/subscriptions interactively
