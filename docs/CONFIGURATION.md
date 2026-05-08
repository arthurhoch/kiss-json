---
layout: default
---

# Configuration

> **Note:** This document describes the current v1 configuration options for `0.1.0`.

All configuration is done through the `JsonBuilder` fluent API:

```java
import io.github.arthurhoch.kissjson.Json;
import io.github.arthurhoch.kissjson.FieldNaming;
import io.github.arthurhoch.kissjson.DateFormat;
import io.github.arthurhoch.kissjson.EnumMode;

Json json = Json.builder()
    .fieldNaming(FieldNaming.SNAKE_CASE)
    .includeNulls(false)
    .failOnUnknownProperties(true)
    .maxDepth(64)
    .prettyPrint(true)
    .build();
```

Configuration is **immutable after `build()`**. The resulting `Json` instance is **thread-safe for reads**.

---

## Options Reference

### `fieldNaming`

| Property | Value |
|----------|-------|
| **Type** | `FieldNaming` |
| **Default** | `FieldNaming.IDENTITY` |

**Description:**

Naming strategy for converting Java field names to JSON keys and matching JSON keys to Java fields during deserialization.

**Effect on serialization:**

Java field names are transformed according to the strategy before writing as JSON keys. `@JsonName` annotations take precedence over this strategy.

| Strategy | `userName` becomes |
|----------|-------------------|
| `IDENTITY` | `userName` |
| `LOWER_CASE` | `username` |
| `UPPER_CASE` | `USERNAME` |
| `CAMEL_CASE` | `userName` |
| `SNAKE_CASE` | `user_name` |
| `KEBAB_CASE` | `user-name` |

**Effect on deserialization:**

JSON keys are matched against Java fields using the same strategy. Matching also considers `@JsonName` and `@JsonAliases`.

```java
Json json = Json.builder().fieldNaming(FieldNaming.SNAKE_CASE).build();

// Java field "firstName" matches JSON key "first_name"
```

---

### `includeNulls`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `true` |

**Description:**

Whether to include fields with `null` values in the serialized JSON output.

**Effect on serialization:**

- `true` — all fields are included, even if their value is `null`: `{"name":"Alice","email":null}`
- `false` — fields with `null` values are omitted: `{"name":"Alice"}`

Can be overridden per-field with `@JsonIncludeNull` and `@JsonExcludeNull`.

**Effect on deserialization:**

None. This option only affects serialization output.

```java
Json json = Json.builder().includeNulls(false).build();

User user = new User();
user.name = "Alice";
// email is null

String text = json.stringify(user);
// {"name":"Alice"}
```

---

### `failOnUnknownProperties`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `false` |

**Description:**

Whether to throw a `JsonMappingException` when a JSON object key has no matching Java field.

**Effect on serialization:**

None. This option only affects deserialization.

**Effect on deserialization:**

- `false` (default) — unknown JSON keys are silently ignored. This is the safe default because JSON often contains extra fields from newer API versions.
- `true` — a `JsonMappingException` is thrown for the first unknown key, including the key name and target class.

```java
Json json = Json.builder().failOnUnknownProperties(true).build();

String input = """
    {"name":"Alice","phone":"555-1234"}
    """;

// Throws JsonMappingException: Unknown property 'phone' at $ [target=User]
User user = json.parse(input, User.class);
```

---

### `failOnMissingRequiredFields`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `false` |

**Description:**

Whether to throw a `JsonMappingException` when a field annotated with `@JsonRequired` is missing from the JSON input.

**Effect on serialization:**

None. This option only affects deserialization.

**Effect on deserialization:**

- `false` (default) — missing `@JsonRequired` fields are allowed and keep normal Java defaults.
- `true` — fields annotated with `@JsonRequired` must be present in the JSON input.

This option only enforces fields that are annotated with `@JsonRequired`; it does not make every field required.

```java
public class User {
    @JsonRequired String email;
    String name;
}

Json json = Json.builder().failOnMissingRequiredFields(true).build();

// Throws JsonMappingException: Required field 'email' is missing
json.parse("{\"name\":\"Alice\"}", User.class);
```

---

### `failOnNullForPrimitives`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `false` |

**Description:**

Whether to throw a `JsonMappingException` when a `null` JSON value would be assigned to a primitive Java field (`int`, `long`, `boolean`, etc.).

**Effect on serialization:**

None. This option only affects deserialization.

**Effect on deserialization:**

- `false` (default) — `null` is converted to the primitive default value: `0` for numeric types, `false` for `boolean`, `'\0'` for `char`.
- `true` — a `JsonMappingException` is thrown with the field name, expected type, and actual value.

```java
public class Score {
    int value;
}

Json json = Json.builder().failOnNullForPrimitives(true).build();

// Throws JsonMappingException: Cannot assign null to primitive int at $.value
json.parse("{\"value\":null}", Score.class);
```

With the default (`false`):

```java
Json json = Json.create();
Score score = json.parse("{\"value\":null}", Score.class);
// score.value == 0 (default int value)
```

---

### `failOnDuplicateKeys`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `false` |

**Description:**

Whether to throw a `JsonParseException` when a JSON object contains duplicate keys.

**Effect on serialization:**

None. This option only affects deserialization/parsing.

**Effect on deserialization:**

- `false` (default) — when duplicate keys exist, the **last value wins**. This matches the behavior of most JSON parsers and the JSON specification's recommendation.
- `true` — a `JsonParseException` is thrown at the first duplicate key, including the key name, line, and column.

```java
Json json = Json.builder().failOnDuplicateKeys(true).build();

String input = """
    {"name":"Alice","name":"Bob"}
    """;

// Throws JsonParseException: Duplicate key 'name' at line 1, column 22
json.parse(input, User.class);
```

---

### `failOnCycles`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `true` |

**Description:**

Whether to detect circular references (object cycles) during serialization and throw a `JsonException`.

**Effect on serialization:**

- `true` (default) — an `IdentityHashMap` tracks visited objects. If a cycle is detected, a `JsonException` is thrown with the path to the cycle. This prevents `StackOverflowError`.
- `false` — no cycle detection. Circular references will cause a `StackOverflowError`. **Use with extreme caution.**

Cycle detection uses identity comparison (`==`), not `equals()`, so distinct objects with equal content are not flagged.

**Effect on deserialization:**

None. This option only affects serialization.

```java
public class Node {
    String value;
    Node next;
}

Node a = new Node();
a.value = "A";
Node b = new Node();
b.value = "B";
a.next = b;
b.next = a; // cycle

Json json = Json.create();
json.stringify(a);
// Throws JsonException: Cycle detected at $.next.next
```

---

### `maxDepth`

| Property | Value |
|----------|-------|
| **Type** | `int` |
| **Default** | `128` |

**Description:**

Maximum nesting depth for both serialization and deserialization. Prevents deeply nested structures from causing `StackOverflowError`.

**Effect on serialization:**

If the object graph exceeds `maxDepth` nesting levels, a `JsonException` is thrown.

**Effect on deserialization:**

If the JSON input exceeds `maxDepth` nesting levels, a `JsonException` is thrown.

```java
Json json = Json.builder().maxDepth(64).build();

// If JSON has more than 64 levels of nesting, parsing fails
```

---

### `prettyPrint`

| Property | Value |
|----------|-------|
| **Type** | `boolean` |
| **Default** | `false` |

**Description:**

Whether to format JSON output with indentation (2 spaces per level).

**Effect on serialization:**

- `false` (default) — compact JSON on a single line: `{"name":"Alice","age":30}`
- `true` — formatted JSON with 2-space indentation:

```json
{
  "name": "Alice",
  "age": 30
}
```

**Effect on deserialization:**

None. The parser handles both compact and pretty-printed JSON regardless of this setting.

```java
Json json = Json.builder().prettyPrint(true).build();
```

---

### `dateFormat`

| Property | Value |
|----------|-------|
| **Type** | `DateFormat` |
| **Default** | `DateFormat.ISO` |

**Description:**

Global strategy for serializing and deserializing date/time types.

**Effect on serialization:**

| Strategy | Output |
|----------|--------|
| `ISO` | ISO-8601 string: `"2025-01-15T10:30:00"` |
| `EPOCH_MILLIS` | Long epoch milliseconds for instant-like temporal types |
| `EPOCH_SECONDS` | Long epoch seconds for instant-like temporal types |

**Effect on deserialization:**

| Strategy | Accepts |
|----------|---------|
| `ISO` | ISO-8601 string |
| `EPOCH_MILLIS` | Number for instant-like temporal types |
| `EPOCH_SECONDS` | Number for instant-like temporal types |

Can be overridden per-field with `@JsonDateFormat("pattern")`.

Epoch formats apply to `Instant`, `OffsetDateTime`, `ZonedDateTime`, `Date`, and `Calendar`. `LocalDate`, `LocalTime`, `LocalDateTime`, `Duration`, and `Period` remain ISO because converting them to an epoch loses local semantic information.

```java
Json json = Json.builder().dateFormat(DateFormat.EPOCH_MILLIS).build();

Event event = new Event();
event.timestamp = Instant.parse("2025-01-15T10:30:00Z");

String text = json.stringify(event);
// {"timestamp":1736934600000}
```

---

### `zoneId`

| Property | Value |
|----------|-------|
| **Type** | `java.time.ZoneId` |
| **Default** | `ZoneId.of("UTC")` |

**Description:**

Timezone used for converting `java.util.Date` and `java.util.Calendar` to/from `java.time` types internally.

**Effect on serialization:**

`Date` and `Calendar` values are converted to the specified timezone for ISO-8601 output.

**Effect on deserialization:**

`Date` and `Calendar` values are constructed using the specified timezone.

This option does not affect `java.time` types (they have their own timezone/offset information).

```java
import java.time.ZoneId;

Json json = Json.builder()
    .zoneId(ZoneId.of("America/New_York"))
    .build();
```

---

### `enumMode`

| Property | Value |
|----------|-------|
| **Type** | `EnumMode` |
| **Default** | `EnumMode.NAME` |

**Description:**

Strategy for serializing and deserializing enum values.

**Effect on serialization:**

| Mode | Output |
|------|--------|
| `NAME` | `Enum.name()` — the declared enum constant name: `"ACTIVE"` |
| `TO_STRING` | `Enum.toString()` — the custom string representation |

**Effect on deserialization:**

| Mode | Matching |
|------|----------|
| `NAME` | Match by `Enum.name()` (case-sensitive) |
| `TO_STRING` | Match by `Enum.toString()` (case-sensitive) |

If no enum constant matches, a `JsonMappingException` is thrown with the invalid value and the enum type.

```java
Json json = Json.builder().enumMode(EnumMode.NAME).build();

// Serialization: Status.ACTIVE -> "ACTIVE"
// Deserialization: "ACTIVE" -> Status.ACTIVE
```

---

## Configuration Summary

| Option | Type | Default | Serialization | Deserialization |
|--------|------|---------|---------------|-----------------|
| `fieldNaming` | `FieldNaming` | `IDENTITY` | Transforms field names | Matches JSON keys |
| `includeNulls` | `boolean` | `true` | Include/exclude null fields | — |
| `failOnUnknownProperties` | `boolean` | `false` | — | Throw on unknown keys |
| `failOnMissingRequiredFields` | `boolean` | `false` | — | Throw on missing required |
| `failOnNullForPrimitives` | `boolean` | `false` | — | Throw on null → primitive |
| `failOnDuplicateKeys` | `boolean` | `false` | — | Throw on duplicate keys |
| `failOnCycles` | `boolean` | `true` | Detect cycles | — |
| `maxDepth` | `int` | `128` | Limit nesting depth | Limit nesting depth |
| `prettyPrint` | `boolean` | `false` | Format with indentation | — |
| `dateFormat` | `DateFormat` | `ISO` | Date format strategy | Date format strategy |
| `zoneId` | `ZoneId` | `UTC` | Timezone for Date/Calendar | Timezone for Date/Calendar |
| `enumMode` | `EnumMode` | `NAME` | Enum representation | Enum matching |

---

## Thread Safety and Immutability

- `JsonBuilder` is **not thread-safe**. It is designed for single-use: configure and call `build()`.
- `JsonConfig` is **immutable**. All getter methods return the same values for the lifetime of the instance.
- `Json` is **thread-safe for reads**. Multiple threads can safely call `stringify()`, `parse()`, `parseList()`, and `parseMap()` concurrently on the same instance.
- Internal caches (`ConcurrentHashMap`) are shared across `Json` instances with the same configuration.

```java
// Safe: build once, share across threads
Json json = Json.builder()
    .fieldNaming(FieldNaming.SNAKE_CASE)
    .includeNulls(false)
    .build();

// Multiple threads can use json concurrently
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> json.stringify(user));   // thread-safe
executor.submit(() -> json.parse(text, User.class)); // thread-safe
```

---

## Annotation Overrides

Annotations override global configuration at the field level:

| Annotation | Overrides |
|------------|-----------|
| `@JsonName` | `fieldNaming` — always uses the specified name |
| `@JsonAliases` | Adds extra key matches during deserialization |
| `@JsonIgnore` | Always excludes regardless of other settings |
| `@JsonRequired` | Marks a field for required-field enforcement when `failOnMissingRequiredFields` is true |
| `@JsonIncludeNull` | `includeNulls = false` — includes this field even when null |
| `@JsonExcludeNull` | `includeNulls = true` — excludes this field when null |
| `@JsonDateFormat` | `dateFormat` — uses the specified pattern for this field |

---

*All configuration options described above are the current v1 contract for `0.1.0`.*
