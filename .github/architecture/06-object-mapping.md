# 06 — Object Mapping

This document defines the object mapping rules for KissJson. These rules govern how Java objects are converted to JSON and how JSON is converted to Java objects. All rules described here are the **current v1 contract**.

---

## Field Selection

### Which Fields Are Mapped

A field is eligible for mapping if **all** of the following are true:

1. It is declared in the target class or any superclass (excluding `Object`).
2. It is not `static`.
3. It is not `transient`.
4. It is not `synthetic` (compiler-generated).
5. It is not annotated with `@JsonIgnore`.

### Superclass Fields

Fields from all superclasses (up to but not including `java.lang.Object`) are included. If a subclass and superclass have fields with the same name, the subclass field takes precedence.

### Field Order

Fields are serialized and deserialized in the following order:

1. Fields from the highest superclass first, then working down to the declaring class.
2. Within a single class, fields are ordered by their declaration order in the source code (as reported by `Class.getDeclaredFields()`).

This ordering is deterministic and consistent between serialization and deserialization.

### Access

- Private fields are accessed via `Field.setAccessible(true)`.
- If `setAccessible(true)` fails (e.g., due to a SecurityManager or module access restrictions), a `JsonMappingException` is thrown with:
  - The class name.
  - The field name.
  - The `InaccessibleObjectException` or `SecurityException` as the cause.

---

## Constructor Requirement

### Rule

The target class must have a **no-argument constructor**. The constructor can have any access modifier (`public`, `protected`, package-private, `private`).

### Enforcement

- During `ClassModel` creation, the cache checks for a no-arg constructor via `Class.getDeclaredConstructor()`.
- If no no-arg constructor exists, a `JsonMappingException` is thrown with:
  - The class name.
  - Message: "Class <ClassName> requires a no-argument constructor."
- The constructor is made accessible via `setAccessible(true)` and cached for reuse.

### Records

Java records are **not required** in v1. If a record is passed as a target type, the standard field-mapping rules apply to its component fields. However, records do not have a no-arg constructor, so deserialization into records will throw `JsonMappingException` unless special handling is added. This is a known v1 limitation.

---

## Field Name Resolution

### Serialization

The JSON key for each field during serialization is determined by:

1. If the field has `@JsonName("customName")`, use `"customName"`.
2. Otherwise, apply the `FieldNaming` strategy to the Java field name.

**Example:**

```java
// FieldNaming = SNAKE_CASE
private String userName;  // JSON key: "user_name"

@JsonName("login")
private String userName;  // JSON key: "login"
```

Only the primary name is used during serialization. Aliases are ignored.

### Deserialization

During deserialization, the reader looks for the JSON value by checking names in this order:

1. Primary name (from `@JsonName` or naming strategy).
2. If not found, check each alias from `@JsonAliases` in declaration order.
3. If none match, the field is considered "missing."

**Example:**

```java
@JsonAliases({"login", "handle"})
private String username;
```

JSON `{"username": "art"}` → matched by primary name.
JSON `{"login": "art"}` → matched by alias "login".
JSON `{"handle": "art"}` → matched by alias "handle".
JSON `{"user_name": "art"}` → no match, field is missing.

When a field is matched by an alias, the value is still written to the Java field named `username`.

---

## Required Fields

### Rule

A field annotated with `@JsonRequired` is checked after deserialization. If the JSON did not contain a value for this field (by primary name or alias), and `failOnMissingRequiredFields` is `true`, a `JsonMappingException` is thrown.

### Behavior Matrix

| Field has `@JsonRequired` | `failOnMissingRequiredFields` | JSON missing field | Result |
|---------------------------|-------------------------------|--------------------|--------|
| Yes | `true` | Yes | `JsonMappingException`: "Required field 'fieldName' is missing from JSON" |
| Yes | `false` | Yes | Field keeps Java default value (no error) |
| No | any | Yes | Field keeps Java default value (no error) |
| Yes | any | No (value is null) | Field is set to null (required does not mean non-null) |

**Important:** `@JsonRequired` checks for the *presence* of the key in JSON, not whether the value is non-null. A field with JSON value `null` satisfies `@JsonRequired`.

---

## Unknown Properties

### Default Behavior

JSON keys that do not correspond to any mapped field (by primary name or alias) are silently ignored during deserialization. The deserialized object does not contain these values, and no error is thrown.

### Strict Behavior

If `failOnUnknownProperties` is `true` and the JSON contains a key that does not match any field, a `JsonMappingException` is thrown with:

- The unknown JSON key name.
- The JSON path.
- The target class name.
- Message: "Unknown property 'unknownKey' in JSON for class com.example.User"

---

## Null Handling

### Serialization: Null Fields

Whether null fields are included in the JSON output is determined by the interaction of global config and per-field annotations:

| `includeNulls` | `@JsonIncludeNull` | `@JsonExcludeNull` | Null field output |
|----------------|---------------------|---------------------|-------------------|
| `true` | absent | absent | Included: `"field": null` |
| `true` | present | absent | Included: `"field": null` |
| `true` | absent | present | Excluded (field omitted) |
| `true` | present | present | `JsonMappingException` (conflict) |
| `false` | absent | absent | Excluded (field omitted) |
| `false` | present | absent | Included: `"field": null` |
| `false` | absent | present | Excluded (field omitted) |
| `false` | present | present | `JsonMappingException` (conflict) |

If both `@JsonIncludeNull` and `@JsonExcludeNull` are present on the same field, a `JsonMappingException` is thrown during `ClassModel` creation (at cache time), not during serialization. The error message includes the class name and field name.

### Deserialization: Null JSON Values

When a JSON value is `null`:

| Java field type | `failOnNullForPrimitives` | Result |
|-----------------|---------------------------|--------|
| Object (String, Integer, etc.) | any | Field set to `null` |
| Primitive (`int`, `long`, etc.) | `false` | Field keeps Java default (0, false, '\0') |
| Primitive (`int`, `long`, etc.) | `true` | `JsonMappingException`: "Null value for primitive field 'fieldName' in class ClassName" |

### Deserialization: Missing Fields

When a JSON key is not present for a field:

- The field keeps its Java default value (whatever the no-arg constructor set it to).
- This applies regardless of `failOnNullForPrimitives` (that config only applies to explicit `null` values).
- To enforce presence, use `@JsonRequired` + `failOnMissingRequiredFields`.

---

## Supported Types

### Primitives and Wrappers

| Java Type | JSON Type | Notes |
|-----------|-----------|-------|
| `int` / `Integer` | Number (integer) | Precision loss for values outside int range throws `JsonMappingException`. |
| `long` / `Long` | Number (integer) | Full 64-bit range. |
| `double` / `Double` | Number (decimal) | Special values (NaN, Infinity) throw `JsonException`. |
| `float` / `Float` | Number (decimal) | Promoted to double. Special values throw. |
| `boolean` / `Boolean` | Boolean | `true` or `false`. |
| `short` / `Short` | Number (integer) | Range check. |
| `byte` / `Byte` | Number (integer) | Range check. |
| `char` / `Character` | String (single char) | Serialized as single-character string. Deserialized from single-character string. |
| `String` | String | Null preserved. |
| `BigDecimal` | Number | Full precision. No loss. |
| `BigInteger` | Number (integer) | Full precision. No loss. |

### Enums

Serialized and deserialized according to `EnumMode`:

| Mode | Serialization | Deserialization |
|------|---------------|-----------------|
| `NAME` | `enum.name()` → string | Match by `name()`, case-sensitive. Unmatched throws `JsonMappingException`. |
| `TO_STRING` | `enum.toString()` → string | Match by `toString()`, case-sensitive. Unmatched throws `JsonMappingException`. |

Null enum values are preserved.

### Arrays

| Java Type | JSON Type | Notes |
|-----------|-----------|-------|
| `int[]` | Array of numbers | Each element mapped as `int`. |
| `long[]` | Array of numbers | Each element mapped as `long`. |
| `double[]` | Array of numbers | Each element mapped as `double`. |
| `boolean[]` | Array of booleans | Each element mapped as `boolean`. |
| `char[]` | Array of strings | Each element mapped as single-char string. |
| `String[]` | Array of strings | Null elements preserved. |
| `Object[]` | Array | Each element dispatched by runtime type. |
| `T[]` (any supported type) | Array | Each element mapped as `T`. |

Null arrays are serialized as `null`. Null elements within arrays are serialized as `null`.

### List

`List<T>` is serialized as a JSON array and deserialized as an `ArrayList<T>`.

- `T` can be any supported type.
- `List` is always deserialized as `ArrayList` (concrete type).
- Null elements within the list are preserved.
- A null `List` field is serialized as `null` (or omitted per null handling rules).

Generic type information is extracted from the field's `ParameterizedType` in `FieldModel`.

### Map

`Map<String, T>` is serialized as a JSON object and deserialized as a `LinkedHashMap<String, T>`.

- Keys must be `String`. Non-string keys throw `JsonException` during serialization.
- `T` can be any supported type.
- `Map` is always deserialized as `LinkedHashMap` (preserves insertion order).
- Null values within the map are preserved.
- A null `Map` field is serialized as `null` (or omitted per null handling rules).

### Nested Objects

Any object whose class is not one of the above special types is treated as a nested object:

- Serialized as a JSON object with its own fields.
- Deserialized by recursively applying the same mapping rules.
- Must have a no-arg constructor.
- Cycle detection applies (see below).

### Date/Time Types

Handled by `DateCodec` (see internal architecture). Serialized as ISO strings or epoch numbers based on `DateFormat` config.

| Java Type | ISO Example |
|-----------|-------------|
| `LocalDate` | `"2025-01-15"` |
| `LocalTime` | `"10:30:00"` |
| `LocalDateTime` | `"2025-01-15T10:30:00"` |
| `OffsetDateTime` | `"2025-01-15T10:30:00+01:00"` |
| `ZonedDateTime` | `"2025-01-15T10:30:00+01:00[Europe/Paris]"` |
| `Instant` | `"2025-01-15T09:30:00Z"` |
| `Duration` | `"PT1H30M"` |
| `Period` | `"P1Y2M3D"` |
| `Date` | `"2025-01-15T09:30:00Z"` |
| `Calendar` | `"2025-01-15T10:30:00+01:00"` |

### Unsupported Types

If a field's type is not recognized (not a primitive, wrapper, String, BigDecimal, BigInteger, enum, array, List, Map, date/time, or nested object):

- **Serialization:** Throw `JsonException` with the class name, field name, and field type.
- **Deserialization:** Throw `JsonMappingException` with the JSON path, target type, and expected type.

---

## Cycle Detection

### Serialization

During serialization, an `IdentityHashMap<Object, Object>` tracks all objects currently being serialized:

1. Before serializing an object, check if it is already in the map.
2. If yes: this is a cycle. If `failOnCycles` is `true` (the default), throw `JsonMappingException` with:
   - The JSON path where the cycle was detected.
   - The class of the cyclic object.
   - Message: "Reference cycle detected for class ClassName at path $.user.address.owner"
3. If no: add the object to the map, serialize it, then remove it from the map.

### Deserialization

Cycle detection during deserialization is not applicable — JSON is a tree structure and cannot contain cycles. Cycles can only occur in the Java object graph during serialization.

---

## Max Depth

### Serialization

The writer tracks the current nesting depth:

- Entering an object or array: depth + 1.
- Leaving an object or array: depth - 1.
- After entering: if depth exceeds `maxDepth`, throw `JsonMappingException` with the current path and max depth.

### Deserialization

The parser enforces `maxDepth` during parsing (see [04-json-parser.md](04-json-parser.md)). After parsing, the `ObjectReader` does not apply additional depth limiting — the parser has already enforced it.

### Configuration

- `maxDepth` default: `128`.
- A value below `1` is rejected by `JsonBuilder.maxDepth(int)`.

---

## Type Conversion

### Numeric Coercion (Deserialization)

When converting a JSON number to a Java numeric field:

| Target Type | Source JSON Number | Behavior |
|-------------|---------------------|----------|
| `int` | Integer, fits in int range | Cast to int |
| `int` | Integer, exceeds int range | `JsonMappingException` (overflow) |
| `int` | Decimal | `JsonMappingException` (loss of precision) |
| `long` | Integer, fits in long range | Cast to long |
| `long` | Integer, exceeds long range | `JsonMappingException` (overflow) |
| `long` | Decimal | `JsonMappingException` (loss of precision) |
| `double` | Any number | Convert via `BigDecimal.doubleValue()` |
| `float` | Any number | Convert via `BigDecimal.floatValue()` |
| `short` | Integer, fits in short range | Cast to short |
| `short` | Integer, exceeds short range | `JsonMappingException` (overflow) |
| `byte` | Integer, fits in byte range | Cast to byte |
| `byte` | Integer, exceeds byte range | `JsonMappingException` (overflow) |
| `BigDecimal` | Any number | Direct from parser |
| `BigInteger` | Integer | Direct from parser |
| `BigInteger` | Decimal | `JsonMappingException` (loss of precision) |

### String to Other Types (Deserialization)

| Target Type | Source JSON String | Behavior |
|-------------|---------------------|----------|
| `char` / `Character` | Single-character string | Extract char at index 0 |
| `char` / `Character` | Multi-character string | `JsonMappingException` |
| `String` | String | Direct |
| Enum | String (matches name/toString) | Return enum constant |
| Enum | String (no match) | `JsonMappingException` |
| Any other type | String | `JsonMappingException` |

### Other Conversions

| Target Type | Source JSON | Behavior |
|-------------|-------------|----------|
| `boolean` / `Boolean` | Boolean | Direct |
| Any type | `null` | Set to null (or default for primitives) |
| Wrong type | Any | `JsonMappingException` with expected vs. actual |

---

## Duplicate Keys

### Serialization

If a Java object has two fields that resolve to the same JSON key (e.g., two fields with `@JsonName("name")`), the behavior is undefined. The last field in declaration order wins. This is not validated — it is the user's responsibility to avoid key collisions.

### Deserialization

Duplicate key behavior is controlled by the parser (see [04-json-parser.md](04-json-parser.md)):

- Default: last value wins.
- `failOnDuplicateKeys(true)`: `JsonParseException` on duplicate keys.

---

## Mapping Examples

### Basic Object

```java
public class User {
    private String name;
    private int age;
    private boolean active;
}
```

JSON:
```json
{"name":"Arthur","age":30,"active":true}
```

### With Annotations

```java
public class User {
    @JsonName("full_name")
    @JsonAliases({"fullName", "displayName"})
    private String name;

    @JsonIgnore
    private String password;

    @JsonRequired
    private String id;

    @JsonDateFormat("yyyy-MM-dd")
    private LocalDate createdAt;
}
```

JSON (deserialization):
```json
{"full_name":"Arthur","id":"123","createdAt":1736934600000,"password":"secret"}
```

- `name` matched by primary name `"full_name"`.
- `password` ignored (`@JsonIgnore`).
- `id` present (`@JsonRequired` satisfied).
- `createdAt` parsed as epoch millis.
- `"password": "secret"` is unknown property → silently ignored.

### Nested Object

```java
public class Order {
    private String id;
    private User customer;
    private List<Item> items;
}

public class Item {
    private String product;
    private double price;
}
```

JSON:
```json
{
  "id": "ORD-001",
  "customer": {"name": "Arthur", "age": 30, "active": true},
  "items": [
    {"product": "Widget", "price": 9.99},
    {"product": "Gadget", "price": 24.95}
  ]
}
```

---

## Do Not Use Getters or Setters

This rule is absolute and non-negotiable:

- **Serialization:** Read field values via `Field.get(object)`. Never invoke getter methods.
- **Deserialization:** Write field values via `Field.set(object, value)`. Never invoke setter methods.
- **Rationale:** Getters and setters may contain side effects, validation logic, or transformations that do not correspond to the actual data. Fields are the source of truth.

---

## Status

This document describes the **current v1 contract** for object mapping in `0.1.0`.
