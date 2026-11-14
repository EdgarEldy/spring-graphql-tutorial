# Configuration

## Profiles

| Profile | Used for | Activated by |
|---|---|---|
| *(default)* | Shared base configuration | always |
| `dev` | Local development | `-Dspring-boot.run.profiles=dev` or env var |
| `prod` | Docker / production | `SPRING_PROFILES_ACTIVE=prod` |
| `test` | Testcontainers integration tests | `@ActiveProfiles("test")` on test classes |

## Base `application.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  graphql:
    websocket:
      path: /graphql        # WS endpoint, same path as HTTP

jwt:
  secret: ${JWT_SECRET:dev-only-...}  # REQUIRED in prod; has a dev fallback
  expiration-ms: 3600000              # 1 hour
  refresh-expiration-ms: 604800000    # 7 days

spring.mail:
  host: ${MAIL_HOST:localhost}
  port: ${MAIL_PORT:1025}
```

`ddl-auto: validate` means Hibernate only validates the schema; Flyway manages all DDL.

## `application-dev.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/spring_graphql_tutorial}
    username: ${DB_USERNAME:spring_graphql_tutorial}
    password: ${DB_PASSWORD:spring_graphql_tutorial}
  graphql:
    graphiql:
      enabled: true
      path: /graphiql           # interactive explorer
  mail:
    host: ${MAIL_HOST:localhost}
    port: ${MAIL_PORT:1025}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    com.edgareldy.springgraphqltutorial: DEBUG
```

The dev profile ships with sensible defaults for the database connection and the local
mail server so the application can be started with no environment variables at all,
provided the database `spring_graphql_tutorial` exists on `localhost:5432`.
See [getting-started.md](getting-started.md) for instructions on creating that database
in an existing PostgreSQL container or a Docker Compose service.

## `application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    show-sql: false

management:
  endpoints:
    web:
      exposure:
        include: health    # only health in prod

logging:
  file:
    name: logs/app.log
  level:
    root: INFO
```

## Environment variables

| Variable | Profile | Default | Description |
|---|---|---|---|
| `DB_URL` | dev, prod | none | JDBC connection URL |
| `DB_USERNAME` | dev, prod | none | Database username |
| `DB_PASSWORD` | dev, prod | none | Database password |
| `JWT_SECRET` | all | `dev-only-...` (base) | HMAC-SHA secret for signing JWTs. Must be overridden in prod. |
| `MAIL_HOST` | all | `localhost` | SMTP host |
| `MAIL_PORT` | all | `1025` | SMTP port |
| `SPRING_PROFILES_ACTIVE` | - | - | Set to `prod` for the Docker Compose run |

## JWT properties binding

JWT configuration uses `@ConfigurationProperties` rather than `@Value`:

```java
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs) {}
```

Registered via `@EnableConfigurationProperties(JwtProperties.class)` on `SecurityConfig`.
Renaming a property in `application.yml` produces a compile-time error rather than a
silent misconfiguration at runtime.

## GraphiQL explorer

GraphiQL is a browser-based IDE for exploring the schema and running queries. It is
enabled only in the dev profile:

```
http://localhost:8080/graphiql
```

It is not available in prod. The introspection endpoint (`/graphql` with an introspection
query) remains available in all profiles, so external tools (Insomnia, Postman, Apollo
Studio) can still import the schema.
