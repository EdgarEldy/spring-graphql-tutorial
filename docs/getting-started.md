# Getting Started

## Prerequisites

| Tool | Required version |
|---|---|
| Java | 17 (LTS) |
| Maven | Use the wrapper `./mvnw` - no separate install needed |
| PostgreSQL | 16 |
| Docker + Docker Compose | For the containerised run |

The Maven wrapper (`mvnw` / `mvnw.cmd`) is included at the root of the repository.
All commands below use it; replace with `mvn` if you have Maven 3.9+ installed.

## Running with Docker Compose

The `docker-compose.yml` builds the application image and starts it on port 8080.
It expects a PostgreSQL 16 instance to already be accessible on the host (the
`docker-compose.yml` file does not start a database container itself).

```bash
# 1. Set env vars (or create a .env file at the project root)
export DB_URL=jdbc:postgresql://localhost:5432/spring_graphql_tutorial
export DB_USERNAME=your_user
export DB_PASSWORD=your_password

# 2. Build and start
docker compose up --build
```

The application starts with `SPRING_PROFILES_ACTIVE=prod`. Health check:
`http://localhost:8080/actuator/health`

## Running locally with Maven

The dev profile activates GraphiQL, debug logging, and seed data. It reads the
database coordinates from environment variables (no defaults are provided in
`application-dev.yml`).

```bash
export DB_URL=jdbc:postgresql://localhost:5432/spring_graphql_tutorial
export DB_USERNAME=your_user
export DB_PASSWORD=your_password

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows (PowerShell):

```powershell
$Env:DB_URL     = "jdbc:postgresql://localhost:5432/spring_graphql_tutorial"
$Env:DB_USERNAME = "your_user"
$Env:DB_PASSWORD = "your_password"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Seed data (dev profile only)

`DataSeeder` runs at startup when the dev profile is active. It creates two roles
(`ADMIN`, `USER`) and an admin account if the database is empty:

| Field | Value |
|---|---|
| Email | `admin@example.com` |
| Password | `Admin1@seed` |
| Role | `ADMIN` |

The seeder is guarded by `roleRepository.count() > 0`: running the application a
second time against a database that already has roles leaves the data untouched.

## First request

With the application running in dev mode, open the interactive GraphiQL explorer:

```
http://localhost:8080/graphiql
```

Or send the login mutation directly:

```bash
curl -s -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation($input: LoginInput!) { login(input: $input) { accessToken expiresIn } }",
    "variables": { "input": { "email": "admin@example.com", "password": "Admin1@seed" } }
  }'
```

Copy the `accessToken` from the response and pass it as `Authorization: Bearer <token>`
on subsequent requests. See [api-reference.md](api-reference.md) for the full list of
operations.

## Running tests

```bash
./mvnw test
```

Tests require Docker to be running (Testcontainers starts a PostgreSQL 16 container
automatically). See [testing.md](testing.md) for a detailed explanation of the test
strategy.
