# Deployment

## Docker image

The `Dockerfile` uses a two-stage build:

1. **Build stage** - Maven wrapper compiles the application and produces the fat JAR.
2. **Runtime stage** - copies only the JAR into a slim JRE 17 image.

Build the image manually:

```bash
docker build -t spring-graphql-tutorial:latest .
```

## `docker-compose.yml`

The Compose file defines only the application service. It does **not** start a PostgreSQL
container: the project assumes a shared `postgres_main` container already running on the
host (used across several tutorial projects on the same machine). The application
container reaches that host-level container via `host.docker.internal`.

```bash
# Start with defaults (connects to host Postgres at port 5432)
docker compose up --build

# Override the database connection
DB_URL=jdbc:postgresql://host.docker.internal:5432/my_db \
DB_USERNAME=my_user \
DB_PASSWORD=my_pass \
docker compose up --build
```

The service starts with `SPRING_PROFILES_ACTIVE=prod`.

## Required environment variables

| Variable | Purpose | Prod default in Compose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Activates the prod configuration | `prod` (hardcoded in Compose) |
| `DB_URL` | JDBC connection URL | `jdbc:postgresql://host.docker.internal:5432/spring_graphql_db` |
| `DB_USERNAME` | Database username | `spring_graphql_db` |
| `DB_PASSWORD` | Database password | `spring_graphql_db` |
| `JWT_SECRET` | HMAC-SHA key for JWT signing | *(none, must be set in prod)* |

In a real deployment, set `JWT_SECRET` to a long random string (at least 256 bits).
The base `application.yml` has a fallback dev value (`${JWT_SECRET:dev-only-...}`), but
`application-prod.yml` requires the variable without a default to force it to be present.

## CI/CD

GitHub Actions runs on every push and pull request. The workflow:
1. Sets up JDK 17.
2. Runs `./mvnw --no-transfer-progress verify` - this compiles, runs all tests
   (Testcontainers starts a PostgreSQL 16 container), and packages the JAR.

See `.github/workflows/ci.yml` for the full pipeline definition.

## Health check

With the prod profile, only the `health` endpoint is exposed:

```
GET http://localhost:8080/actuator/health
```

Expected response when the application and database are reachable:

```json
{ "status": "UP" }
```

## Ports

| Port | Protocol | Description |
|---|---|---|
| `8080` | HTTP | GraphQL endpoint (`/graphql`), Actuator (`/actuator/*`) |
| `8080` | WebSocket | Subscription endpoint (`ws://host:8080/graphql`) |

There is no separate WebSocket port: the GraphQL WebSocket upgrade happens on the same
port as the HTTP endpoint.
