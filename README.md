# KissJson

A tiny, high-performance, zero-dependency Java 17+ JSON library with field-based object mapping.

## Status

**Initial v1 implementation is present and under validation.** This repository contains the JSON parser, writer, field-based mapper, public API, tests, project specification, architecture, and governance files.

Do not publish a functional release until implementation and tests are complete.

## Maven

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-json</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Examples

```java
Json json = Json.create();

// Serialize
String text = json.stringify(user);

// Deserialize
User user = json.parse(text, User.class);

// List
List<User> users = json.parseList(text, User.class);

// Map
Map<String, Object> map = json.parseMap(text);
Map<String, User> usersById = json.parseMap(text, User.class);
```

## Configured Instance

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

## Philosophy

- **KISS**: Keep It Simple, Stupid. Small, understandable, maintainable, composable, and focused.
- **Zero dependencies**: No external libraries required.
- **Native JDK**: Built only with Java 17+ standard APIs.
- **Memorable API**: Serialize and deserialize from memory.
- **Field-based mapping**: Use object fields directly, never getters or setters.
- **Simple object model**: Designed for normal Java objects, not framework magic.
- **Fast by design**: Metadata caching, direct writing, token-based parsing, no unnecessary intermediate objects.
- **Rich errors**: Exceptions contain path, line, column, target type, field name, and the real failure cause.
- **No internal API exposure**: Users interact only with the public API.
- **Safe defaults**: Forgiving, practical, hard to misuse.

## KISS Boundary

KissJson does one thing well: convert between JSON and normal Java object graphs using fields directly.

It is a JSON library, not a framework. It does not handle HTTP, persistence, validation, routing, dependency injection, service discovery, logging, telemetry, schema validation, JSONPath, JSON Patch, or application runtime integration.

It should compose with those tools without depending on them:

```java
String body = json.stringify(request);
HttpResult result = http.request(HttpMethod.POST, url, headers, body).execute();
Response response = json.parse(result.body(), Response.class);
```

The public API must stay small enough to memorize. Every new public class, method, enum, annotation, or configuration option must justify its existence against the core mental model:

```java
Json json = Json.create();
String text = json.stringify(user);
User user = json.parse(text, User.class);
```

## Default Behavior

| Option | Default | Rationale |
|--------|---------|-----------|
| `fieldNaming` | `IDENTITY` | Use Java field names as-is |
| `includeNulls` | `true` | Include null fields in output |
| `failOnUnknownProperties` | `false` | Ignore unknown JSON fields |
| `failOnMissingRequiredFields` | `false` | Do not fail for missing fields |
| `failOnNullForPrimitives` | `false` | Keep Java defaults for null primitives |
| `failOnDuplicateKeys` | `false` | Last value wins for duplicate keys |
| `failOnCycles` | `true` | Prevent infinite recursion |
| `maxDepth` | `128` | Reasonable depth limit |
| `prettyPrint` | `false` | Compact output by default |
| `dateFormat` | `ISO` | ISO-8601 format |
| `zoneId` | `UTC` | UTC timezone |
| `enumMode` | `NAME` | Use `enum.name()` |

## v1 Scope

- Serialize/deserialize Java objects to/from JSON
- Field-based mapping (no getters/setters)
- Primitives, wrappers, String, BigDecimal, BigInteger, char/Character
- Enums, arrays, List, Map, nested objects, null
- Date/time: LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime, Instant, Duration, Period, Date, Calendar
- Annotations: @JsonName, @JsonAliases, @JsonIgnore, @JsonRequired, @JsonIncludeNull, @JsonExcludeNull, @JsonDateFormat
- Naming strategies, null handling, unknown property handling, duplicate key handling
- Max depth, cycle detection, pretty print
- Rich parse and mapping errors
- Maven Central publishing, GitHub Actions CI, GitHub Pages docs

## Non-Goals

- No Jackson/Gson/JSON-B/JSON-P dependency
- No custom serializers/deserializers in v1
- No polymorphic type metadata, `$id`/`$ref`, or graph reconstruction
- No framework integration (Spring, Quarkus, Micronaut, Jakarta, CDI)
- No validation framework, JSON Schema, JSONPath, JSON Patch, binary JSON
- No HTTP, persistence, routing, dependency injection, logging, telemetry, or service discovery
- No mixins, views, modules, service loaders, or classpath scanning
- No getter/setter/JavaBean mapping
- No public internal parser/writer/mapper/cache/model classes
- No production dependencies, code generation, Lombok, annotation processing

## Benchmarks

JMH benchmarks, including a benchmark-only comparison with Jackson, run from the isolated `benchmark` Maven profile:

```bash
mvn -Pbenchmark clean test-compile
mvn -Pbenchmark exec:exec
```

Jackson is used only in that benchmark profile and is not a production dependency. Benchmark results depend on JVM, hardware, warmup, and payload shape; see [Performance](docs/PERFORMANCE.md) for scenarios and process.

## Security and quality checks

Normal development and CI use the fast build:

```bash
mvn -B verify
```

Security-heavy dependency scanning runs separately:

```bash
mvn -Psecurity verify
```

Optional static quality checks run separately:

```bash
mvn -Pquality verify
```

GitHub Actions run CodeQL and Semgrep as separate workflows. Dependabot tracks Maven and GitHub Actions updates weekly. OWASP Dependency-Check is available through the `security` Maven profile. Snyk is optional/manual unless maintainers configure it later. See [Security Scanning](docs/SECURITY_SCANNING.md).

## Documentation

- [Product Specification](docs/PRODUCT_SPEC.md)
- [Getting Started](docs/GETTING_STARTED.md)
- [API Reference](docs/API.md)
- [Configuration](docs/CONFIGURATION.md)
- [Examples](docs/EXAMPLES.md)
- [Error Handling](docs/ERROR_HANDLING.md)
- [Performance](docs/PERFORMANCE.md)
- [Security Scanning](docs/SECURITY_SCANNING.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Release Guide](docs/RELEASE.md)
- [Maven Central Guide](docs/MAVEN_CENTRAL.md)
- [Architecture](.github/architecture/index.md)

## License

Apache License 2.0. Copyright 2026 Arthur Hoch. See [LICENSE.txt](LICENSE.txt).
