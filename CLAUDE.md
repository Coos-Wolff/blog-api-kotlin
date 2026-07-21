# CLAUDE.md

## Working style

- The user writes application code. Claude reviews, explains, and flags issues — especially duplicated code, where Claude should propose the idiomatic refactor (shared helpers, dependency factories, parameterization) rather than just pointing out the duplication.
- Claude may scaffold infrastructure/config more directly (build files, CI, docker, migrations skeletons, etc.).

## Build/test

`./gradlew build` (requires Docker running — tests use Testcontainers).

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

- `*Test` — fast slice tests: `@DataJdbcTest` + `@Import(TestcontainersConfiguration::class)`.
- `*IT` — integration tests, in a separate source set (deferred, not yet set up).
- Write tests against the spec/intended behavior, never mirroring the implementation.
- Use MockK, not Mockito.

## Git/workflow

- Feature branch → PR → merge to protected `main`.
- `git add <paths>` explicitly — never `git add -A` / `git add .`.
- No AI attribution in commits: no `Co-Authored-By`, no "Generated with Claude Code",
  no similar trailers.
- Commits should be deterministic and logically grouped.