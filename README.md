# blog-api (Kotlin)

A JSON REST API for a simple blogging platform, built to learn idiomatic,
production-grade Kotlin on Spring Boot.

## Tech stack

- **Language:** Kotlin 2.3.21
- **Framework:** Spring Boot 4.1.0, Spring Web MVC
- **Runtime:** Java 25 toolchain
- **Build:** Gradle (Kotlin DSL)
- **Persistence:** Spring Data JDBC (not JPA), PostgreSQL 17, Flyway migrations
- **Security:** Spring Security OAuth2 resource server (JWT)
- **Validation:** Jakarta Validation
- **Testing:** Testcontainers, RestTestClient, MockK

## Prerequisites

- JDK 25
- Docker running locally — required for `./gradlew integrationTest` and `./gradlew build`
  (Testcontainers-backed tests). `./gradlew test` (fast unit tests) does not need Docker.
  Running the app locally also needs a Postgres, via either option described below.

## Building and running

### Tests

```bash
./gradlew test            # fast unit tests only — no Docker required
./gradlew integrationTest  # container-backed tests (Testcontainers) — requires Docker running
./gradlew build            # runs both suites (check depends on integrationTest)
```

### Running the app locally

**Option A — throwaway container, no setup.** Run `TestBlogApiApplication`
(`src/integrationTest/kotlin/com/wolffsoft/blogapi/TestBlogApiApplication.kt`) instead of the main
class. It boots the app with `TestcontainersConfiguration` applied, which starts a throwaway
`postgres:17-alpine` container, runs the Flyway migrations against it, and serves on port 8080.
Requires Docker running; nothing else to configure.

**Option B — persistent local Postgres via `.env`.** Copy `.env.example` to `.env` (gitignored)
and fill in local values, start a matching Postgres, e.g.:

```bash
docker run -d --name blog-api-postgres \
  -e POSTGRES_USER=blog_api -e POSTGRES_PASSWORD=blog_api -e POSTGRES_DB=blog_api \
  -p 5432:5432 postgres:17-alpine
```

then run the main `BlogApiApplication`. A `DotenvEnvironmentPostProcessor` loads `.env` into the
environment at startup, so the `${...}` placeholders in `application.properties` resolve.
Production supplies these same keys as real environment variables — `.env` is a local-only
convenience and is gitignored.

## Current state

The blog post feature is now complete. What exists today:

- `User` and `BlogPost` domain entities (Spring Data JDBC)
- Flyway migration creating the `users` and `blog_post` tables
- Repository layer (`UserRepository`, `BlogPostRepository`)
- Security/JWT configuration (stateless resource server, JWT encoder/decoder, filter chain)
- Auth service and controller: register, login, refresh — see "API surface" below
- Blog post service and controller: public reads, authenticated writes, author-or-admin
  ownership enforcement — see "API surface" below

## API surface

- **Auth** (`/api/auth`, JWT-based):
  - `POST /register` — create a user account
  - `POST /login` — exchange credentials for an access/refresh token pair
  - `POST /refresh` — exchange a refresh token for a new token pair
  - See `http/auth.http` for runnable request examples (select the `local` environment
    from `http/http-client.env.json` in IntelliJ's HTTP Client before running).
- **Blog posts** (`/api/posts`):
  - `GET /api/posts` — public, paginated list of posts (no authentication required)
  - `GET /api/posts/{id}` — public, single post by id (no authentication required)
  - `POST /api/posts` — authenticated; creates a post authored by the caller (author is
    taken from the JWT, never from the request body)
  - `PATCH /api/posts/{id}` — authenticated; partial update, restricted to the post's
    author or an admin
  - `DELETE /api/posts/{id}` — authenticated; restricted to the post's author or an admin
  - See `http/blogpost.http` for a runnable end-to-end request example.