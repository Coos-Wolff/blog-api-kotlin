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

There is currently no bundled local-dev profile that starts a throwaway Postgres
container for manual `bootRun` — the local-dev runner is planned but not yet wired up.

## Current state

This project is early-stage. What exists today:

- `User` and `BlogPost` domain entities (Spring Data JDBC)
- Flyway migration creating the `users` and `blog_post` tables
- Repository layer (`UserRepository`, `BlogPostRepository`)

**Not yet implemented:** service layer, controllers, and the security/JWT configuration.
There are no HTTP endpoints available yet.

## Planned API surface

Once built out, the API will expose:

- **Auth:** register, login, refresh (JWT-based)
- **BlogPost CRUD:** public reads; authenticated writes; update/delete restricted to the
  post's author or an admin

This section describes the target behavior, not what is currently
callable — see "Current state" above.