# CLAUDE.md

## Working style

- The user writes application code. Claude reviews, explains, and flags issues — especially duplicated code, where Claude should propose the idiomatic refactor (shared helpers, dependency factories, parameterization) rather than just pointing out the duplication.
- Claude may scaffold infrastructure/config more directly (build files, CI, docker, migrations skeletons, etc.).

## Build/test

`./gradlew build` (requires Docker running — tests use Testcontainers).

## Local configuration

Local runs read a gitignored `.env` file via `DotenvEnvironmentPostProcessor` (backed by
`io.github.cdimascio:dotenv-java`), which populates the `${...}` placeholders in
`application.properties`. `.env.example` documents the required keys — copy it to `.env` and fill
in real local values. Production supplies these same keys as real environment variables.

`application-local.properties` was intentionally removed in favor of this mechanism — do not
reintroduce a separate local profile.

## Persistence

Spring Data JDBC, **not** JPA.

- Always use Spring Data annotations: `org.springframework.data.annotation.Id`,
  `org.springframework.data.annotation.Version`,
  `org.springframework.data.relational.core.mapping.Table`,
  `org.springframework.data.relational.core.mapping.Column`,
  `org.springframework.data.jdbc.core.mapping.AggregateReference`.
  Never `jakarta.persistence.*`.
- Entities are immutable data classes with `val` properties.
- Cross-aggregate references use `AggregateReference`, never a JPA-style relation/mapping.

### ID/version (Option B)

```kotlin
@Id val id: UUID = UUID.randomUUID()
@Version val version: Long? = null
```

Null/zero `version` drives Spring Data JDBC's INSERT-vs-UPDATE detection and provides
optimistic locking. Don't deviate from this pattern.

### Naming

camelCase → snake_case conversion is automatic. Only add `@Table`/`@Column` when the
database name differs from the derived name (e.g. `@Table("users")`).

## Validation

The compiler flag `-Xannotation-default-target=param-property` is set, so validation
annotations need **no** `@field:` prefix — write `@NotBlank val title: String` directly.

## Testing

- `src/test` — pure unit tests only (MockK, no Spring context, no Docker): the `*Test` files
  (`TokenServiceTest`, `AuthServiceTest`, `BlogPostServiceTest`). Runs via `./gradlew test`.
- `src/integrationTest` — a dedicated Gradle source set (JVM Test Suite plugin) for everything
  container-backed: controller `*IT` tests (`AuthControllerIT`, `BlogPostControllerIT`), the
  repository slice tests (`UserRepositoryIT`, `BlogPostRepositoryIT` — `@DataJdbcTest` +
  `@Import(TestcontainersConfiguration::class)`), the full-context `BlogApiApplicationTests`, and
  the shared infra (`IntegrationTestBase`, `IntegrationTestConstants`, `TestcontainersConfiguration`,
  `TestBlogApiApplication`). Runs via `./gradlew integrationTest` (needs Docker); `check`/`build`
  run both suites. Controller ITs use `IntegrationTestBase`: `@SpringBootTest(webEnvironment =
  RANDOM_PORT)` + `@AutoConfigureRestTestClient`, driving the app over HTTP with `RestTestClient`.
- Because `RANDOM_PORT` integration tests run outside the test transaction, there's no
  transactional rollback between tests — clean up mutated state explicitly. `IntegrationTestBase`
  autowires `userRepository`/`blogPostRepository` and does this once in a shared `@BeforeEach`,
  FK-safe order (`blog_post` rows deleted before `users` rows, since `blog_post.author` references
  `users`). Subclasses inherit this and must not re-declare their own repository fields or
  cleanup — that reintroduces the duplication and risks an FK violation if a subclass's cleanup
  forgets the ordering.
- Admin-override integration tests (a promoted user acting on another user's resource) must call
  `userRepository.save(user.copy(isAdmin = true))` **before** logging in, not after — roles are
  snapshotted into the JWT at login time, so a token minted before the promotion would only carry
  `ROLE_USER`.
- Write tests against the spec/intended behavior, never mirroring the implementation.
- Use MockK, not Mockito.

## Conventions

- `token_type` (access vs refresh) is enforced in the auth service flow, not on the JWT
  decoder — `SecurityConfig`'s `JwtDecoder` validates signature/expiry only.
- Response/request JSON uses snake_case (`spring.jackson.property-naming-strategy=SNAKE_CASE`);
  Kotlin properties stay camelCase.
- List endpoints that return entities with an `AggregateReference` to another aggregate (e.g.
  `BlogPost.author`) must batch-fetch: collect the distinct referenced ids and issue one
  `findAllById`, then map in memory. Spring Data JDBC does not lazy-load `AggregateReference`,
  so fetching one-by-one per row is an N+1 query bug, not just an inefficiency.

## Git/workflow

- Feature branch → PR → merge to protected `main`.
- `git add <paths>` explicitly — never `git add -A` / `git add .`.
- No AI attribution in commits: no `Co-Authored-By`, no "Generated with Claude Code",
  no similar trailers.
- Commits should be deterministic and logically grouped.