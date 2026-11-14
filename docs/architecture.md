# Architecture

## Overview

Spring for GraphQL exposes a **single HTTP endpoint** (`POST /graphql`) that multiplexes
all queries, mutations, and subscriptions. A WebSocket connection to the same path (`ws://host/graphql`)
handles subscriptions with the `graphql-ws` subprotocol.

The project is built around standard Spring for GraphQL annotations and patterns:

```
Client
  |
  |  HTTP POST /graphql  (queries, mutations)
  |  WS /graphql         (subscriptions)
  v
GraphQL Engine  (Spring for GraphQL)
  |
  +-- Controllers           @QueryMapping / @MutationMapping / @SubscriptionMapping
  |      (graphql/controller/)
  |
  +-- Field Resolvers       @SchemaMapping  +  DataLoader
  |      (graphql/resolver/)
  |
  +-- Exception Handler     GraphQlExceptionResolver  (DataFetcherExceptionResolver)
  |      (graphql/exception/)
  |
  +-- Services              XxxService interface  /  XxxServiceImpl
  |      (service/)
  |
  +-- Repositories          Spring Data JPA  (JpaRepository)
  |      (repository/)
  |
  +-- JPA Entities          Hibernate, PostgreSQL 16
         (entity/)
```

## Conventions carried over from spring-boot-tutorial

The service, repository, and entity layers follow the same design as SBT:

- **Contract/implementation pattern** - `XxxService` interface at `service/`, `XxxServiceImpl`
  at `service/impl/` annotated `@Service`. Controllers and tests inject the interface.
- `@Transactional` on write methods, on the implementation class.
- No business logic in controllers - delegation to the service only.
- Input types are Java `record`s in `graphql/input/` (immutable, used instead of `@RequestBody`).

The GraphQL layer replaces SBT's REST controllers, `ApiResponse<T>` wrapper,
`PageResponse<T>`, MapStruct mappers, and Swagger/OpenAPI annotations.

## Controllers

Controllers annotated with `@Controller` (not `@RestController`) declare operations via:

| Annotation | Used for |
|---|---|
| `@QueryMapping` | Read operations (fields under `Query` in the schema) |
| `@MutationMapping` | Write operations (fields under `Mutation`) |
| `@SubscriptionMapping` | Reactive streams (fields under `Subscription`) |

Method arguments are bound from GraphQL variables directly - for example,
`@Argument CategoryInput input` receives the `input` variable from the client document.

Authorization is declared at the method level using `@PreAuthorize`:

```java
@QueryMapping
public Page<Category> categories(/* ... */) { ... }   // public, no annotation

@MutationMapping
@PreAuthorize("hasRole('ADMIN')")
public Category createCategory(@Argument CategoryInput input) { ... }
```

This per-method placement is deliberate: a single `@RequestMapping("/graphql")` path
cannot distinguish operations, so a class-level `@PreAuthorize` would either block all
reads or allow all writes.

## Field Resolvers and the DataLoader pattern

JPA associations on entities are mapped `FetchType.LAZY`. When a GraphQL query requests
a related field (e.g. `Product.category`), Spring for GraphQL calls a **field resolver**
annotated with `@SchemaMapping(typeName, field)`.

Field resolvers do **not** call the repository directly. Instead they delegate to a
`DataLoader`, which Spring for GraphQL injects as a method argument. The DataLoader
collects all the IDs for a given parent field across the entire response in a single batch
call, replacing N+1 selects with one SELECT IN.

Five DataLoaders are registered in `DataLoaderConfig`:

| DataLoader name / type pair | Resolves |
|---|---|
| `"userRoles"` | `User.roles` - one user ID -> list of roles |
| `"rolePermissions"` | `Role.permissions` - one role ID -> list of permissions |
| `Long -> Category` | `Product.category` - one product ID -> one category |
| `Long -> Customer` | `Order.customer` - one order ID -> one customer |
| `Long -> Product` | `Order.product` - one order ID -> one product |

The one-to-many loaders (`userRoles`, `rolePermissions`) use
`BatchLoaderRegistry.forName(name)` because the value type is `List<Role>`, which
prevents using the `forTypePair(K, V)` shorthand that Spring for GraphQL uses to inject
the loader by type. The corresponding resolvers (`UserRolesResolver`,
`RolePermissionsResolver`) retrieve the loader from the `DataFetchingEnvironment`
registry by name.

All batch functions run on `Schedulers.boundedElastic()` via `Mono.fromCallable` to
avoid blocking the Netty event loop.

## Real-time Subscriptions

`OrderController.orderCreated` returns a `Flux<Order>` backed by a
`Sinks.Many<Order>` shared bean:

```
OrderServiceImpl.create()       ->  orderSink.tryEmitNext(savedOrder)
OrderController.orderCreated()  ->  orderSink.asFlux()
```

The sink is configured with `Sinks.many().multicast().directBestEffort()`.
`directBestEffort` drops events silently when there are no active subscribers
(e.g. during startup before anyone connects), which avoids replaying historical orders
to new subscribers - a correctness concern that `onBackpressureBuffer` would introduce.

`OrderEventsConfig` owns the `Sinks.Many<Order>` bean, decoupling the service (which
emits) from the controller (which exposes it as a Flux).

## Exception Handling

`GraphQlExceptionResolver` implements Spring for GraphQL's `DataFetcherExceptionResolver`
interface and maps domain exceptions to GraphQL error classifications:

| Exception | `extensions.classification` |
|---|---|
| `ResourceNotFoundException` | `NOT_FOUND` |
| `BusinessRuleException` | `BAD_REQUEST` |
| `AccessDeniedException` | `FORBIDDEN` |
| `OptimisticLockingFailureException` | `BAD_REQUEST` (transport) + `"CONFLICT"` in extensions |
| Any other `Exception` | `INTERNAL_ERROR` (original message is never exposed) |

Errors are returned in the standard GraphQL `errors` array; there is no
`ApiResponse<T>` wrapper as in SBT.

## What this project does NOT use

Compared to SBT, the following Spring Boot features are absent by design:

- AOP (`@Around`, `@Before`, `LoggingAspect`, `ExecutionTimeAspect`) - not applicable
  in a reactive GraphQL pipeline.
- Caffeine cache (`@Cacheable`) - DataLoaders handle batching at the request level;
  application-level caching is left as a future concern.
- MapStruct - entities are mapped to output types directly in the service layer.
- Custom Actuator endpoint (`@Endpoint`) - not added to this project.
- Swagger / springdoc-openapi - GraphQL is self-documenting via introspection and GraphiQL.
- `ApiResponse<T>` / `PageResponse<T>` wrappers - replaced by the GraphQL envelope and
  pagination via `Page<T>` from Spring Data.
