# KissJson Product Specification

> **Status:** This document is the authoritative specification for KissJson v1.
> All features described here are the current v1 contract unless explicitly marked otherwise.
> Initial v1 release is `0.1.0`; see [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the original phased roadmap.

---

## 1. Mission

KissJson is a **tiny, high-performance, zero-dependency Java 17+ JSON library** that serializes Java objects to JSON and deserializes JSON to normal Java objects using **fields directly** — no getters, no setters, no JavaBean conventions, no framework magic.

The name stands for **K**eep **I**t **S**imple, **S**tupid **Json**.

---

## 2. Problem

Existing Java JSON libraries have significant drawbacks:

| Library | Problem |
|---------|---------|
| **Jackson** | ~1.5 MB jar, hundreds of classes, complex configuration, requires getters/setters, framework integration sprawl |
| **Gson** | ~500 KB jar, slower performance, `JsonElement` tree overhead, getter-based by default |
| **JSON-B / JSON-P** | API-only specifications, require a separate implementation, Jakarta EE baggage |

Common pain points:

- Large dependency trees pulled in transitively
- Complex configuration with too many options
- Required JavaBean conventions (getters/setters)
- Framework integration that bleeds into library design
- Internal classes exposed in public API
- Poor error messages with no context

KissJson exists for developers who want: **serialize this object, parse this JSON, done.**

---

## 3. Target Users

- Developers who want simple JSON serialization/deserialization
- Library authors who need a zero-dependency JSON solution
- Projects that cannot afford large transitive dependency trees
- Developers who prefer field-based mapping over JavaBean conventions
- Anyone who values clear error messages over framework flexibility

---

## 4. Philosophy

| Principle | Description |
|-----------|-------------|
| **KISS** | Keep It Simple, Stupid. Small, understandable, maintainable, composable, and focused. |
| **Zero dependencies** | No external libraries required. The JDK is enough. |
| **Native JDK** | Built only with Java 17+ standard APIs. No preview features. |
| **Memorable API** | `Json.create()`, `stringify()`, `parse()`. Easy to remember. |
| **Field-based mapping** | Use object fields directly. Never getters or setters. |
| **Simple object model** | Normal Java objects (POJOs). Not framework magic. |
| **Fast by design** | Metadata caching, direct writing, token-based parsing. |
| **Rich errors** | Path, line, column, target type, field name, real cause. |
| **No internal API exposure** | Only public API is user-facing. Internals are package-private. |
| **Safe defaults** | Forgiving, practical, hard to misuse. |

### 4.1 KISS Interpretation

KISS means **Keep It Simple, Stupid**. In the Unix tradition, KissJson should be a small tool that does one thing well instead of becoming a monolithic framework.

The one thing is:

> Convert between JSON text and normal Java object graphs using fields directly.

That boundary is intentional. KissJson should not handle HTTP, persistence, validation, routing, dependency injection, service discovery, logging, telemetry, schema validation, JSONPath, JSON Patch, or application runtime integration.

KissJson should compose with other tools without depending on them:

```java
String body = json.stringify(request);
HttpResult result = http.request(HttpMethod.POST, url, headers, body).execute();
Response response = json.parse(result.body(), Response.class);
```

Configuration exists only to cover common JSON behavior. It is not a mechanism for accumulating every rare edge case. Every new public class, method, enum, annotation, or configuration option must justify why the existing API cannot solve a common user need clearly.

Internal implementation may use metadata caching, direct writing, token-based parsing, date codecs, type conversion, and class models. Those details must remain internal. Users should not need to understand parser internals, token streams, caches, field models, or codecs to serialize and parse JSON.

---

## 5. v1 Scope

### 5.1 Supported Types

| Category | Types |
|----------|-------|
| Primitives | `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `char` |
| Wrappers | `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `Boolean`, `Character` |
| Text | `String` |
| Numbers | `BigDecimal`, `BigInteger` |
| Enums | Any `enum` type |
| Arrays | Any supported type `[]` |
| Collections | `List<T>`, `Map<String, T>` |
| Nested | Objects containing objects (recursive) |
| Null | `null` values |

### 5.2 Date/Time Types (intended v1 contract)

| Type | Default ISO Format |
|------|--------------------|
| `java.time.LocalDate` | `yyyy-MM-dd` |
| `java.time.LocalTime` | `HH:mm:ss` |
| `java.time.LocalDateTime` | `yyyy-MM-dd'T'HH:mm:ss` |
| `java.time.OffsetDateTime` | ISO-8601 with offset |
| `java.time.ZonedDateTime` | ISO-8601 with zone |
| `java.time.Instant` | ISO-8601 instant |
| `java.time.Duration` | ISO-8601 duration (`PT1H30M`) |
| `java.time.Period` | ISO-8601 period (`P1Y2M3D`) |
| `java.util.Date` | ISO-8601 instant |
| `java.util.Calendar` | ISO-8601 with timezone |

### 5.3 Annotations (intended v1 contract)

| Annotation | Description |
|------------|-------------|
| `@JsonName(String)` | Custom JSON key name for a field |
| `@JsonAliases(String[])` | Alternative JSON key names for deserialization |
| `@JsonIgnore` | Exclude field from serialization and deserialization |
| `@JsonRequired` | Field must be present during deserialization when `failOnMissingRequiredFields` is enabled |
| `@JsonIncludeNull` | Include this field in output even when null (overrides config) |
| `@JsonExcludeNull` | Exclude this field from output when null (overrides config) |
| `@JsonDateFormat(String)` | Custom date format pattern for a specific field |

### 5.4 Features (intended v1 contract)

- **Naming strategies**: IDENTITY, LOWER_CASE, UPPER_CASE, CAMEL_CASE, SNAKE_CASE, KEBAB_CASE
- **Null handling**: Include or exclude null fields globally or per-field
- **Unknown properties**: Ignore (default) or fail
- **Duplicate keys**: Last wins (default) or fail
- **Max depth**: Configurable nesting limit (default 128)
- **Cycle detection**: Detect circular references during serialization (default enabled)
- **Pretty print**: Configurable indentation
- **Rich errors**: Path, line, column, target type, field name, real cause
- **Unit tests**: Full test coverage for all public methods
- **Maven Central**: Published to Maven Central
- **CI/CD**: GitHub Actions for build, test, and release

---

## 6. v1 Non-Goals

The following are **explicitly out of scope** for v1:

- **No Jackson/Gson/JSON-B/JSON-P dependency** — KissJson is standalone
- **No custom serializers/deserializers** — no `TypeAdapter`, `JsonSerializer`, or equivalent
- **No polymorphic type metadata** — no `@TypeInfo`, no `@JsonSubTypes`
- **No `$id`/`$ref`** — no identity/preservation mode
- **No framework integration** — no Spring, Jakarta EE, Micronaut, etc.
- **No HTTP, persistence, routing, DI, logging, telemetry, or service discovery** — compose externally
- **No validation framework** — no Bean Validation, JSON Schema, etc.
- **No JSON Schema** — no schema generation or validation
- **No JSONPath** — no querying support
- **No JSON Patch** — no RFC 6902 support
- **No binary JSON** — no BSON, CBOR, Smile, etc.
- **No mixins, views, modules, service loaders, or classpath scanning** — no framework-style extension system
- **No getter/setter/JavaBean mapping** — fields only
- **No public internal classes** — internals are package-private
- **No production dependencies** — zero
- **No code generation** — runtime only
- **No Lombok** — handwritten code
- **No annotation processing** — runtime reflection only
- **No Java >17** — Java 17 only
- **No preview features** — stable APIs only
- **No enum ordinal support** — enums by name or `toString()` only

---

## 7. Public API

### 7.1 Package

All public API lives in: `io.github.arthurhoch.kissjson`

### 7.2 Entry Point: `Json`

```java
public final class Json {
    public static Json create();
    public static JsonBuilder builder();
    public JsonConfig config();
    public String stringify(Object value);
    public <T> T parse(String json, Class<T> type);
    public <T> List<T> parseList(String json, Class<T> elementType);
    public Map<String, Object> parseMap(String json);
    public <T> Map<String, T> parseMap(String json, Class<T> valueType);
}
```

### 7.3 Builder: `JsonBuilder`

```java
public final class JsonBuilder {
    public JsonBuilder fieldNaming(FieldNaming strategy);
    public JsonBuilder includeNulls(boolean value);
    public JsonBuilder failOnUnknownProperties(boolean value);
    public JsonBuilder failOnMissingRequiredFields(boolean value);
    public JsonBuilder failOnNullForPrimitives(boolean value);
    public JsonBuilder failOnDuplicateKeys(boolean value);
    public JsonBuilder failOnCycles(boolean value);
    public JsonBuilder maxDepth(int value);
    public JsonBuilder prettyPrint(boolean value);
    public JsonBuilder dateFormat(DateFormat value);
    public JsonBuilder zoneId(ZoneId value);
    public JsonBuilder enumMode(EnumMode value);
    public Json build();
}
```

### 7.4 Configuration: `JsonConfig`

```java
public final class JsonConfig {
    public FieldNaming fieldNaming();
    public boolean includeNulls();
    public boolean failOnUnknownProperties();
    public boolean failOnMissingRequiredFields();
    public boolean failOnNullForPrimitives();
    public boolean failOnDuplicateKeys();
    public boolean failOnCycles();
    public int maxDepth();
    public boolean prettyPrint();
    public DateFormat dateFormat();
    public ZoneId zoneId();
    public EnumMode enumMode();
}
```

`JsonConfig` is **immutable**. Instances are obtained via `Json.config()` or `JsonBuilder.build().config()`.

### 7.5 Exceptions

```java
public class JsonException extends RuntimeException {
    public String getMessage();
}

public final class JsonParseException extends JsonException {
    public int line();
    public int column();
    public int offset();
}

public final class JsonMappingException extends JsonException {
    public String jsonPath();
    public Class<?> targetType();
    public String fieldName();
    public Class<?> expectedType();
    public Object actualValue();
}
```

### 7.6 Enums

```java
public enum FieldNaming {
    IDENTITY,
    LOWER_CASE,
    UPPER_CASE,
    CAMEL_CASE,
    SNAKE_CASE,
    KEBAB_CASE
}

public enum DateFormat {
    ISO,
    EPOCH_MILLIS,
    EPOCH_SECONDS
}

public enum EnumMode {
    NAME,
    TO_STRING
}
```

### 7.7 Annotations

```java
@JsonName("custom_name")
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonName {
    String value();
}

@JsonAliases({"alias1", "alias2"})
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonAliases {
    String[] value();
}

@JsonIgnore
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonIgnore {}

@JsonRequired
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonRequired {}

@JsonIncludeNull
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonIncludeNull {}

@JsonExcludeNull
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonExcludeNull {}

@JsonDateFormat("yyyy-MM-dd")
@Target(FIELD)
@Retention(RUNTIME)
public @interface JsonDateFormat {
    String value();
}
```

---

## 8. Default Configuration

| Option | Default | Rationale |
|--------|---------|-----------|
| `fieldNaming` | `IDENTITY` | Java field names are the most common convention; no surprise transformation |
| `includeNulls` | `true` | Include null fields by default — lossless round-trip is the safe choice |
| `failOnUnknownProperties` | `false` | Ignore unknown properties — JSON often has extra fields; failing would be fragile |
| `failOnMissingRequiredFields` | `false` | Missing fields, including `@JsonRequired` fields, keep Java defaults unless strict required-field enforcement is enabled |
| `failOnNullForPrimitives` | `false` | Assign default value (0, false) for null → primitive — practical and forgiving |
| `failOnDuplicateKeys` | `false` | Last wins — matches common JSON parsers; failing is opt-in strict mode |
| `failOnCycles` | `true` | Cycle detection on by default — prevents infinite loops and `StackOverflowError` |
| `maxDepth` | `128` | Deeply nested JSON is rare; 128 is generous while preventing abuse |
| `prettyPrint` | `false` | Compact output by default — pretty print is opt-in |
| `dateFormat` | `ISO` | ISO-8601 is the standard; epoch is opt-in |
| `zoneId` | `UTC` | UTC is the neutral default for `Date`/`Calendar` conversion |
| `enumMode` | `NAME` | `Enum.name()` is the natural, stable representation |

All defaults are chosen to be **forgiving, practical, and hard to misuse**. The library should "just work" for the common case.

---

## 9. Field Mapping Rules

### 9.1 Field Selection

- All **non-static, non-transient** fields are eligible for mapping.
- **Static** fields are always ignored.
- **Transient** fields are always ignored.
- Fields annotated `@JsonIgnore` are always ignored.
- Superclass fields (up to but not including `Object`) are included.
- Field access uses `setAccessible(true)` for private fields.

### 9.2 Constructor Policy

- **No-arg constructor required for object deserialization.** It may be public, protected, package-private, or private.
- Fields are set via reflection using `setAccessible(true)`.
- If no no-arg constructor exists, a `JsonMappingException` is thrown with context.
- If field setting fails due to access restrictions (e.g., module system), a `JsonMappingException` is thrown with context.

### 9.3 JSON Key Resolution

1. If `@JsonName` is present, use its value as the JSON key.
2. Otherwise, apply the configured `FieldNaming` strategy to the Java field name.
3. `@JsonName` always takes precedence over the naming strategy.

### 9.4 Deserialization Key Matching

1. For each JSON key, check for a field with `@JsonName` matching the key.
2. If no match, check for a field whose name (after naming strategy) matches.
3. If no match, check for a field with `@JsonAliases` containing the key.
4. If no match and `failOnUnknownProperties` is true, throw `JsonMappingException`.
5. If no match and `failOnUnknownProperties` is false, ignore the key.

### 9.5 Required Fields

- A field annotated `@JsonRequired` must have a corresponding key in the JSON input only when `failOnMissingRequiredFields` is true.
- If the key is missing and `failOnMissingRequiredFields` is true, throw `JsonMappingException`.
- `null` values satisfy the requirement — the key must exist, but the value can be `null`.

### 9.6 Null Handling (Deserialization)

- If a JSON value is `null` and the target field is a **primitive type**:
  - If `failOnNullForPrimitives` is true → throw `JsonMappingException`.
  - If `failOnNullForPrimitives` is false → assign the default value (`0`, `false`, `'\0'`).
- If a JSON value is `null` and the target field is a **reference type** → assign `null`.

### 9.7 Null Handling (Serialization)

- If `includeNulls` is true (default) → include the field with `null` value.
- If `includeNulls` is false → omit the field.
- `@JsonIncludeNull` on a field overrides `includeNulls = false` for that field.
- `@JsonExcludeNull` on a field overrides `includeNulls = true` for that field.

### 9.8 Duplicate Keys

- If `failOnDuplicateKeys` is false (default) → last value wins.
- If `failOnDuplicateKeys` is true → throw `JsonParseException` on first duplicate.

### 9.9 Max Depth

- Depth is tracked during both serialization and deserialization.
- If depth exceeds `maxDepth` → throw `JsonException`.
- Default: 128.

### 9.10 Cycle Detection

- If `failOnCycles` is true (default) → detect circular references during serialization.
- Uses `IdentityHashMap<Object, ?>` to track visited objects.
- On cycle detection → throw `JsonException` with the path to the cycle.
- Only applies to object types (not primitives, strings, or boxed numbers).

### 9.11 Naming Strategies

| Strategy | Example: `userName` → |
|----------|----------------------|
| `IDENTITY` | `userName` |
| `LOWER_CASE` | `username` |
| `UPPER_CASE` | `USERNAME` |
| `CAMEL_CASE` | `userName` |
| `SNAKE_CASE` | `user_name` |
| `KEBAB_CASE` | `user-name` |

### 9.12 Enum Handling

- `EnumMode.NAME` (default): Use `Enum.name()` for both serialization and deserialization.
- `EnumMode.TO_STRING`: Use `Enum.toString()` for serialization, match by `toString()` for deserialization.
- If enum value not found → throw `JsonMappingException` with the invalid value and enum type.

### 9.13 Date/Time Handling

- `DateFormat.ISO` (default): Use ISO-8601 string format.
- `DateFormat.EPOCH_MILLIS`: Use `long` epoch milliseconds for instant-like temporal types (`Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date`, `Calendar`).
- `DateFormat.EPOCH_SECONDS`: Use `long` epoch seconds for instant-like temporal types (`Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date`, `Calendar`).
- `LocalDate`, `LocalTime`, `LocalDateTime`, `Duration`, and `Period` remain ISO for epoch modes because there is no safe universal epoch conversion.
- `@JsonDateFormat("pattern")` overrides the global format for a specific field.
- `zoneId` applies to `java.util.Date` and `java.util.Calendar` conversion.

---

## 10. Error Behavior

### 10.1 Exception Hierarchy

```
RuntimeException
└── JsonException
    ├── JsonParseException     (syntax errors in JSON text)
    └── JsonMappingException   (type mapping errors)
```

### 10.2 JsonParseException

Thrown when JSON text is syntactically invalid.

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `line` | `int` | 1-based line number |
| `column` | `int` | 1-based column number |
| `offset` | `long` | 0-based character offset |

**Example message:**

```
Unexpected character '}' at line 3, column 15 (offset 142): expected ':' after key
```

### 10.3 JsonMappingException

Thrown when JSON cannot be mapped to the target Java type.

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `jsonPath` | `String` | JSON path to the error, e.g. `$.user.address.zipCode` |
| `targetType` | `Class<?>` | The Java class being deserialized |
| `fieldName` | `String` | The Java field name (if applicable) |
| `expectedType` | `Class<?>` | The expected Java type |
| `actualValue` | `Object` | The actual JSON value (truncated if too long) |

**Example messages:**

```
Cannot map STRING to int at $.user.age [target=User, field=age, expected=int, actual="thirty"]
```

```
Required field 'email' is missing at $.user [target=User, field=email]
```

```
Unknown enum value 'ACTIVE' for Status at $.status [target=Order, field=status, expected=Status, actual="ACTIVE"]
```

```
Unknown property 'phone_number' at $.user [target=User, field=phoneNumber]
  Hint: Did you mean 'phoneNumber'? Or enable a naming strategy like SNAKE_CASE.
```

### 10.4 Error Principles

- Every exception includes **human-readable context**.
- Every exception includes the **root cause** in the message.
- **No wrapping** in generic `RuntimeException` — always `JsonParseException` or `JsonMappingException`.
- Errors should help the developer **fix the problem**, not just report it.

---

## 11. Performance Expectations

KissJson is designed to be fast without sacrificing simplicity.

### 11.1 Design Principles

| Technique | Where | Why |
|-----------|-------|-----|
| **Metadata caching** | Class model | Parse class fields once, reuse for all instances |
| **Precomputed field names** | Field model | Compute JSON key name once, not per call |
| **Direct writing** | Serializer | Write directly to `Appendable`, no intermediate tree |
| **Token-based parsing** | Parser | Stream tokens, no full tree in memory |
| **No regex** | Parser | Hand-written tokenizer, faster than regex |
| **IdentityHashMap** | Cycle detection | Identity-based comparison, not `equals()` |
| **StringBuilder** | Writer | Efficient string building for output |

### 11.2 Caching

- Class metadata (fields, annotations, types) is cached per `Class<?>` in a `ConcurrentHashMap`.
- Cache is shared across all `Json` instances with the same configuration.
- Cache grows unbounded (classes are loaded once and rarely unloaded).

### 11.3 Thread Safety

- `Json` instances are **thread-safe for reads** (serialization and deserialization).
- `JsonConfig` is immutable.
- `JsonBuilder` is **not** thread-safe (single-use builder pattern).
- Internal caches use `ConcurrentHashMap`.

### 11.4 Memory

- **Serialization**: No intermediate tree. Direct write to `Appendable`.
- **Deserialization**: Token-based streaming. Only the target object graph is in memory.
- No `JsonElement`-like intermediate representation exposed to users.

### 11.5 Benchmarks

Benchmarks are isolated in the Maven `benchmark` profile and are not part of normal verification. The current comparison benchmark covers KissJson and Jackson (`ObjectMapper`) on equivalent payload shapes.

The goal is to be **competitive with Jackson** for common use cases while maintaining a much smaller footprint.

---

## 12. Security

- **Zero dependencies** — no transitive vulnerability surface.
- **No secrets** — no API keys, tokens, or credentials in the codebase.
- **No network access** — the library never makes network calls.
- **No file system access** — the library never reads or writes files.
- **No code execution** — no `Runtime.exec()`, `ProcessBuilder`, or `ScriptEngine`.
- **Reflection** — uses `setAccessible(true)` for private fields. This is standard practice for field-based mapping.
- **Max depth** — prevents deeply nested JSON from causing `StackOverflowError`.
- **Cycle detection** — prevents circular references from causing infinite loops.
- **Input validation** — all public methods validate arguments and throw `NullPointerException` or `IllegalArgumentException` for null/invalid inputs where appropriate.

---

## 13. Acceptance Criteria

The following criteria must be met before v1 release:

### 13.1 Type Support

- [ ] All primitive types serialize/deserialize correctly
- [ ] All wrapper types serialize/deserialize correctly
- [ ] `String` serializes/deserializes correctly
- [ ] `BigDecimal` and `BigInteger` serialize/deserialize correctly
- [ ] `char`/`Character` serializes/deserializes correctly
- [ ] Enum types serialize/deserialize correctly
- [ ] Arrays of all supported types serialize/deserialize correctly
- [ ] `List<T>` of all supported types serializes/deserializes correctly
- [ ] `Map<String, T>` of all supported types serializes/deserializes correctly
- [ ] Nested objects serialize/deserialize correctly
- [ ] `null` values serialize/deserialize correctly
- [ ] All date/time types serialize/deserialize correctly

### 13.2 Annotations

- [ ] `@JsonName` customizes JSON key
- [ ] `@JsonAliases` accepts alternative keys
- [ ] `@JsonIgnore` excludes fields
- [ ] `@JsonRequired` enforces presence when required-field enforcement is enabled
- [ ] `@JsonIncludeNull` includes null fields
- [ ] `@JsonExcludeNull` excludes null fields
- [ ] `@JsonDateFormat` customizes date format per field

### 13.3 Configuration

- [ ] All `JsonBuilder` options work correctly
- [ ] Default configuration is correct
- [ ] Custom configuration overrides work

### 13.4 Error Handling

- [ ] `JsonParseException` includes line, column, offset
- [ ] `JsonMappingException` includes jsonPath, targetType, fieldName
- [ ] Error messages are human-readable and helpful

### 13.5 Quality

- [ ] All public methods have at least one test
- [ ] `mvn -B verify` passes
- [ ] No production dependencies
- [ ] No internal classes exposed as public API
- [ ] Java 17 only (no higher, no preview features)
- [ ] Documentation is consistent with implementation

### 13.6 Publishing

- [ ] Published to Maven Central via Sonatype Central Publisher Portal
- [ ] GPG signed
- [ ] CI/CD pipeline passes

---

## 14. Implementation Roadmap

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the phased implementation roadmap.

---

## 15. Document History

| Date | Version | Notes |
|------|---------|-------|
| 2025-04-29 | 0.1.0 | Initial product specification |

---

*This document is the single source of truth for KissJson v1. All other documentation must be consistent with this spec.*
