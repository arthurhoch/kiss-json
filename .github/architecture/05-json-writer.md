# 05 — JSON Writer

This document describes the design of the KissJson JSON writer. This is an **internal component** — **not public API**. Users never interact with the writer directly.

---

## Overview

The writer is a **direct `StringBuilder`-based JSON output generator**. It writes JSON text character by character into a `StringBuilder`, with proper escaping, formatting, and structure.

The writer receives calls from `ObjectWriter` during object serialization, writing field names and values without building an intermediate `JsonValue` tree.

---

## Output Modes

### Compact Mode (Default)

All JSON is written on a single line with no unnecessary whitespace:

```json
{"name":"Arthur","age":30,"active":true}
```

- No spaces after `:`, `,`, `{`, `[`, `}`, `]`.
- No newlines.
- This is the default and most common output mode.

### Pretty-Print Mode

When `prettyPrint` is `true`, output is indented with the configured indent string:

```json
{
  "name": "Arthur",
  "age": 30,
  "active": true
}
```

Rules:
- Newline after `{` and `[`.
- Each member/element indented one level.
- Newline after each `,` (except the last element).
- `}` and `]` on their own line, at the parent indentation level.
- A single space after `:` in object members.
- Indentation is fixed at two spaces when `JsonConfig.prettyPrint()` is enabled.

---

## String Escaping

All strings (keys and values) are enclosed in double quotes and escaped according to RFC 8259.

### Required Escapes

| Character | Escape | Condition |
|-----------|--------|-----------|
| `"` (U+0022) | `\"` | Always |
| `\` (U+005C) | `\\` | Always |
| Control chars (U+0000–U+001F) | `\uXXXX` | Always |

### Named Escapes

For readability, the following control characters use named escapes instead of `\uXXXX`:

| Character | Escape |
|-----------|--------|
| `\b` (U+0008) | `\b` |
| `\f` (U+000C) | `\f` |
| `\n` (U+000A) | `\n` |
| `\r` (U+000D) | `\r` |
| `\t` (U+0009) | `\t` |

### Forward Slash

The forward slash `/` (U+002F) is **not** escaped by default. JSON does not require it. If a user needs `\/` escaping, they can post-process the output.

### Non-ASCII Characters

Non-ASCII characters (code points above U+001F) are written as-is (not escaped to `\uXXXX`). This means:

- UTF-8 characters like `é`, `日`, `🎉` are preserved in the output.
- The output is valid UTF-8 JSON.
- Surrogate pairs are written as-is (two UTF-16 code units in the `StringBuilder`).

This behavior ensures human-readable output for international text while remaining fully JSON-compliant.

---

## Null Handling

### Root null

If the root value being serialized is `null`, the writer outputs the literal string `null`.

### Null fields

Field-level null handling is controlled by the interaction of `JsonConfig.includeNulls()` and per-field annotations:

| `includeNulls` | `@JsonIncludeNull` | `@JsonExcludeNull` | Null field output |
|----------------|---------------------|---------------------|-------------------|
| `true` | absent | absent | `"field": null` |
| `true` | present | absent | `"field": null` |
| `true` | absent | present | Field omitted |
| `true` | present | present | **Error:** `JsonMappingException` (conflict) |
| `false` | absent | absent | Field omitted |
| `false` | present | absent | `"field": null` |
| `false` | absent | present | Field omitted |
| `false` | present | present | **Error:** `JsonMappingException` (conflict) |

### Null in collections

- `null` elements in `List`: output `null` (e.g., `[1, null, 3]`).
- `null` values in `Map`: output `null` (e.g., `{"key": null}`).
- These are not affected by `includeNulls` (which controls object fields only).

---

## Number Formatting

### Integer (`int`, `long`)

- Output as decimal integer with no decimal point.
- Negative values preceded by `-`.
- `0` is output as `0`.
- No leading zeros.
- `int` and `long` are formatted identically. No type distinction in JSON.

**Examples:** `42`, `-7`, `0`, `9223372036854775807`

### Floating Point (`double`)

- Output using `Double.toString()` or equivalent, ensuring valid JSON number format.
- Special values are handled as follows:
  - `Double.NaN` → throw `JsonException` (not valid JSON).
  - `Double.POSITIVE_INFINITY` → throw `JsonException` (not valid JSON).
  - `Double.NEGATIVE_INFINITY` → throw `JsonException` (not valid JSON).
- `Float.NaN`, `Float.POSITIVE_INFINITY`, `Float.NEGATIVE_INFINITY` → same behavior as `Double`.

**Examples:** `3.14`, `-0.5`, `1.0`, `1.2345678901234567E20`

### BigDecimal

- Output using `BigDecimal.toPlainString()` to avoid scientific notation where possible.
- If the value has a scale that produces a very long string, scientific notation is used.
- No loss of precision.

**Examples:** `123456789.123456789`, `0.001`, `1000000`

### BigInteger

- Output as decimal integer, same as `long` but without size limit.
- No loss of precision.

**Examples:** `123456789012345678901234567890`

---

## Boolean Formatting

- `true` → literal `true`.
- `false` → literal `false`.

---

## Date/Time Formatting

Date/time formatting is determined by `JsonConfig.dateFormat()` or per-field `@JsonDateFormat`.

### ISO Format (Default)

| Java Type | Output Format | Example |
|-----------|---------------|---------|
| `LocalDate` | `yyyy-MM-dd` | `"2025-01-15"` |
| `LocalTime` | `HH:mm:ss` (with fraction if non-zero) | `"10:30:00"` or `"10:30:00.123"` |
| `LocalDateTime` | `yyyy-MM-dd'T'HH:mm:ss` (with fraction if non-zero) | `"2025-01-15T10:30:00"` |
| `OffsetDateTime` | ISO-8601 with offset | `"2025-01-15T10:30:00+01:00"` |
| `ZonedDateTime` | ISO-8601 with zone ID | `"2025-01-15T10:30:00+01:00[Europe/Paris]"` |
| `Instant` | ISO-8601 UTC | `"2025-01-15T09:30:00Z"` |
| `Duration` | ISO-8601 duration | `"PT1H30M"` |
| `Period` | ISO-8601 period | `"P1Y2M3D"` |
| `Date` | ISO-8601 UTC | `"2025-01-15T09:30:00Z"` |
| `Calendar` | ISO-8601 with offset/timezone | `"2025-01-15T10:30:00+01:00"` |

All ISO date/time values are serialized as JSON strings (enclosed in `"`).

### Epoch Millis

All date/time types that represent an instant on the timeline are serialized as a JSON number (long) representing milliseconds since `1970-01-01T00:00:00Z`.

`LocalDate`, `LocalTime`, `LocalDateTime`, `Duration`, and `Period` remain ISO strings when `EPOCH_MILLIS` is configured because converting them to an epoch value would require assumptions that are outside the v1 KISS contract.

### Epoch Seconds

Same as `EPOCH_MILLIS` but in seconds.

---

## Enum Formatting

Enum formatting is determined by `JsonConfig.enumMode()`:

| Mode | Serialization | Example |
|------|---------------|---------|
| `NAME` | `enumValue.name()` | `"ACTIVE_STATUS"` |
| `TO_STRING` | `enumValue.toString()` | `"active"` |

Enum values are always serialized as JSON strings.

---

## Array Formatting

- Arrays are enclosed in `[` and `]`.
- Elements are separated by `,`.
- Pretty-print: newline + indent after `[` and after each `,`.
- Null elements are output as `null`.

Supported array types:
- `int[]`, `long[]`, `double[]`, `boolean[]`, `char[]`
- `String[]`, `Object[]`
- Any array of supported types

---

## Collection Formatting

- `List<?>` is formatted as a JSON array.
- `Set<?>` is formatted as a JSON array (iteration order).
- `Map<String, ?>` is formatted as a JSON object.
- `Map` keys must be `String`. Non-string keys throw `JsonException`.

---

## Streaming Mode (Normal Serialization Path)

When `ObjectWriter` serializes a Java object, it calls the writer directly without building an intermediate `JsonValue` tree:

```
ObjectWriter:
  writer.beginObject()
  for each field:
    writer.name("fieldName")
    writer.value(fieldValue)     // dispatches by type
  writer.endObject()
```

This avoids the overhead of creating `JsonValue` nodes for the common case of serializing a Java object to JSON.

### Writer Methods (Internal API)

| Method | Description |
|--------|-------------|
| `beginObject()` | Write `{` (with newline + indent if pretty-print). |
| `endObject()` | Write `}` (with newline + dedent if pretty-print). |
| `beginArray()` | Write `[` (with newline + indent if pretty-print). |
| `endArray()` | Write `]` (with newline + dedent if pretty-print). |
| `name(String)` | Write the field name (escaped string) followed by `:`. |
| `value(String)` | Write a string value (escaped). |
| `value(long)` | Write a long value. |
| `value(double)` | Write a double value. |
| `value(BigDecimal)` | Write a BigDecimal value. |
| `value(boolean)` | Write `true` or `false`. |
| `valueNull()` | Write `null`. |
| `value(Object)` | Dispatch by type: delegate to the appropriate `value()` method or recurse via `ObjectWriter`. |
| `separator()` | Write `,` if needed (tracks whether a separator is needed before the next element). |
| `toString()` | Return the built JSON string. |

---

## Pretty-Print Implementation

The writer tracks an `indentLevel` counter:

- `beginObject()` and `beginArray()`: increment level, write newline + indent.
- `endObject()` and `endArray()`: decrement level, write newline + indent.
- `name()`: write newline + indent before the name (if not first element).
- `separator()`: write `,` followed by newline + indent.

The indent string is repeated `indentLevel` times. Default: `"  "` (two spaces).

---

## Implementation Constraints

1. **Single `StringBuilder`.** The writer uses one `StringBuilder` for the entire output. No intermediate buffers.
2. **No external library.** Only `java.*` imports.
3. **No `javax.json` or `jakarta.json`.** No JSON-P dependency.
4. **Efficient escaping.** String escaping is done character-by-character in a tight loop. No regex.
5. **No unnecessary tree.** The normal serialization path does not create an intermediate `JsonValue` tree.
6. **Thread safety.** The writer is created per-serialization call. Not shared across threads.

---

## Status

This document describes the **current v1 contract** for the JSON writer. The writer is an internal component implemented for `0.1.0`.
