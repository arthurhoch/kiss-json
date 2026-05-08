# KissJson

Tiny zero-dependency Java 17+ JSON library for field-based serialization and deserialization.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.arthurhoch/kiss-json.svg)](https://central.sonatype.com/artifact/io.github.arthurhoch/kiss-json)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE.txt)
[![CI](https://github.com/arthurhoch/kiss-json/actions/workflows/ci.yml/badge.svg)](https://github.com/arthurhoch/kiss-json/actions/workflows/ci.yml)
[![CodeQL](https://github.com/arthurhoch/kiss-json/actions/workflows/codeql.yml/badge.svg)](https://github.com/arthurhoch/kiss-json/actions/workflows/codeql.yml)
[![Docs](https://github.com/arthurhoch/kiss-json/actions/workflows/pages.yml/badge.svg)](https://github.com/arthurhoch/kiss-json/actions/workflows/pages.yml)

Part of the KISS Java Libraries family: small, explicit, zero-dependency Java 17+ libraries. Each project is independent. Use only the modules you need.

## Status

Latest stable release: `0.1.0`.

Current development version: `0.1.1-SNAPSHOT`.

The `0.1.0` artifact is published on Maven Central and the `v0.1.0` GitHub release is available.

## Why this exists

KissJson exists for Java projects that need practical JSON serialization and deserialization without introducing a large JSON framework or transitive runtime dependencies. It maps fields directly, keeps configuration explicit, and returns errors with enough context to debug malformed JSON or mapping failures.

## Installation

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-json</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start

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

## Design Principles

- KISS: keep the library small, explicit, and understandable.
- Zero production dependencies.
- Java 17+ standard APIs.
- Field-based behavior instead of JavaBean magic.
- Small public API and internal classes kept internal.
- Predictable errors with useful path, location, type, field, and cause details.
- Low maintenance and no framework lock-in.

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

## Related KISS Projects

These libraries are independent, zero-dependency Java 17+ projects. Use only the modules you need.

| Project | Purpose |
|---|---|
| [kiss-json](https://github.com/arthurhoch/kiss-json) | Field-based JSON serialization and deserialization. |
| [kiss-requests](https://github.com/arthurhoch/kiss-requests) | Simple HTTP client built on Java HttpClient. |
| [kiss-server](https://github.com/arthurhoch/kiss-server) | Small HTTP/1.1 server for simple REST-style applications. |
| [kiss-config](https://github.com/arthurhoch/kiss-config) | Configuration loading from properties, .env files, system properties, and environment variables. |
| [kiss-binary](https://github.com/arthurhoch/kiss-binary) | Explicit binary IO for primitive binary formats. |

## Benchmarks

JMH benchmarks, including a benchmark-only comparison with Jackson, run from the isolated `benchmark` Maven profile:

```bash
mvn -Pbenchmark clean test-compile
mvn -Pbenchmark exec:exec
```

Jackson is used only in that benchmark profile and is not a production dependency. Benchmark results depend on JVM, hardware, warmup, and payload shape; see [Performance](docs/PERFORMANCE.md) for scenarios and process.

## Requirements

- Java 17 or newer.
- Maven for building from source.

## Build

```bash
mvn -B clean verify
mvn -B test jacoco:report
mvn -B javadoc:javadoc
```

Additional configured profiles:

```bash
mvn -Pbenchmark clean test-compile
mvn -Psecurity verify
mvn -Pquality verify
```

## Security and Quality

Normal development and CI use the fast build:

```bash
mvn -B clean verify
```

Security-heavy dependency scanning runs separately:

```bash
mvn -Psecurity verify
```

Optional static quality checks run separately:

```bash
mvn -Pquality verify
```

JaCoCo coverage is generated during `verify` and can be regenerated locally with:

```bash
mvn -B test jacoco:report
```

Read the HTML report at `target/site/jacoco/index.html`. The XML report for Codecov or Sonar integrations is `target/site/jacoco/jacoco.xml`; no coverage badge is shown until one of those services is actually configured.

GitHub Actions run CI, CodeQL, Semgrep, GitHub Pages, and release workflows. Dependabot tracks Maven and GitHub Actions updates weekly. OWASP Dependency-Check is available through the `security` Maven profile. Snyk is optional/manual unless maintainers configure it later. See [Security Scanning](docs/SECURITY_SCANNING.md).

Before deleting code, follow [Safe Code Cleanup](docs/code-cleanup.md): distinguish internal code from public API, search source/tests/docs/examples, inspect coverage, run Javadocs, and document user-visible removals in `CHANGELOG.md`. Before release, run the normal build, Javadocs, coverage generation, and any relevant optional quality/security profiles.

## Documentation

- [GitHub Pages](https://arthurhoch.github.io/kiss-json/)
- [Product Specification](docs/PRODUCT_SPEC.md)
- [Getting Started](docs/GETTING_STARTED.md)
- [AI Usage Guide](docs/AI_USAGE.md)
- [API Reference](docs/API.md)
- [Configuration](docs/CONFIGURATION.md)
- [Examples](docs/EXAMPLES.md)
- [Error Handling](docs/ERROR_HANDLING.md)
- [Performance](docs/PERFORMANCE.md)
- [Security Scanning](docs/SECURITY_SCANNING.md)
- [Testing Report](docs/TESTING_REPORT.md)
- [Safe Code Cleanup](docs/code-cleanup.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Release Guide](docs/RELEASE.md)
- [Maven Central Guide](docs/MAVEN_CENTRAL.md)
- [Architecture](.github/architecture/index.md)

## License

Apache License 2.0. Copyright 2026 Arthur Hoch. See [LICENSE.txt](LICENSE.txt).
