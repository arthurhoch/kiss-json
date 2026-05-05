# 01 — Public API Contract

This document defines the **current v1 public API** for KissJson `0.1.0`.

## Package

```
io.github.arthurhoch.kissjson
```

This is the only package users import from. All other packages are internal and must not be imported by users.

---

## Classes

### `Json`

The main entry point. A facade that delegates all work to internal components. Users never interact with internal classes.

```java
public final class Json
```

**Static Factory Methods:**

| Method | Returns | Description |
|--------|---------|-------------|
| `Json create()` | `Json` | Creates a `Json` instance with default configuration. |
| `JsonBuilder builder()` | `JsonBuilder` | Returns a new `JsonBuilder` for constructing a configured `Json` instance. |

**Serialization:**

| Method | Description |
|--------|-------------|
| `String stringify(Object value)` | Serializes the given object to a JSON string. Compact output by default. |

**Deserialization:**

| Method | Description |
|--------|-------------|
| `<T> T parse(String json, Class<T> type)` | Deserializes a JSON string into an object of the given type. |
| `<T> List<T> parseList(String json, Class<T> elementType)` | Deserializes a JSON array into a `List` of the given element type. |
| `<T> Map<String, T> parseMap(String json, Class<T> valueType)` | Deserializes a JSON object into a `Map<String, T>` with the given value type. |
| `Map<String, Object> parseMap(String json)` | Deserializes a JSON object into a `Map<String, Object>` with natural value types (String, Integer, Long, Double, BigDecimal, Boolean, null, List, Map). |

**Configuration:**

| Method | Description |
|--------|-------------|
| `JsonConfig config()` | Returns the active configuration for this instance. |

---

### `JsonBuilder`

Builder for constructing configured `Json` instances.

```java
public final class JsonBuilder
```

**Builder Methods (each returns `this` for chaining):**

| Method | Description |
|--------|-------------|
| `JsonBuilder prettyPrint(boolean prettyPrint)` | Enable or disable pretty-printed output. Default: `false`. |
| `JsonBuilder fieldNaming(FieldNaming strategy)` | Set the field naming strategy. Default: `FieldNaming.IDENTITY`. |
| `JsonBuilder dateFormat(DateFormat format)` | Set the date/time serialization format. Default: `DateFormat.ISO`. |
| `JsonBuilder enumMode(EnumMode mode)` | Set the enum serialization mode. Default: `EnumMode.NAME`. |
| `JsonBuilder includeNulls(boolean includeNulls)` | Include null fields in serialized output. Default: `true`. |
| `JsonBuilder failOnUnknownProperties(boolean fail)` | Throw on unknown JSON properties during deserialization. Default: `false`. |
| `JsonBuilder failOnNullForPrimitives(boolean fail)` | Throw when a null JSON value maps to a primitive Java field. Default: `false`. |
| `JsonBuilder failOnMissingRequiredFields(boolean fail)` | Throw when `@JsonRequired` fields are absent from JSON. Default: `false`. |
| `JsonBuilder failOnCycles(boolean fail)` | Throw when a reference cycle is detected during serialization. Default: `true`. |
| `JsonBuilder failOnDuplicateKeys(boolean fail)` | Throw when duplicate keys are encountered during parsing. Default: `false`. |
| `JsonBuilder maxDepth(int maxDepth)` | Set the maximum nesting depth for parsing and serialization. Default: `128`. Must be at least `1`. |

**Terminal Method:**

| Method | Description |
|--------|-------------|
| `Json build()` | Builds and returns a new `Json` instance with the configured settings. |

---

### `JsonConfig`

Immutable configuration object. Cannot be modified after creation. Obtain via `Json.config()` or construct via `JsonBuilder`.

```java
public final class JsonConfig
```

**Accessor Methods:**

| Method | Returns | Default |
|--------|---------|---------|
| `boolean prettyPrint()` | `boolean` | `false` |
| `FieldNaming fieldNaming()` | `FieldNaming` | `IDENTITY` |
| `DateFormat dateFormat()` | `DateFormat` | `ISO` |
| `EnumMode enumMode()` | `EnumMode` | `NAME` |
| `boolean includeNulls()` | `boolean` | `true` |
| `boolean failOnUnknownProperties()` | `boolean` | `false` |
| `boolean failOnNullForPrimitives()` | `boolean` | `false` |
| `boolean failOnMissingRequiredFields()` | `boolean` | `false` |
| `boolean failOnCycles()` | `boolean` | `true` |
| `boolean failOnDuplicateKeys()` | `boolean` | `false` |
| `int maxDepth()` | `int` | `128` |

---

### `JsonException`

Base exception for all KissJson errors. Extends `RuntimeException`.

```java
public class JsonException extends RuntimeException
```

| Constructor | Description |
|-------------|-------------|
| `JsonException(String message)` | Creates an exception with a message. |
| `JsonException(String message, Throwable cause)` | Creates an exception with a message and cause. |

---

### `JsonParseException`

Thrown when JSON text cannot be parsed. Extends `JsonException`. Always includes line, column, and byte offset.

```java
public final class JsonParseException extends JsonException
```

| Method | Returns | Description |
|--------|---------|-------------|
| `int line()` | `int` | The 1-based line number where the parse error occurred. |
| `int column()` | `int` | The 1-based column number where the parse error occurred. |
| `int offset()` | `int` | The 0-based character offset where the parse error occurred. |

---

### `JsonMappingException`

Thrown when JSON cannot be mapped to or from a Java object. Extends `JsonException`. Includes rich context about what went wrong.

```java
public final class JsonMappingException extends JsonException
```

| Method | Returns | Description |
|--------|---------|-------------|
| `String jsonPath()` | `String` | The JSON path to the problematic value (e.g., `$.user.address.city`), or `null` if not applicable. |
| `Class<?> targetType()` | `Class<?>` | The target Java type being mapped to, or `null` if not applicable. |
| `String fieldName()` | `String` | The Java field name being mapped, or `null` if not applicable. |
| `Class<?> expectedType()` | `Class<?>` | The expected Java type for the field, or `null` if not applicable. |
| `Object actualValue()` | `Object` | The actual JSON value encountered, or `null` if not applicable. |

---

## Enums

### `FieldNaming`

Defines how Java field names are converted to JSON keys during serialization and deserialization.

```java
public enum FieldNaming
```

| Value | Example Field Name | JSON Key |
|-------|--------------------|----------|
| `IDENTITY` | `userName` | `userName` |
| `LOWER_CASE` | `userName` | `username` |
| `UPPER_CASE` | `userName` | `USERNAME` |
| `CAMEL_CASE` | `user_name` | `userName` |
| `SNAKE_CASE` | `userName` | `user_name` |
| `KEBAB_CASE` | `userName` | `user-name` |

Note: `CAMEL_CASE` assumes the Java field uses snake_case and converts to camelCase. The other strategies assume camelCase Java fields and convert accordingly.

### `DateFormat`

Defines how date/time types are serialized and deserialized.

```java
public enum DateFormat
```

| Value | Serialization Example | Description |
|-------|-----------------------|-------------|
| `ISO` | `"2025-01-15T10:30:00"` | ISO-8601 string format. |
| `EPOCH_MILLIS` | `1736934600000` | Unix epoch milliseconds for instant-like temporal types. |
| `EPOCH_SECONDS` | `1736934600` | Unix epoch seconds for instant-like temporal types. |

### `EnumMode`

Defines how Java enums are serialized and deserialized.

```java
public enum EnumMode
```

| Value | Serialization | Deserialization |
|-------|---------------|-----------------|
| `NAME` | `enumValue.name()` | Match by `name()`, case-sensitive. |
| `TO_STRING` | `enumValue.toString()` | Match by `toString()`, case-sensitive. |

---

## Annotations

All annotations are in `io.github.arthurhoch.kissjson` and target `FIELD`.

### `@JsonName`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonName {
    String value();
}
```

Overrides the JSON key for the annotated field. Takes precedence over any `FieldNaming` strategy. Applied during both serialization and deserialization.

**Example:**

```java
@JsonName("user_name")
private String userName;
```

### `@JsonAliases`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAliases {
    String[] value();
}
```

Defines alternate JSON keys accepted during deserialization. The primary name (from field name, naming strategy, or `@JsonName`) is always accepted. Aliases are checked only if the primary name is not found. Serialization always uses the primary name.

**Example:**

```java
@JsonAliases({"login", "handle"})
private String username;
```

### `@JsonIgnore`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnore {
}
```

Excludes the field from both serialization and deserialization. The field keeps its Java default during deserialization and is not written during serialization.

**Example:**

```java
@JsonIgnore
private String internalCache;
```

### `@JsonRequired`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRequired {
}
```

Marks a field as required during deserialization. If the JSON does not contain this field (either by primary name or alias) and `failOnMissingRequiredFields` is `true`, a `JsonMappingException` is thrown. If `failOnMissingRequiredFields` is `false` (the default), this annotation has no effect.

**Example:**

```java
@JsonRequired
private String id;
```

### `@JsonIncludeNull`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIncludeNull {
}
```

Forces the field to be included in serialized output even when its value is `null`. This overrides the global `includeNulls(false)` setting for this specific field. Has no effect when the global setting is `includeNulls(true)`.

### `@JsonExcludeNull`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonExcludeNull {
}
```

Excludes the field from serialized output when its value is `null`. This overrides the global `includeNulls(true)` setting for this specific field. Has no effect when the global setting is `includeNulls(false)`.

**Conflict rule:** If both `@JsonIncludeNull` and `@JsonExcludeNull` are present on the same field, a `JsonMappingException` is thrown at mapping time.

### `@JsonDateFormat`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonDateFormat {
    String value();
}
```

Overrides the global date/time formatting with a `DateTimeFormatter` pattern for this specific field. Applied during both serialization and deserialization for compatible temporal string formatting.

**Example:**

```java
@JsonDateFormat("yyyy-MM-dd")
private LocalDate createdAt;
```

---

## Default Configuration

| Setting | Default | Rationale |
|---------|---------|-----------|
| `prettyPrint` | `false` | Compact output is the norm for APIs and storage. |
| `fieldNaming` | `IDENTITY` | Java field names as-is. No surprises. |
| `dateFormat` | `ISO` | ISO-8601 is the standard, human-readable format. |
| `enumMode` | `NAME` | `name()` is stable and predictable. |
| `includeNulls` | `true` | Explicit nulls avoid ambiguity about missing vs. null. |
| `failOnUnknownProperties` | `false` | Tolerant of API evolution and extra fields. |
| `failOnNullForPrimitives` | `false` | Nulls become Java defaults (0, false) rather than errors. |
| `failOnMissingRequiredFields` | `false` | Only strict when explicitly requested via config + annotation. |
| `failOnCycles` | `true` | Cycles cause infinite recursion by default; fail loudly. |
| `failOnDuplicateKeys` | `false` | Last-wins is the practical default for duplicate keys. |
| `maxDepth` | `128` | Prevents stack overflow on deeply nested input. |

---

## What Is NOT Public API

The following are internal implementation details and must never be imported or referenced by users:

- Internal package: `io.github.arthurhoch.kissjson.internal`
- `JsonReader` / `JsonParser` — token-based parser and parser state
- `JsonWriter` — direct string writer
- `JsonTokenType` — internal token enum
- `JsonValue` — internal value model for limited generic representation
- `ObjectReader` — JSON to Java object mapper
- `ObjectWriter` — Java object to JSON mapper
- `ClassModel` — cached field metadata for a class
- `FieldModel` — per-field metadata
- `ClassModelCache` — concurrent class metadata cache
- `NamingStrategy` — field name conversion logic
- `DateCodec` — date/time encoding and decoding
- `TypeConverter` — type conversion utilities
- `JsonPaths` — path tracking for error context
- All token types, parser states, and internal utility classes

Users interact only with the classes, enums, and annotations defined above.

## Status

This document describes the **current v1 contract**. All types and methods listed here are implemented for `0.1.0`.
