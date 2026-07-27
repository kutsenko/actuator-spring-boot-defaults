# actuator-spring-boot-defaults — Claude Code Rules

Spring Boot service establishing sensible Actuator defaults. Coding conventions are
adopted from the `docint-service` project (backend subset).

## Tech Stack

Java 25 / Spring Boot 4.0.7 / Gradle (Kotlin DSL)

## Build & Run

```bash
# Build (compile + test + checkstyle + spotbugs + jacoco coverage gate)
./gradlew build

# Run tests only
./gradlew test

# Run the application
./gradlew :app:bootRun

# Build OCI image
./gradlew :app:bootBuildImage

# Dependency vulnerability scan (needs NVD_API_KEY for a full run)
./gradlew dependencyCheckAnalyze

# Regenerate dependency lockfiles after a dependency change
./gradlew dependencies --write-locks
```

## Modules

| Module | Purpose |
|--------|---------|
| `app` | The Spring Boot application. Full quality stack: Checkstyle, SpotBugs, JaCoCo (80%), OWASP, dependency-locking. |
| `checkstyle-rules` | Plain Java library holding the custom `SingleStatementBracesCheck`. Deliberately excluded from the application machinery — it configures itself. |

## Coding Conventions

### Java 25
- **Records** for all value objects, DTOs, events, query results — no Lombok `@Value`
- **Sealed interfaces** for closed hierarchies; **pattern matching** in `switch`/`instanceof`
- **Virtual threads** for I/O-bound tasks; **ScopedValue** over ThreadLocal
- `Objects.requireNonNull(param, "msg")` at method entry
- Methods max 20-30 lines; classes max ~200 lines; max 3-4 params (use a record for more)
- Errors: RFC 9457 Problem Details; sealed `Result<T>` for domain operations
- **Fail fast, not defensive programming** — no silent fallbacks, no catch-and-ignore, no "just in case" null checks inside the system. Validate at boundaries, trust internal calls. Broken infrastructure must fail loudly.
- Always use `var` for local variables with initializers — no explicit types
- **No curly braces on single-statement `if`/`else`/`for`/`while` blocks** — enforced by the custom `SingleStatementBracesCheck` (dangling-else + documented-block carve-outs). `var` enforcement is convention only (no CheckStyle rule — too fragile).
- Single-param value types: use `Type.create()` for random UUID init, `Type.of(value)` for wrapping existing values
- `--enable-preview` is on — preview language features are permitted

### General
- **English only** — all code, comments, commits, docs, specs
- **TDD** — write tests before implementation (Red → Green → Refactor)
- **80% line coverage** minimum (JaCoCo); the `check` task fails below it
- Line length max 120; 4-space indent; Google Java Style base (see `config/checkstyle/checkstyle.xml`)

## Commit Messages

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```
Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`

## Do NOT

- Use Lombok — use Java records
- Use `ThreadLocal` — use `ScopedValue`
- Write German in code/comments/commits
- Add curly braces to single-statement blocks
- Skip tests — no implementation without tests
- Commit without running tests first
- Silently swallow exceptions or add defensive fallbacks — fail loudly

## Quality Tooling

| Tool | Config | Gate |
|------|--------|------|
| Checkstyle | `config/checkstyle/checkstyle.xml` (+ `suppressions.xml`) | `isIgnoreFailures = false` |
| SpotBugs | `config/spotbugs/exclude-filter.xml` | effort MAX, confidence MEDIUM |
| JaCoCo | root `build.gradle.kts` | 80% line coverage |
| OWASP Dependency-Check | `config/owasp/suppressions.xml` | fail on CVSS ≥ 7.0 |

## References

- **Base package:** `com.github.kutsenko`
- Conventions ported from `docint-service` (`../docint-service/CLAUDE.md`)
