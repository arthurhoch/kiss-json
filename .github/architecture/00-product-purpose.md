# 00 — Product Purpose

## Why KissJson Exists

### The Problem

Existing Java JSON libraries carry significant baggage:

- **Jackson** is powerful but enormous. It requires modules, SPI plugins, annotation configurations, and a deep understanding of `ObjectMapper` internals. A basic "object to JSON" operation requires understanding `JsonFactory`, `JsonGenerator`, `JsonParser`, module registration, mixin annotations, and serialization views. The learning curve is steep for simple use cases.

- **Gson** is simpler but still framework-heavy. It relies on getter/setter conventions, has inconsistent null handling, and its `TypeToken` system for generics is error-prone. Error messages are often uninformative, making debugging difficult.

- **Both** require getters and setters for field mapping, add unnecessary abstraction layers, produce unhelpful error messages, and carry transitive dependencies in larger projects. Neither was designed with "zero setup, zero surprises" as a primary goal.

### The KissJson Answer

KissJson is for developers who think:

> "I have a plain Java object with fields. I want it as JSON. Then I want it back. That's it."

KissJson does one thing well: convert between JSON and normal Java object graphs using fields directly.

KissJson provides:

- **Zero dependencies** — no external JARs, no modules, no SPI, no classpath scanning.
- **Field-based mapping** — reads and writes object fields directly via reflection. No getters. No setters. No JavaBean conventions.
- **Memorable API** — `json.stringify(obj)` and `json.parse(text, MyClass.class)`. No `ObjectMapper` to configure, no `GsonBuilder` to chain. Start with `Json.create()`, customize with `Json.builder()`.
- **Rich errors** — every exception includes the JSON path, line, column, field name, expected type, actual value, and root cause. No more staring at "JsonSyntaxException" with no context.
- **Safe defaults** — unknown properties are ignored, missing fields keep their defaults, nulls are handled gracefully, duplicate keys use last-wins, cycles fail with a clear message.
- **Small footprint** — a single JAR, minimal code, class metadata cached internally, no dependency tree.

### Target Users

KissJson is designed for:

- Java developers building services, CLIs, or applications who need straightforward JSON serialization.
- Developers who prefer plain objects over framework-managed entities.
- Teams that want zero external dependencies in their JSON layer.
- Projects that value clear error messages over extensive configurability.
- Developers who want a library they can understand end-to-end in an afternoon.

### Not For

KissJson is **not** designed for:

- Developers who need framework integration (Spring, Jakarta, Micronaut, Quarkus).
- Applications that want JSON handling to also perform HTTP, persistence, routing, dependency injection, service discovery, logging, or telemetry.
- Use cases requiring streaming JSON processing (event-based parsing of huge files).
- Projects that need a tree model API (like Jackson's `JsonNode` or Gson's `JsonObject`).
- Developers who need custom serializers/deserializers for complex types.
- Applications requiring JSON Patch, JSON Path, JSON Schema, or binary JSON formats.
- Applications requiring polymorphic type metadata, object identity, `$id`/`$ref`, mixins, views, modules, service loaders, or classpath scanning.
- Projects that depend on getter/setter-based mapping or JavaBean conventions.

## Philosophy

### KISS — Keep It Simple, Stupid

Every design decision favors simplicity over flexibility. In the Unix tradition, a good tool is small, understandable, maintainable, composable, and focused. When two solutions exist, choose the simpler one. When a feature adds complexity without proportional value, omit it.

KissJson should remain a JSON library, not a framework. Complexity must be justified, not assumed.

### Do One Thing Well

KissJson exists to serialize JSON and deserialize JSON for normal Java objects. It should not handle HTTP, persistence, validation, routing, dependency injection, service discovery, logging, telemetry, schema validation, JSONPath, JSON Patch, or framework integration.

Users should compose KissJson with those tools externally:

```java
String body = json.stringify(request);
HttpResult result = http.request(HttpMethod.POST, url, headers, body).execute();
Response response = json.parse(result.body(), Response.class);
```

### Small Is Beautiful

The public API must stay small and memorable. The core mental model is:

```java
Json json = Json.create();
String text = json.stringify(user);
User user = json.parse(text, User.class);
```

Every new public class, method, enum, annotation, or configuration option must justify why this model is not enough for a common user need.

### Zero Dependencies

The library must run on any Java 17+ JVM with no additional JARs. No shading, no optional dependencies, no "provided" scope tricks. This is non-negotiable.

### Java 17 Only

Target Java 17. No higher. No preview features. This ensures maximum compatibility while benefiting from modern language features like sealed classes (if needed internally), pattern matching for `instanceof`, and text blocks.

### Field-Based

Objects are mapped by their fields, not by methods. This means:

- No getter naming conventions to learn.
- No setter naming conventions to remember.
- No `@JsonProperty` on both getter and setter to keep in sync.
- No confusion about which method controls serialization vs. deserialization.

Fields are fields. JSON keys are field names (with configurable naming strategies). It just works.

### Configuration Is Limited

Configuration exists to cover common JSON behavior, not every edge case. A configuration option is acceptable only when it keeps the main API simple and addresses a common need. Rare framework-like features should be deferred or rejected.

### Internals Stay Internal

KissJson may use parser tokens, metadata caches, class models, field models, date codecs, and type converters internally. None of these should leak into public API. A normal Java developer should not need to understand those internals to use the library.

### Rich Errors

Every exception must tell the developer exactly what went wrong and where. Error messages include:

- The JSON path to the problematic value (e.g., `$.orders[2].items[0].price`).
- Line and column numbers for parse errors.
- The target Java type, field name, expected type, and actual JSON value for mapping errors.
- The root cause wrapped (not swallowed).

### Safe Defaults

The library must be forgiving by default and strict only when explicitly configured:

- Unknown JSON properties are silently ignored (not errors).
- Missing JSON fields leave Java fields at their defaults (not null pointer exceptions).
- Null values for primitive fields keep the Java default (0, false, etc.) rather than throwing.
- Duplicate keys use last-wins behavior (not errors).
- Cycles in object graphs fail with a clear message (not infinite recursion).
- All of these can be made strict via configuration, but the defaults are practical.

## Status

This document describes the **current v1 contract** for `0.1.0`.
