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
- Docker running locally (required for Testcontainers-backed tests)

## Building and running

```bash
./gradlew build
```

For manual local runs, use `TestBlogApiApplication` (`src/test/kotlin/.../TestBlogApiApplication.kt`)
instead of the main class. It boots the app with `TestcontainersConfiguration` applied, which
starts a throwaway `postgres:17-alpine` container, runs the Flyway migrations against it, and
serves on port 8080 — no manual Postgres setup required.

## Current state

This project is early-stage. What exists today:

- `User` and `BlogPost` domain entities (Spring Data JDBC)
- Flyway migration creating the `users` and `blog_post` tables
- Repository layer (`UserRepository`, `BlogPostRepository`)
- Security/JWT configuration (stateless resource server, JWT encoder/decoder, filter chain)

**Not yet implemented:** service layer and controllers (auth and blog post endpoints).
There are no callable HTTP endpoints yet.

## Planned API surface

Once built out, the API will expose:

- **Auth:** register, login, refresh (JWT-based)
- **BlogPost CRUD:** public reads; authenticated writes; update/delete restricted to the
  post's author or an admin

This section describes the target behavior, not what is currently
callable — see "Current state" above.