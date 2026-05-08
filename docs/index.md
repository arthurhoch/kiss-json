---
layout: default
title: KissJson
---

# KissJson

Tiny zero-dependency Java 17+ JSON library for field-based serialization and deserialization.

Part of the KISS Java Libraries family.

KissJson serializes Java objects to JSON and deserializes JSON to normal Java objects using **fields directly**: no getters, no setters, no framework magic.

> **Status:** Latest stable release is `0.1.0`; current development version is `0.1.1-SNAPSHOT`.

## Install

```xml
<dependency>
  <groupId>io.github.arthurhoch</groupId>
  <artifactId>kiss-json</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Quick Example

```java
import io.github.arthurhoch.kissjson.Json;

public class User {
    String name;
    int age;
    boolean active;
}

// Serialize
Json json = Json.create();
User user = new User();
user.name = "Alice";
user.age = 30;
user.active = true;
String text = json.stringify(user);
// {"name":"Alice","age":30,"active":true}

// Deserialize
User parsed = json.parse(text, User.class);
// parsed.name == "Alice", parsed.age == 30, parsed.active == true
```

## Why KissJson?

- **Tiny** — zero dependencies, small jar
- **Fast** — metadata caching, direct writing, token-based parsing
- **Simple** — field-based mapping, no getters/setters required
- **Safe** — rich errors with path, line, column, and cause context
- **Modern** — Java 17+, no legacy baggage

## Core Features

- Field-based object serialization and deserialization.
- Primitives, wrappers, strings, numbers, dates, enums, arrays, lists, maps, nested objects, and nulls.
- Annotations for names, aliases, ignored fields, required fields, null inclusion/exclusion, and date format.
- Configurable naming, null handling, unknown-property handling, duplicate-key handling, depth limits, cycle handling, pretty print, dates, and enums.
- JMH benchmark profile and published benchmark methodology.

## Documentation

| Document | Description |
|----------|-------------|
| [Product Spec](PRODUCT_SPEC.html) | Authoritative v1 specification |
| [Getting Started](GETTING_STARTED.html) | Quick-start guide with copyable examples |
| [AI Usage](AI_USAGE.html) | User-facing AI usage guide for consumer projects |
| [API Reference](API.html) | Public API documentation |
| [Examples](EXAMPLES.html) | Complete examples for all v1 behaviors |
| [Configuration](CONFIGURATION.html) | All builder options documented |
| [Error Handling](ERROR_HANDLING.html) | Exception hierarchy and rich errors |
| [Performance](PERFORMANCE.html) | Performance design and expectations |
| [Benchmarks](BENCHMARKS.html) | JMH benchmark results vs Jackson |
| [Security Scanning](SECURITY_SCANNING.html) | CodeQL, Semgrep, Dependabot, OWASP, SpotBugs, and optional Snyk |
| [Security Hardening](security-hardening.html) | CodeQL build, Dependency Review, OpenSSF Scorecard, and manual GitHub setup |
| [Testing Report](TESTING_REPORT.html) | Current verification results and known limits |
| [Safe Code Cleanup](code-cleanup.html) | Coverage, quality checks, and deletion policy |
| [Implementation Plan](IMPLEMENTATION_PLAN.html) | Phased implementation roadmap |
| [Review Checklist](REVIEW_CHECKLIST.html) | Pre-release review checklist |
| [Maven Central](MAVEN_CENTRAL.html) | Publishing to Maven Central |
| [Release](RELEASE.html) | Release process |

## Related KISS Projects

These libraries are independent, zero-dependency Java 17+ projects. Use only the modules you need.

| Project | Purpose |
|---|---|
| [kiss-json](https://github.com/arthurhoch/kiss-json) | Field-based JSON serialization and deserialization. |
| [kiss-requests](https://github.com/arthurhoch/kiss-requests) | Simple HTTP client built on Java HttpClient. |
| [kiss-server](https://github.com/arthurhoch/kiss-server) | Small HTTP/1.1 server for simple REST-style applications. |
| [kiss-config](https://github.com/arthurhoch/kiss-config) | Configuration loading from properties, .env files, system properties, and environment variables. |
| [kiss-binary](https://github.com/arthurhoch/kiss-binary) | Explicit binary IO for primitive binary formats. |

## Links

- [GitHub](https://github.com/arthurhoch/kiss-json)
- [Maven Central](https://central.sonatype.com/artifact/io.github.arthurhoch/kiss-json)
- [Changelog](https://github.com/arthurhoch/kiss-json/blob/main/CHANGELOG.md)
- [Security Policy](https://github.com/arthurhoch/kiss-json/blob/main/SECURITY.md)

## License

KissJson is open source. See the repository for license details.
