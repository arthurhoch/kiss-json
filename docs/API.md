# API Reference

> **Note:** All API described below is the current v1 contract for `0.1.0-SNAPSHOT`.

All public API lives in the package `io.github.arthurhoch.kissjson`.

---

## `Json`

The main entry point for serialization and deserialization.

```java
public final class Json
```

### `create()`

```java
public static Json create()
```

Creates a `Json` instance with default configuration. Equivalent to `Json.builder().build()`.

### `builder()`

```java
public static JsonBuilder builder()
```

Returns a new `JsonBuilder` for configuring a `Json` instance with custom options.

### `config()`

```java
public JsonConfig config()
```

Returns the immutable `JsonConfig` for this instance.

### `stringify(Object)`

```java
public String stringify(Object value)
```

Serializes the given Java object to a JSON string.

- **Parameters:** `value` — the object to serialize. May be `null` (returns `"null"`).
- **Returns:** JSON string.
- **Throws:**
  - `JsonException` — if cycle detection triggers or max depth is exceeded.

### `parse(String, Class<T>)`

```java
public <T> T parse(String json, Class<T> type)
```

Deserializes a JSON string to an object of the given type.

- **Parameters:**
  - `json` — the JSON string to parse.
  - `type` — the target class.
- **Returns:** deserialized object.
- **Throws:**
  - `JsonParseException` — if the JSON is syntactically invalid.
  - `JsonMappingException` — if the JSON cannot be mapped to the target type.
  - `NullPointerException` — if `json` or `type` is null.

### `parseList(String, Class<T>)`

```java
public <T> List<T> parseList(String json, Class<T> elementType)
```

Deserializes a JSON array to a `List<T>`.

- **Parameters:**
  - `json` — the JSON array string.
  - `elementType` — the element type.
- **Returns:** `List<T>` of deserialized elements.
- **Throws:**
  - `JsonParseException` — if the JSON is syntactically invalid or not an array.
  - `JsonMappingException` — if an element cannot be mapped.

### `parseMap(String)`

```java
public Map<String, Object> parseMap(String json)
```

Deserializes a JSON object to a `Map<String, Object>`. Values are mapped to their natural Java types:

| JSON type | Java type |
|-----------|-----------|
| string | `String` |
| number | `Long` or `Double` |
| boolean | `Boolean` |
| null | `null` |
| array | `List<Object>` |
| object | `Map<String, Object>` |

- **Parameters:** `json` — the JSON object string.
- **Returns:** `Map<String, Object>`.
- **Throws:** `JsonParseException` — if the JSON is syntactically invalid.

### `parseMap(String, Class<T>)`

```java
public <T> Map<String, T> parseMap(String json, Class<T> valueType)
```

Deserializes a JSON object to a `Map<String, T>` where each value is deserialized to the given type.

- **Parameters:**
  - `json` — the JSON object string.
  - `valueType` — the value type class.
- **Returns:** `Map<String, T>`.
- **Throws:**
  - `JsonParseException` — if the JSON is syntactically invalid.
  - `JsonMappingException` — if a value cannot be mapped.

---

## `JsonBuilder`

Builder for creating `Json` instances with custom configuration.

```java
public final class JsonBuilder
```

All setter methods return `this` for chaining. Call `build()` to create the `Json` instance.

### `fieldNaming(FieldNaming)`

```java
public JsonBuilder fieldNaming(FieldNaming strategy)
```

Sets the naming strategy for mapping Java field names to JSON keys. Default: `FieldNaming.IDENTITY`.

### `includeNulls(boolean)`

```java
public JsonBuilder includeNulls(boolean value)
```

Whether to include `null` fields in serialized JSON output. Default: `true`.

### `failOnUnknownProperties(boolean)`

```java
public JsonBuilder failOnUnknownProperties(boolean value)
```

Whether to throw `JsonMappingException` when a JSON key has no matching field. Default: `false` (ignore).

### `failOnMissingRequiredFields(boolean)`

```java
public JsonBuilder failOnMissingRequiredFields(boolean value)
```

Whether to throw `JsonMappingException` when a `@JsonRequired` field is missing. Default: `false`. With the default, missing required fields keep normal Java defaults; strict required-field enforcement is opt-in.

### `failOnNullForPrimitives(boolean)`

```java
public JsonBuilder failOnNullForPrimitives(boolean value)
```

Whether to throw `JsonMappingException` when a `null` JSON value maps to a primitive field. Default: `false` (assign default value).

### `failOnDuplicateKeys(boolean)`

```java
public JsonBuilder failOnDuplicateKeys(boolean value)
```

Whether to throw `JsonParseException` when a JSON object contains duplicate keys. Default: `false` (last wins).

### `failOnCycles(boolean)`

```java
public JsonBuilder failOnCycles(boolean value)
```

Whether to detect and throw on circular references during serialization. Default: `true`.

### `maxDepth(int)`

```java
public JsonBuilder maxDepth(int value)
```

Maximum nesting depth for serialization and deserialization. Must be positive. Default: `128`.

### `prettyPrint(boolean)`

```java
public JsonBuilder prettyPrint(boolean value)
```

Whether to format JSON output with indentation (2 spaces). Default: `false`.

### `dateFormat(DateFormat)`

```java
public JsonBuilder dateFormat(DateFormat value)
```

Global date/time format strategy. Default: `DateFormat.ISO`.

### `zoneId(ZoneId)`

```java
public JsonBuilder zoneId(ZoneId value)
```

Timezone for `java.util.Date` and `java.util.Calendar` conversion. Default: `ZoneId.of("UTC")`.

### `enumMode(EnumMode)`

```java
public JsonBuilder enumMode(EnumMode value)
```

Enum serialization/deserialization mode. Default: `EnumMode.NAME`.

### `build()`

```java
public Json build()
```

Creates a new `Json` instance with the configured options. The returned `Json` is immutable and thread-safe for reads.

---

## `JsonConfig`

Immutable configuration snapshot.

```java
public final class JsonConfig
```

Obtained via `Json.config()`. All fields are read-only.

| Method | Return Type | Default |
|--------|-------------|---------|
| `fieldNaming()` | `FieldNaming` | `IDENTITY` |
| `includeNulls()` | `boolean` | `true` |
| `failOnUnknownProperties()` | `boolean` | `false` |
| `failOnMissingRequiredFields()` | `boolean` | `false` |
| `failOnNullForPrimitives()` | `boolean` | `false` |
| `failOnDuplicateKeys()` | `boolean` | `false` |
| `failOnCycles()` | `boolean` | `true` |
| `maxDepth()` | `int` | `128` |
| `prettyPrint()` | `boolean` | `false` |
| `dateFormat()` | `DateFormat` | `ISO` |
| `zoneId()` | `ZoneId` | `ZoneId.of("UTC")` |
| `enumMode()` | `EnumMode` | `NAME` |

---

## `JsonException`

Base exception for all KissJson errors.

```java
public class JsonException extends RuntimeException
```

- Extends `RuntimeException` (unchecked).
- Carries a descriptive message with context.

---

## `JsonParseException`

Thrown when JSON text is syntactically invalid.

```java
public final class JsonParseException extends JsonException
```

### `line()`

```java
public int line()
```

Returns the 1-based line number where the error occurred.

### `column()`

```java
public int column()
```

Returns the 1-based column number where the error occurred.

### `offset()`

```java
public int offset()
```

Returns the 0-based character offset where the error occurred.

---

## `JsonMappingException`

Thrown when JSON cannot be mapped to the target Java type.

```java
public final class JsonMappingException extends JsonException
```

### `jsonPath()`

```java
public String jsonPath()
```

Returns the JSON path to the error location, e.g. `$.user.address.zipCode`.

### `targetType()`

```java
public Class<?> targetType()
```

Returns the target Java class being deserialized.

### `fieldName()`

```java
public String fieldName()
```

Returns the Java field name where the error occurred, or `null` if not applicable.

### `expectedType()`

```java
public Class<?> expectedType()
```

Returns the expected Java type, or `null` if not applicable.

### `actualValue()`

```java
public Object actualValue()
```

Returns the actual JSON value that caused the error (truncated if too long), or `null` if not applicable.

---

## `FieldNaming`

Naming strategies for field-to-JSON-key mapping.

```java
public enum FieldNaming
```

| Value | Description | Example |
|-------|-------------|---------|
| `IDENTITY` | Use the Java field name as-is | `userName` → `userName` |
| `LOWER_CASE` | Lowercase the entire field name | `userName` → `username` |
| `UPPER_CASE` | Uppercase the entire field name | `userName` → `USERNAME` |
| `CAMEL_CASE` | Convert to camelCase | `user_name` → `userName` |
| `SNAKE_CASE` | Convert to snake_case | `userName` → `user_name` |
| `KEBAB_CASE` | Convert to kebab-case | `userName` → `user-name` |

---

## `DateFormat`

Date/time format strategies.

```java
public enum DateFormat
```

| Value | Description | Example |
|-------|-------------|---------|
| `ISO` | ISO-8601 string format (default) | `"2025-01-15T10:30:00"` |
| `EPOCH_MILLIS` | Unix epoch milliseconds for instant-like temporal types | `1736934600000` |
| `EPOCH_SECONDS` | Unix epoch seconds for instant-like temporal types | `1736934600` |

---

## `EnumMode`

Enum serialization/deserialization modes.

```java
public enum EnumMode
```

| Value | Description | Serialize | Deserialize |
|-------|-------------|-----------|-------------|
| `NAME` | Use `Enum.name()` | `Status.ACTIVE` → `"ACTIVE"` | `"ACTIVE"` → `Status.ACTIVE` |
| `TO_STRING` | Use `Enum.toString()` | Custom `toString()` | Match by `toString()` |

---

## Annotations

All annotations target **fields** and have **runtime retention**.

### `@JsonName`

```java
@io.github.arthurhoch.kissjson.JsonName("custom_name")
```

Specifies a custom JSON key name for a field. Takes precedence over naming strategies.

- **Applies to:** serialization and deserialization
- **Example:** `@JsonName("user_name") String userName` → JSON key is `"user_name"`

### `@JsonAliases`

```java
@io.github.arthurhoch.kissjson.JsonAliases({"alias1", "alias2"})
```

Specifies alternative JSON key names accepted during deserialization. Useful for backward compatibility.

- **Applies to:** deserialization only
- **Example:** `@JsonAliases({"userName", "user_name"}) String name` — JSON keys `"userName"` or `"user_name"` both map to `name`
- **Multiple matches:** First matching alias wins (order matters)

### `@JsonIgnore`

```java
@io.github.arthurhoch.kissjson.JsonIgnore
```

Excludes the field from both serialization and deserialization.

- **Applies to:** serialization and deserialization
- **Example:** `@JsonIgnore String internalId` — never appears in JSON

### `@JsonRequired`

```java
@io.github.arthurhoch.kissjson.JsonRequired
```

Marks a field as required during deserialization when `failOnMissingRequiredFields(true)` is configured. The JSON key must be present (value can be `null`).

- **Applies to:** deserialization only
- **Example:** `@JsonRequired String email` — JSON must contain `"email"` key
- **Error:** `JsonMappingException` if the key is missing and required-field enforcement is enabled

### `@JsonIncludeNull`

```java
@io.github.arthurhoch.kissjson.JsonIncludeNull
```

Always include this field in serialized output, even when its value is `null`. Overrides a global `includeNulls = false` setting.

- **Applies to:** serialization only
- **Example:** `@JsonIncludeNull String middleName` — always appears in JSON output

### `@JsonExcludeNull`

```java
@io.github.arthurhoch.kissjson.JsonExcludeNull
```

Exclude this field from serialized output when its value is `null`. Overrides a global `includeNulls = true` setting.

- **Applies to:** serialization only
- **Example:** `@JsonExcludeNull String nickname` — omitted from JSON when `null`

### `@JsonDateFormat`

```java
@io.github.arthurhoch.kissjson.JsonDateFormat("yyyy-MM-dd")
```

Specifies a custom date format pattern for a specific field. Overrides the global `dateFormat` setting.

- **Applies to:** serialization and deserialization
- **Parameter:** `value` — a `java.time.format.DateTimeFormatter` pattern string
- **Example:** `@JsonDateFormat("dd/MM/yyyy") LocalDate birthDate` → `"15/01/2025"`

---

## Thread Safety

| Class | Thread-safe? |
|-------|-------------|
| `Json` | Yes (immutable config, shared caches) |
| `JsonConfig` | Yes (immutable) |
| `JsonBuilder` | No (single-use, build once) |
| Annotations | Yes (metadata, no state) |
| Enums | Yes (immutable) |

---

*All API described above is the current v1 contract for `0.1.0-SNAPSHOT`.*
