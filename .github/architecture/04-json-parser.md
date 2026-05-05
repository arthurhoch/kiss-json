# 04 — JSON Parser

This document describes the design of the KissJson JSON parser. This is an **internal component** — **not public API**. Users never interact with the parser directly.

---

## Overview

The parser is a **hand-written, token-based parser** that reads JSON text character by character from a `char[]` array. The normal deserialization path exposes internal tokens to `ObjectReader`, which maps directly to Java objects without building an intermediate `JsonValue` tree.

An internal `JsonValue` model may exist for limited generic representation, but it is not public API and must not be used by the typed fast path for `parse`, `parseList`, or typed `parseMap`.

---

## Input

- Accepts a `String` as input.
- Converts to `char[]` internally for direct indexing.
- Validates that the input is not `null`. Throws `JsonParseException` if `null` is passed.
- Validates that the input is not empty or all whitespace. Throws `JsonParseException` if no JSON value is found.

---

## State Tracking

The parser maintains the following state during parsing:

| State | Type | Description |
|-------|------|-------------|
| `chars` | `char[]` | The input character array. |
| `offset` | `int` | Current position in the array (0-based). |
| `length` | `int` | Total length of the input. |
| `line` | `int` | Current line number (1-based). Starts at 1. |
| `column` | `int` | Current column number (1-based). Starts at 1. Resets on `\n`. |
| `depth` | `int` | Current nesting depth. Starts at 0. |

Line and column are tracked by checking for `\n`, `\r\n`, and `\r` line endings. Both `\r\n` and `\r` are treated as line breaks.

---

## Parsing Rules

### Whitespace

The following characters are whitespace and are skipped: space (`0x20`), tab (`0x09`), newline (`0x0A`), carriage return (`0x0D`).

Whitespace is skipped before and after every JSON value, between tokens in objects and arrays, and after structural characters (`{`, `}`, `[`, `]`, `:`, `,`).

### JSON Value

A JSON value is one of:
- Object (`{ ... }`)
- Array (`[ ... ]`)
- String (`"..."`)
- Number
- `true`
- `false`
- `null`

The parser dispatches on the first non-whitespace character:

| First Char | Parsed As |
|------------|-----------|
| `{` | Object |
| `[` | Array |
| `"` | String |
| `-` or `0`–`9` | Number |
| `t` | Boolean `true` |
| `f` | Boolean `false` |
| `n` | Null |

Any other character results in a `JsonParseException` with the unexpected character, line, column, and offset.

### Object

```
object → '{' ws '}' | '{' ws member (',' ws member)* ','? ws '}'
member → ws string ws ':' ws value
```

- Empty objects (`{}`) are valid.
- Trailing commas are **not** allowed in standard JSON. The parser enforces this strictly.
- Each member key must be a string (in quotes).
- Keys are separated from values by `:`.
- Members are separated by `,`.
- The parser does **not** enforce key ordering. Keys are stored in insertion order via `LinkedHashMap`.

**Duplicate keys:**
- Default behavior: last value wins. Earlier values for the same key are silently overwritten.
- If `failOnDuplicateKeys` is `true`: on encountering a duplicate key, throw `JsonParseException` with the key name, line, column, and offset.

### Array

```
array → '[' ws ']' | '[' ws value (',' ws value)* ','? ws ']'
```

- Empty arrays (`[]`) are valid.
- Trailing commas are **not** allowed.
- Elements can be any JSON value (mixed types allowed).
- Elements are stored in an `ArrayList` preserving insertion order.

### String

```
string → '"' character* '"'
```

**Escape sequences** (after `\`):

| Escape | Character | Unicode | Description |
|--------|-----------|---------|-------------|
| `\"` | `"` | U+0022 | Quotation mark |
| `\\` | `\` | U+005C | Reverse solidus |
| `\/` | `/` | U+002F | Solidus (optional) |
| `\b` | backspace | U+0008 | Backspace |
| `\f` | form feed | U+000C | Form feed |
| `\n` | newline | U+000A | Line feed |
| `\r` | carriage return | U+000D | Carriage return |
| `\t` | tab | U+0009 | Character tabulation |
| `\uXXXX` | unicode | U+XXXX | 4-hex-digit unicode escape |

**Unicode escape handling:**
- `\u` followed by exactly 4 hex digits (case-insensitive).
- Invalid hex digits result in `JsonParseException`.
- Surrogate pairs: `\uD800\uDC00` through `\uDBFF\uDFFF` are decoded into the corresponding Unicode code point.
- Isolated surrogate halves (high without low, low without high) result in `JsonParseException`.

**String validation:**
- Unescaped control characters (U+0000 through U+001F) result in `JsonParseException`.
- Unterminated strings (EOF before closing `"`) result in `JsonParseException`.

### Number

```
number → '-'? integer ('.' digit+)? ('e'|'E' ('+'|'-')? digit+)?
integer → '0' | digit1-9 digit*
digit → '0'-'9'
```

**Parsing strategy:**
- The parser reads the entire number as a character range (from start offset to end offset).
- If the number has no fractional part and no exponent and fits in `long`: store as `long`.
- If the number has no fractional part and no exponent and does not fit in `long`: store as `BigDecimal`.
- If the number has a fractional part or exponent: store as `BigDecimal` if it cannot be exactly represented as `double`, otherwise store as `double`.
- The parser never loses precision during parsing. Integral values are kept as `long` when possible, and decimal or oversized values use `BigDecimal`.

**Validation:**
- Leading zeros are not allowed (except for `0` itself or `0.xxx`). `01` is invalid.
- `-0` and `-0.0` are valid.
- At least one digit must follow `.`.
- At least one digit must follow `e`/`E` and the optional sign.
- Empty exponent (e.g., `1e`, `1e+`) results in `JsonParseException`.

### Boolean

- `true`: match characters `t`, `r`, `u`, `e` exactly. Any mismatch results in `JsonParseException`.
- `false`: match characters `f`, `a`, `l`, `s`, `e` exactly. Any mismatch results in `JsonParseException`.

### Null

- `null`: match characters `n`, `u`, `l`, `l` exactly. Any mismatch results in `JsonParseException`.

---

## Depth Limiting

The parser enforces the `maxDepth` configuration:

- `depth` is incremented when entering an object (`{`) or array (`[`).
- `depth` is decremented when leaving an object (`}`) or array (`]`).
- Before incrementing, if `depth >= maxDepth`, throw `JsonParseException` with the current line, column, offset, and a message indicating the max depth was exceeded.
- If `maxDepth` is `0` or negative, depth checking is disabled (no limit).

---

## Error Handling

All parse errors throw `JsonParseException` with:

| Field | Description |
|-------|-------------|
| Message | Describes what went wrong. Example: "Unexpected character 'x' at line 3, column 15". |
| Line | 1-based line number where the error was detected. |
| Column | 1-based column number where the error was detected. |
| Offset | 0-based character offset where the error was detected. |

**Common error scenarios:**

| Scenario | Message Pattern |
|----------|-----------------|
| Unexpected character | "Unexpected character 'X' at line N, column N" |
| Unterminated string | "Unterminated string at line N, column N" |
| Invalid escape sequence | "Invalid escape sequence '\\x' at line N, column N" |
| Invalid unicode escape | "Invalid unicode escape '\\uXXXX' at line N, column N" |
| Trailing comma | "Trailing comma at line N, column N" |
| Missing colon | "Expected ':' at line N, column N" |
| Missing comma | "Expected ',' or '}' at line N, column N" |
| Duplicate key | "Duplicate key \"fieldName\" at line N, column N" |
| Max depth exceeded | "Maximum nesting depth (N) exceeded at line N, column N" |
| Empty input | "Empty input" |
| Premature EOF | "Unexpected end of input at line N, column N" |
| Invalid number | "Invalid number format at line N, column N" |

---

## Output

The primary parser output is an internal token stream consumed by `ObjectReader`:

| Token | Description |
|------|-------------|
| `OBJECT_START` | `{` |
| `OBJECT_END` | `}` |
| `ARRAY_START` | `[` |
| `ARRAY_END` | `]` |
| `COLON` | `:` |
| `COMMA` | `,` |
| `STRING` | String value |
| `NUMBER` | Number value |
| `BOOLEAN` | Boolean value |
| `NULL` | Null value |
| `END` | End of input |

If a `JsonValue` tree exists internally, it remains package-private and is limited to generic internal use:

| Type | Storage | Accessor |
|------|---------|----------|
| Object | `LinkedHashMap<String, JsonValue>` | `asObject()` |
| Array | `ArrayList<JsonValue>` | `asArray()` |
| String | `String` | `asString()` |
| Number (long) | `long` | `asLong()`, `asInt()`, `asDouble()`, `asBigDecimal()` |
| Number (double) | `double` | `asDouble()`, `asBigDecimal()` |
| Number (BigDecimal) | `BigDecimal` | `asBigDecimal()`, `asDouble()`, `asLong()` |
| Boolean | `boolean` | `asBoolean()` |
| Null | sentinel | `isNull()` |

Typed object deserialization consumes tokens directly and never exposes tokens or `JsonValue` nodes to users.

---

## Implementation Constraints

1. **No regex.** The parser must not use `java.util.regex` anywhere.
2. **No `String.split`.** The parser must not use `String.split` for tokenization.
3. **No external library.** Only `java.*` imports.
4. **Character-by-character.** The parser reads from `char[]` using index-based access.
5. **Single pass.** The input is parsed in one forward pass. No backtracking beyond peek-ahead.
6. **UTF-16 aware.** Java `char[]` is UTF-16 code units. The parser handles surrogate pairs in strings.
7. **Internal only.** Tokens, parser state, and any value model stay package-private.

---

## Performance Considerations

- **Avoid object allocation for number parsing.** Parse `long` values directly from characters without creating intermediate `String` objects where possible.
- **Reuse `StringBuilder` for strings.** Use a single `StringBuilder` instance during parsing for building string values (cleared and reused).
- **Size hints for collections.** When parsing objects and arrays, use the `LinkedHashMap` and `ArrayList` default constructors (no pre-sizing based on content — that would require a two-pass approach).
- **Minimize method call overhead.** The parser methods are called at high frequency. Keep them small and let the JIT compiler inline them.

---

## Status

This document describes the **current v1 contract** for the JSON parser. The parser is an internal component implemented for `0.1.0`.
