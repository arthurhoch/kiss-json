# AGENTS.md — Primary AI Agent Instructions

This is the primary instruction file for AI coding agents working on KissJson.

## Mandatory Reading Order

Before making any changes to this project, read these files in order:

1. `CAVEMAN.md` — Compact summary
2. `AGENTS.md` — This file
3. `docs/PRODUCT_SPEC.md` — Authoritative spec
4. `.github/architecture/index.md` — Architecture reading order
5. `docs/IMPLEMENTATION_PLAN.md` — Phased plan

## Project Purpose

KissJson is a tiny, high-performance, zero-dependency Java 17+ JSON library. It serializes Java objects to JSON and deserializes JSON to normal Java objects using fields directly.

## Philosophy

- **KISS**: Keep It Simple, Stupid. Small, understandable, maintainable, composable, and focused.
- **Zero dependencies**: No external libraries required.
- **Native JDK**: Built only with Java 17+ standard APIs.
- **Memorable API**: Serialize and deserialize from memory.
- **Field-based mapping**: Use object fields directly, never getters or setters.
- **Simple object model**: Normal Java objects, not framework magic.
- **Fast by design**: Metadata caching, direct writing, token-based parsing.
- **Rich errors**: Path, line, column, target type, field name, real cause.
- **No internal API exposure**: Only public API is user-facing.
- **Safe defaults**: Forgiving, practical, hard to misuse.

## Unix-Style KISS Boundary

KissJson exists to do one thing well: serialize JSON and deserialize JSON for normal Java objects using fields directly.

It must remain a small JSON library, not a framework. Do not add HTTP handling, persistence, validation, routing, dependency injection, service discovery, logging, telemetry, schema validation, JSONPath, JSON Patch, or framework integration.

KissJson should compose with other libraries without depending on them:

```java
String body = json.stringify(request);
HttpResult result = http.request(HttpMethod.POST, url, headers, body).execute();
Response response = json.parse(result.body(), Response.class);
```

Configuration exists only for common JSON behavior. It is not a place to collect every edge case. Every new public type, method, enum, annotation, or option must justify why the existing API cannot handle the common case clearly.

## Non-Negotiable KISS Rules

1. Do one thing well: JSON <-> normal Java object graphs.
2. Zero production dependencies. No exceptions.
3. Java 17 only. No higher. No preview features.
4. Field-based mapping. No getters. No setters. No JavaBean conventions.
5. No framework integration.
6. No internal class exposure.
7. No regex parser. No `String.split` parser.
8. Rich errors with context.
9. Default behavior must be forgiving and practical.
10. Internal optimizations must not leak into public API.

## v1 Scope

See `docs/PRODUCT_SPEC.md` for the complete v1 scope. Summary:

- Serialize/deserialize Java objects to/from JSON
- Primitives, wrappers, String, BigDecimal, BigInteger, char/Character
- Enums, arrays, List, Map, nested objects, null
- Date/time types: LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime, Instant, Duration, Period, Date, Calendar
- Annotations: @JsonName, @JsonAliases, @JsonIgnore, @JsonRequired, @JsonIncludeNull, @JsonExcludeNull, @JsonDateFormat
- Naming strategies, null handling, unknown properties, duplicate keys
- Max depth, cycle detection, pretty print
- Rich errors, unit tests, Maven Central, CI/CD

## v1 Non-Goals

- No Jackson/Gson/JSON-B/JSON-P dependency
- No custom serializers/deserializers
- No polymorphic type metadata, `$id`/`$ref`
- No framework integration
- No HTTP, persistence, routing, dependency injection, logging, telemetry, or service discovery
- No validation framework, JSON Schema, JSONPath, JSON Patch, binary JSON
- No getter/setter/JavaBean mapping
- No public internal classes
- No mixins, views, modules, service loaders, classpath scanning
- No production dependencies, code generation, Lombok, annotation processing
- No enum ordinal support

## Public API Rules

- Public API package: `io.github.arthurhoch.kissjson`
- Only these classes are public API:
  - `Json`, `JsonBuilder`, `JsonConfig`
  - `JsonException`, `JsonParseException`, `JsonMappingException`
  - `FieldNaming`, `DateFormat`, `EnumMode`
  - `JsonName`, `JsonAliases`, `JsonIgnore`, `JsonRequired`, `JsonIncludeNull`, `JsonExcludeNull`, `JsonDateFormat`
- Every public method must have Javadoc.
- Every public method must have at least one test.
- Public API changes must update docs, tests, and changelog.
- Do not add public methods without updating all three.

## Internal API Rules

- Internal package: `io.github.arthurhoch.kissjson.internal`
- Internal classes must not be public API.
- Prefer package-private access.
- Internal classes: parser, writer, object reader/writer, class model, field model, cache, naming strategy, date codec, type converter, JSON value model.
- Users must never need to import internal classes.

## Coding Rules

- Use Java 17. No higher. No preview features.
- No production dependencies.
- No Lombok.
- No annotation processing.
- No code generation.
- No getters/setters for field mapping.
- Use reflection and `setAccessible(true)` for private fields.
- No regex in the parser.
- No `String.split` in the parser.
- Prefer simple, readable code over clever code.
- When in doubt, choose the simpler solution.
- Every exception must be informative with context.

## Testing Rules

- Use JUnit 5 only.
- Tests must be deterministic.
- Tests must not require internet access.
- Every public method must have at least one test.
- See `docs/PRODUCT_SPEC.md` and `.github/architecture/10-testing-strategy.md` for the full test matrix.
- Do not skip tests.
- Do not mark tests as disabled without documenting why.

## Documentation Rules

- All docs in English.
- Docs must match the implemented public API.
- If public API changes, update all affected docs and examples.
- Use "Intended v1 contract" or "Planned for v1 implementation" for unimplemented features.
- Do not claim features work if they are not implemented.
- See `.github/architecture/11-documentation-policy.md` for full policy.

## Maven Central Publishing Rules

- Use Sonatype Central Publisher Portal.
- Tag-based release with `v*` prefix.
- GPG signing required for release.
- Normal build (`mvn -B verify`) must not require secrets.
- See `docs/MAVEN_CENTRAL.md` and `docs/RELEASE.md` for full process.
- Do not publish until implementation and tests are complete.

## GitHub Pages Rules

- Documentation site served from `docs/` directory.
- Jekyll-based via GitHub Pages.
- Triggered on push to `main`.
- See `.github/architecture/13-github-pages.md` for details.

## AI Behavior Rules

- Read all required docs before making changes.
- Do not invent features outside v1 scope.
- Do not add dependencies unless explicitly approved.
- Do not implement Jackson/Gson compatibility.
- Do not turn KissJson into a framework.
- Do not add another way to do something already supported unless the existing way is genuinely insufficient for a common case.
- Do not expose internal classes.
- Do not create a framework.
- Do not use getters/setters for mapping.
- Do not skip tests.
- Do not skip docs.
- Do not silently change public API behavior.
- Always choose the simpler solution when in doubt.
- Always preserve Java 17 compatibility.
- Always keep public API small.
- Always update docs, tests, and changelog when changing public API.

## Change Protocol

1. Read required docs before starting.
2. Make the smallest correct change.
3. Add or update tests.
4. Run `mvn -B verify`.
5. Update documentation if public API changed.
6. Update `CHANGELOG.md` under `Unreleased`.
7. Verify no production dependencies were added.
8. Verify no internal classes were exposed as public API.

## Security Rules

- Do not commit secrets, API keys, passwords, or credentials.
- Do not add production dependencies without explicit approval.
- See `SECURITY.md` for full policy.

## Security Scanning Rules

- Normal development and CI must remain fast: `mvn -B verify`.
- Security-heavy dependency scanning runs separately: `mvn -Psecurity verify`.
- Optional static quality scanning runs separately: `mvn -Pquality verify`.
- Do not make normal CI download vulnerability databases.
- Do not add production dependencies for scanning.
- Do not suppress security findings without a documented reason.
- Keep CodeQL, Dependabot, Semgrep, OWASP Dependency-Check, and SpotBugs configuration consistent with `docs/SECURITY_SCANNING.md`.
- Do not add mandatory Snyk CI unless repository secrets and account ownership are explicitly configured.
- Update `docs/SECURITY_SCANNING.md` whenever security tooling changes.

## Implementation Must Not

- Add production dependencies.
- Expose internal classes as public API.
- Use Java versions above 17.
- Use preview features.
- Use getters/setters for field mapping.
- Implement Jackson/Gson/JSON-B compatibility.
- Support enum ordinal.
- Implement `$id`/`$ref`.
- Add framework integration.
- Add Lombok, annotation processing, or code generation.
- Skip tests for any public method.
- Claim unimplemented features as working.
