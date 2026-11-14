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

## Setting up the PostgreSQL database

The application expects a PostgreSQL 16 database named `spring_graphql_db`.
Flyway runs all migrations automatically on first startup, so you only need an
empty database before the first run.

### Option A - Dedicated Docker container (recommended for a fresh machine)

```bash
docker run -d \
  --name spring_graphql_db \
  -e POSTGRES_DB=spring_graphql_db \
  -e POSTGRES_USER=spring_graphql_db \
  -e POSTGRES_PASSWORD=spring_graphql_db \
  -p 5432:5432 \
  postgres:16
```

### Option B - Add a database to an existing PostgreSQL container

If you already have a shared PostgreSQL container running (e.g. `postgres_main`
used across several projects), create a dedicated user and database inside it:

```bash
# Open a psql session in the running container (replace postgres_main with your container name)
docker exec -it postgres_main psql -U postgres
```

Then run these SQL statements in the psql prompt:

```sql
CREATE USER spring_graphql_db WITH PASSWORD 'spring_graphql_db';
CREATE DATABASE spring_graphql_db OWNER spring_graphql_db;
\q
```

Alternatively, as a one-liner without an interactive shell:

```bash
docker exec postgres_main psql -U postgres \
  -c "CREATE USER spring_graphql_db WITH PASSWORD 'spring_graphql_db';" \
  -c "CREATE DATABASE spring_graphql_db OWNER spring_graphql_db;"
```

### Option C - Native PostgreSQL installation

Create the user and database with psql directly:

```bash
psql -U postgres \
  -c "CREATE USER spring_graphql_db WITH PASSWORD 'spring_graphql_db';" \
  -c "CREATE DATABASE spring_graphql_db OWNER spring_graphql_db;"
```

---

## Running locally with Maven

`application-dev.yml` ships with built-in defaults so no environment variables
need to be set when connecting to the database created in the previous step:

| Property | Default value |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/spring_graphql_db` |
| `DB_USERNAME` | `spring_graphql_db` |
| `DB_PASSWORD` | `spring_graphql_db` |

Start the application in dev mode:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

On Windows (PowerShell):

```powershell
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Override any default by setting the corresponding environment variable before
the Maven command:

```bash
export DB_URL=jdbc:postgresql://my-host:5432/my_db
export DB_USERNAME=my_user
export DB_PASSWORD=my_password
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Running with Docker Compose

The `docker-compose.yml` builds the application image and starts it on port 8080.
It does **not** start a database container: it uses `host.docker.internal` to
reach a PostgreSQL instance already running on the host (see the database setup
section above). The image starts with `SPRING_PROFILES_ACTIVE=prod`.

```bash
# Using the same DB created in Option A or B above
docker compose up --build
```

Override the connection if your PostgreSQL runs on a non-default host or port:

```bash
DB_URL=jdbc:postgresql://host.docker.internal:5432/spring_graphql_db \
DB_USERNAME=spring_graphql_db \
DB_PASSWORD=spring_graphql_db \
docker compose up --build
```

Health check: `http://localhost:8080/actuator/health`

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
