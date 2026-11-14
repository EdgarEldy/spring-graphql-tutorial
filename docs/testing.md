# Testing

## Test architecture

All integration tests extend `GraphQlIntegrationTestSupport`, a shared base class that:
- Annotates the test with `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- Imports `TestcontainersConfiguration`, which starts a PostgreSQL 16 container via
  Testcontainers for the duration of the test run.
- Builds an `HttpGraphQlTester` bound to the real embedded HTTP server.
- Exposes helpers used across test classes: `bootstrapAdminToken()`, `login()`,
  `authenticatedTester(token)`, `createEnabledUser()`, `uniqueEmail()`.

There are no `@WebMvcTest` slices in this project. Every controller test hits the full
Spring context, the real Flyway migrations, and a real PostgreSQL instance.

## Test categories

### GraphQL integration tests (`graphql/` package)

Each controller gets its own test class. Tests exercise the full stack from the
GraphQL document down to the database.

```
graphql/
  AuthControllerTest.java
  CategoryControllerTest.java
  CustomerControllerTest.java
  OrderControllerTest.java
  ProductControllerTest.java
  UserControllerTest.java
  RoleControllerTest.java
  PermissionControllerTest.java
  DataLoaderBatchingTest.java
  OrderSubscriptionTest.java
```

**`HttpGraphQlTester`** sends queries and mutations over HTTP. Example:

```java
graphQlTester.document("""
    mutation($input: LoginInput!) {
      login(input: $input) { accessToken }
    }
    """)
    .variable("input", Map.of("email", "admin@example.com", "password", "Admin1@seed"))
    .execute()
    .path("login.accessToken")
    .entity(String.class)
    .satisfies(token -> assertThat(token).isNotBlank());
```

Authenticated tests use `authenticatedTester(token)`, which wraps the base tester with
an `Authorization: Bearer <token>` header:

```java
HttpGraphQlTester admin = authenticatedTester(bootstrapAdminToken());
admin.document("{ users(page: 0, size: 10) { totalElements } }")
     .execute()
     .path("users.totalElements")
     .entity(Integer.class)
     .satisfies(n -> assertThat(n).isGreaterThan(0));
```

**`bootstrapAdminToken()`** runs the full register-then-login flow against the real
endpoint (email: `admin@example.com`, password: `Admin1@seed`). It is idempotent: if
the admin user already exists (from `DataSeeder` or a previous test), it skips
registration and logs in directly.

The base class also exposes `roleRepository` directly, so tests can seed roles or
permissions without going through the GraphQL API:

```java
Role role = roleRepository.save(Role.builder().roleName("REPORTS_VIEWER_" + userId).build());
```

### DataLoader batching test (`DataLoaderBatchingTest`)

Verifies that DataLoaders issue exactly one SQL query per relationship type, regardless
of how many parent entities are in the response. Uses Hibernate Statistics to count
executed queries:

```java
SessionFactory sf = entityManager.getEntityManagerFactory()
    .unwrap(SessionFactory.class);
sf.getStatistics().setStatisticsEnabled(true);
sf.getStatistics().clear();

// ... execute GraphQL query that fetches N products with their category ...

long queryCount = sf.getStatistics().getQueryExecutionCount();
assertThat(queryCount).isEqualTo(2); // 1 for products + 1 batch for categories
```

The four tests cover: `User.roles`, `Role.permissions`, `Product.category`,
and `Order.customer` + `Order.product`.

### Subscription test (`OrderSubscriptionTest`)

Uses `WebSocketGraphQlTester` to connect over WebSocket and verifies real-time delivery
of an `orderCreated` event:

```java
WebSocketGraphQlTester wsTester = WebSocketGraphQlTester.builder(
    "ws://localhost:" + port + "/graphql",
    new StandardWebSocketClient()
).build();

Flux<Map> events = wsTester.document("subscription { orderCreated { id quantity } }")
    .executeSubscription()
    .toFlux("orderCreated", Map.class);

StepVerifier.create(events)
    .thenAwait(Duration.ofMillis(500))   // wait for subscription handshake
    .then(() -> /* createOrder mutation */)
    .expectNextMatches(event -> /* assert */)
    .thenCancel()
    .verify(Duration.ofSeconds(10));
```

The `thenAwait(500ms)` before the mutation is intentional: without it, the `createOrder`
mutation might execute before the WebSocket subscription is fully established, and the
event would be dropped (`directBestEffort` does not buffer for late subscribers).

### Repository tests (`repository/` package)

Each repository gets a `@DataJpaTest` class that tests:
- The custom query methods used by the service layer.
- Database constraints from the SQL migration that Hibernate does not enforce at the
  Java level (NOT NULL foreign keys, CHECK constraints, UNIQUE indexes).
- Lazy-loading verification (asserting that associations are not loaded after a fresh
  `findById`).

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingConfig.class})
class ProductRepositoryTest { ... }
```

`JpaAuditingConfig` is imported explicitly because `@DataJpaTest` does not start the
full Spring context. Without it, `created_at`/`updated_at` are never populated and any
test that creates a `Category` triggers a NOT NULL constraint violation.

### Service unit tests (`service/` package)

Pure Mockito unit tests with `@ExtendWith(MockitoExtension.class)`. Repositories are
mocked; no Spring context or database is involved. These test business logic in
isolation (e.g. that the service throws `ResourceNotFoundException` when a repository
returns `Optional.empty()`).

### Security tests (`security/` package)

Dedicated unit/integration tests for `JwtAuthFilter`, `JwtContextInterceptor`, and
`JwtService`. These run with Testcontainers (via `GraphQlIntegrationTestSupport`) since
the filter needs a real database for the blacklist check.

## Running the tests

```bash
./mvnw test
```

Testcontainers requires Docker to be running. If the tests fail with
"Could not find a valid Docker environment", verify that Docker Desktop is running
(`docker info` should succeed) and, if Testcontainers still fails to find the socket,
set `DOCKER_HOST` explicitly:

```bash
# Example for Docker Desktop on Linux
DOCKER_HOST=unix:///home/<user>/.docker/desktop/docker.sock ./mvnw test
```
