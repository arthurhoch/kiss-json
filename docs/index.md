---
layout: default
title: KissJson
---

# KissJson

A tiny, high-performance, zero-dependency Java 17+ JSON library.

KissJson serializes Java objects to JSON and deserializes JSON to normal Java objects using **fields directly** — no getters, no setters, no framework magic.

> **Status:** Initial v1 implementation is present for `0.1.0-SNAPSHOT` and is under release hardening.

## Maven Dependency

```xml
<dependency>
  <groupId>io.github.arthurhoch</groupId>
  <artifactId>kiss-json</artifactId>
  <version>0.1.0-SNAPSHOT</version>
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

## Documentation

| Document | Description |
|----------|-------------|
| [Product Spec](PRODUCT_SPEC.html) | Authoritative v1 specification |
| [Getting Started](GETTING_STARTED.html) | Quick-start guide with copyable examples |
| [API Reference](API.html) | Public API documentation |
| [Examples](EXAMPLES.html) | Complete examples for all v1 behaviors |
| [Configuration](CONFIGURATION.html) | All builder options documented |
| [Error Handling](ERROR_HANDLING.html) | Exception hierarchy and rich errors |
| [Performance](PERFORMANCE.html) | Performance design and expectations |
| [Benchmarks](BENCHMARKS.html) | JMH benchmark results vs Jackson |
| [Security Scanning](SECURITY_SCANNING.html) | CodeQL, Semgrep, Dependabot, OWASP, SpotBugs, and optional Snyk |
| [Implementation Plan](IMPLEMENTATION_PLAN.html) | Phased implementation roadmap |
| [Review Checklist](REVIEW_CHECKLIST.html) | Pre-release review checklist |
| [Maven Central](MAVEN_CENTRAL.html) | Publishing to Maven Central |
| [Release](RELEASE.html) | Release process |

## License

KissJson is open source. See the repository for license details.
