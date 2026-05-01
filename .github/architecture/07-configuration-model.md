# 07 — Configuration Model

> **Status:** Current v1 contract for `0.1.0-SNAPSHOT`.

## Overview

KissJson uses a **build-once, immutable configuration model**. Configuration is created through `JsonBuilder`, produces a frozen `JsonConfig`, and is held by `Json`. Once built, configuration **cannot be modified**.

## Design Goals

- **Immutability** — No configuration changes after construction.
- **Thread safety** — `Json` instances are thread-safe for reads.
- **Explicit defaults** — Every option has a sensible default.
- **Builder pattern** — `JsonBuilder` is mutable during construction only.

## Configuration Classes

### `JsonConfig` (Immutable)

`JsonConfig` is the immutable configuration object. It is implemented as a **record or final class** with all fields being final. It is never constructed directly by users — always via `JsonBuilder.build()`.

#### Intended v1 Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `fieldNaming` | `FieldNaming` | `IDENTITY` | Field-to-JSON-name naming strategy |
| `includeNulls` | `boolean` | `true` | Include null-valued fields in serialized JSON |
| `failOnUnknownProperties` | `boolean` | `false` | Throw on unrecognized JSON properties during deserialization |
| `failOnMissingRequiredFields` | `boolean` | `false` | Throw when `@JsonRequired` fields are absent from JSON |
| `failOnNullForPrimitives` | `boolean` | `false` | Throw when null is encountered for a primitive field |
| `failOnDuplicateKeys` | `boolean` | `false` | Throw on duplicate keys in JSON objects (default: last wins) |
| `failOnCycles` | `boolean` | `true` | Throw on circular references during serialization |
| `maxDepth` | `int` | `128` | Maximum nesting depth for serialization and deserialization |
| `prettyPrint` | `boolean` | `false` | Format serialized JSON with indentation |
| `dateFormat` | `DateFormat` | `ISO` | Date/time serialization format |
| `zoneId` | `ZoneId` | `UTC` | Timezone for date/time operations |
| `enumMode` | `EnumMode` | `NAME` | How enums are serialized/deserialized (`NAME` or `TO_STRING`) |

#### Design Rationale for Defaults

- **`includeNulls = true`** — JSON typically includes null fields; users can opt out globally or per-field.
- **`failOnUnknownProperties = false`** — Most JSON has extra fields; ignoring them is the forgiving default.
- **`failOnMissingRequiredFields = false`** — Missing fields are allowed by default, including fields annotated with `@JsonRequired`.
- **`failOnNullForPrimitives = false`** — Defaults to setting primitive defaults (0, false, etc.) rather than throwing.
- **`failOnDuplicateKeys = false`** — Last-wins is the pragmatic default; strict mode is opt-in.
- **`failOnCycles = true`** — Fails by default to prevent infinite loops and stack overflows.
- **`maxDepth = 128`** — Generous limit that catches pathological input without affecting normal usage.
- **`prettyPrint = false`** — Compact output by default for performance.
- **`dateFormat = ISO`** — ISO-8601 is the universal standard.
- **`zoneId = UTC`** — Neutral default; users should set this to their application's timezone.
- **`enumMode = NAME`** — `Enum.name()` is the standard, predictable representation.

### `JsonBuilder` (Mutable During Construction)

`JsonBuilder` is a mutable builder that produces an immutable `JsonConfig` when `build()` is called.

#### Intended v1 API

```java
JsonBuilder fieldNaming(FieldNaming strategy)
JsonBuilder includeNulls(boolean include)
JsonBuilder failOnUnknownProperties(boolean fail)
JsonBuilder failOnMissingRequiredFields(boolean fail)
JsonBuilder failOnNullForPrimitives(boolean fail)
JsonBuilder failOnDuplicateKeys(boolean fail)
JsonBuilder failOnCycles(boolean fail)
JsonBuilder maxDepth(int depth)
JsonBuilder prettyPrint(boolean pretty)
JsonBuilder dateFormat(DateFormat format)
JsonBuilder zoneId(ZoneId zoneId)
JsonBuilder enumMode(EnumMode mode)
Json build()
```

Each setter returns `this` for method chaining. `build()` creates the `JsonConfig`, constructs a `Json` instance, and returns it.

### `Json` (Entry Point)

`Json` holds a `JsonConfig` instance. It is the primary user-facing class.

#### Intended v1 API

```java
// Factory methods
static Json create()
static JsonBuilder builder()

// Serialize
String stringify(Object value)

// Deserialize
<T> T parse(String json, Class<T> type)
<T> List<T> parseList(String json, Class<T> elementType)
Map<String, Object> parseMap(String json)
<T> Map<String, T> parseMap(String json, Class<T> valueType)

// Configuration access (read-only)
JsonConfig config()
```

## Construction Flow

```
Json.builder()
    .fieldNaming(FieldNaming.SNAKE_CASE)
    .includeNulls(false)
    .build()
    │
    ▼
JsonBuilder.build()
    ├── Creates immutable JsonConfig from builder state
    ├── Creates Json instance with JsonConfig
    └── Returns Json
```

1. User calls `Json.builder()` → new `JsonBuilder` with defaults.
2. User calls setter methods on `JsonBuilder` (chainable).
3. User calls `build()` → `JsonBuilder` freezes state into `JsonConfig`.
4. `Json` instance is created with the `JsonConfig`.
5. No further configuration changes are possible.

## Thread Safety

- **`Json` instances are thread-safe for reads.** All configuration fields are immutable.
- **`JsonConfig` is immutable.** Safe to share across threads.
- **`JsonBuilder` is NOT thread-safe.** It is intended for single-thread use during construction only.
- **Configuration does not change after `build()`.** There are no setters on `Json` or `JsonConfig`.

## Immutability Guarantees

- `JsonConfig` has no setters. All fields are final.
- `Json` does not expose any way to modify its `JsonConfig`.
- `JsonBuilder` state is captured at `build()` time — later changes to the builder do not affect previously built `Json` instances.
- Mutable configuration objects (e.g., `ZoneId`) are treated as effectively immutable by convention (they are immutable in the JDK).

## Relationship to Other Components

| Component | Relationship |
|---|---|
| `JsonParser` | Receives `JsonConfig` to control parsing behavior (maxDepth, failOnDuplicateKeys, failOnUnknownProperties) |
| `JsonWriter` | Receives `JsonConfig` to control serialization behavior (prettyPrint, includeNulls, dateFormat, enumMode) |
| `ObjectReader` | Receives `JsonConfig` to control mapping behavior (failOnUnknownProperties, failOnMissingRequiredFields, failOnNullForPrimitives) |
| `ObjectWriter` | Receives `JsonConfig` to control field serialization (fieldNaming, includeNulls, dateFormat) |
| `ClassModelCache` | Uses `FieldNaming` to precompute JSON field names |

## Per-Field Overrides

Annotations can override global configuration for individual fields:

- `@JsonIncludeNull` — Override global `includeNulls` for one field.
- `@JsonExcludeNull` — Override global `includeNulls` for one field.
- `@JsonDateFormat` — Override global `dateFormat` for one field.
- `@JsonName` — Override `fieldNaming` strategy for one field.
- `@JsonIgnore` — Exclude a field entirely.
- `@JsonRequired` — Mark a field for required-field enforcement when `failOnMissingRequiredFields` is true.

These annotations are resolved at `ClassModel` creation time (cached per class), not at serialization/deserialization time.
