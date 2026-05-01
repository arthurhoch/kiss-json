# CAVEMAN.md — Compact Low-Token Summary

For AI agents and maintainers. Read this first.

## What This Is

KissJson: tiny, fast, zero-dependency Java 17+ JSON library.

Serialize Java objects to JSON. Deserialize JSON to Java objects. Fields directly. No getters. No setters.

It does one thing well: convert between JSON and normal Java object graphs.

## What This Is Not

Not Jackson. Not Gson. Not JSON-B. Not a framework. Not dependency injection. Not validation. Not JSONPath. Not JSON Patch. Not schema tooling. Not a general-purpose reflection framework. No external dependencies.

## Main Mental Model

```java
Json json = Json.create();
String text = json.stringify(object);
User user = json.parse(text, User.class);
List<User> users = json.parseList(text, User.class);
Map<String, Object> map = json.parseMap(text);
Map<String, User> byId = json.parseMap(text, User.class);
```

Configure:

```java
Json json = Json.builder()
    .fieldNaming(FieldNaming.SNAKE_CASE)
    .includeNulls(false)
    .failOnUnknownProperties(false)
    .failOnMissingRequiredFields(false)
    .failOnNullForPrimitives(false)
    .failOnDuplicateKeys(false)
    .failOnCycles(true)
    .maxDepth(128)
    .prettyPrint(false)
    .dateFormat(DateFormat.ISO)
    .zoneId(ZoneId.of("UTC"))
    .enumMode(EnumMode.NAME)
    .build();
```

## v1 Must Support

- String, char/Character, primitives, wrappers, BigDecimal, BigInteger
- Enum, arrays, List, Map, nested objects, null
- Date/time: LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime, Instant, Duration, Period, Date, Calendar
- @JsonName, @JsonAliases, @JsonIgnore, @JsonRequired, @JsonIncludeNull, @JsonExcludeNull, @JsonDateFormat
- Naming strategies: IDENTITY, LOWER_CASE, UPPER_CASE, CAMEL_CASE, SNAKE_CASE, KEBAB_CASE
- DateFormat: ISO, EPOCH_MILLIS, EPOCH_SECONDS
- EnumMode: NAME, TO_STRING
- Null handling, unknown properties, duplicate keys, max depth, cycle detection, pretty print
- Rich errors, unit tests, Maven Central, CI/CD

## v1 Must Not Support

- No external dependencies
- No Jackson/Gson/JSON-B/JSON-P
- No getters/setters/JavaBean
- No framework integration
- No custom serializers/deserializers
- No polymorphic type metadata, `$id`/`$ref`
- No enum ordinal
- No JSON Schema, JSONPath, JSON Patch, binary JSON
- No public internal classes
- No Lombok, annotation processing, code generation
- No Java >17, no preview features

## KISS Rules

KISS means Keep It Simple, Stupid. In the Unix tradition, keep the tool small, understandable, maintainable, composable, and focused. Complexity must be justified, not assumed.

1. Do one thing well: JSON <-> normal Java objects.
2. Zero production dependencies.
3. Java 17 only.
4. Fields only. No getters. No setters.
5. No framework integration: no HTTP, DI, persistence, validation, routing, logging, telemetry, schema, JSONPath, or JSON Patch.
6. No rare framework features: no custom serializers/deserializers, polymorphic metadata, `$id`/`$ref`, mixins, views, modules, service loaders, classpath scanning, annotation processing, or code generation.
7. No internal class exposure.
8. No regex parser. No `String.split`.
9. Rich errors with context.
10. Safe defaults: unknown properties ignored, missing fields OK, null primitives OK, duplicate keys last wins, cycles fail, ISO dates, UTC zone, enum `name()`.
11. Developer time over machine time: optimize internals, not the public API.
12. When in doubt, simpler solution wins.

## Public API

Package: `io.github.arthurhoch.kissjson`

Classes: Json, JsonBuilder, JsonConfig, JsonException, JsonParseException, JsonMappingException, FieldNaming, DateFormat, EnumMode, JsonName, JsonAliases, JsonIgnore, JsonRequired, JsonIncludeNull, JsonExcludeNull, JsonDateFormat.

That is the entire public API. Do not add to it without updating docs, tests, and changelog.

## Error Rule

Parse errors: line, column, offset.
Mapping errors: JSON path ($.), target type, field name, expected type, actual value.
Always include the real cause. Always be human-readable.

## Performance Rule

Cache class metadata. Precompute field names and aliases. Direct writer. Token-based parser. No repeated annotation scans. No regex. No `String.split`. IdentityHashMap for cycle detection.

These are internal implementation details. Users should not need to understand parser internals, token streams, class models, field models, caches, codecs, or performance knobs to use KissJson.

## Security Scanning Rule

Normal development stays fast:

```bash
mvn -B verify
```

Security and quality scans are separate:

```bash
mvn -Psecurity verify
mvn -Pquality verify
```

Do not add production dependencies for scanning. Do not suppress security findings without a documented reason. If CodeQL, Dependabot, Semgrep, OWASP Dependency-Check, SpotBugs, or Snyk guidance changes, update `docs/SECURITY_SCANNING.md`. Never commit secrets.

## Internal API Exposure Rule

Internal package: `io.github.arthurhoch.kissjson.internal`
Internal classes are never public API. Users must never import them.

## Before Coding Reading Order

1. `CAVEMAN.md` (this file)
2. `AGENTS.md`
3. `docs/PRODUCT_SPEC.md`
4. `.github/architecture/index.md`
5. `docs/IMPLEMENTATION_PLAN.md`
