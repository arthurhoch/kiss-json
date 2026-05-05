# 08 — Error Handling

> **Status:** Current v1 contract for `0.1.0`.

## Overview

KissJson provides **rich, contextual error messages** that tell the user exactly what went wrong and where. Every exception includes the relevant context (line, column, JSON path, types, values) so users can diagnose problems without debugging into library internals.

## Design Principles

1. **Every exception must be informative with context.** No bare "parse error" or "mapping error" messages.
2. **Chain the real cause.** Always use the `cause` parameter when wrapping exceptions.
3. **Never swallow exceptions.** Catch-and-ignore is forbidden.
4. **Use RuntimeException.** All KissJson exceptions are unchecked — users should not be forced to handle JSON errors at compile time.
5. **Expose structured context.** Parse errors expose line/column/offset. Mapping errors expose JSON path, target type, field name, expected type, actual value.

## Exception Hierarchy

```
RuntimeException
└── JsonException
    ├── JsonParseException
    └── JsonMappingException
```

### `JsonException` (Base)

Package: `io.github.arthurhoch.kissjson`

The base exception for all KissJson errors. Extends `RuntimeException`.

```java
public class JsonException extends RuntimeException {
    public JsonException(String message)
    public JsonException(String message, Throwable cause)
}
```

This is the catch-all type. Users who want to handle all KissJson errors uniformly can catch `JsonException`.

### `JsonParseException`

Package: `io.github.arthurhoch.kissjson`

Thrown by the parser when JSON input is malformed or structurally invalid.

```java
public class JsonParseException extends JsonException {
    int line()
    int column()
    int offset()
}
```

#### Context Methods

| Method | Return | Description |
|---|---|---|
| `line()` | `int` | 1-based line number where the error occurred |
| `column()` | `int` | 1-based column number where the error occurred |
| `offset()` | `int` | 0-based character offset from the start of the input |

The message is stored via `RuntimeException`'s standard `message` field — there is no separate `message` field on `JsonParseException`. Callers use `getMessage()` as usual.

#### Example Messages

```
JSON parse error at line 1, column 42: expected string field name
JSON parse error at line 3, column 15: unexpected character 'x', expected value (string, number, boolean, null, object, or array)
JSON parse error at line 2, column 8: invalid number format
JSON parse error at line 5, column 1: unexpected end of input, expected closing '}'
JSON parse error at line 1, column 1: empty input
```

#### Construction Pattern

```java
// Intended construction pattern (internal)
throw new JsonParseException(
    "JSON parse error at line " + line + ", column " + column + ": " + detail,
    cause  // nullable
);
```

The parser tracks line, column, and offset as it reads. When an error occurs, it passes these to the exception constructor. The `line()`, `column()`, and `offset()` values are extracted from the message or stored internally — the exact storage mechanism is an implementation detail.

### `JsonMappingException`

Package: `io.github.arthurhoch.kissjson`

Thrown by the object mapper when JSON cannot be mapped to the target Java type.

```java
public class JsonMappingException extends JsonException {
    String jsonPath()
    Class<?> targetType()
    String fieldName()
    Class<?> expectedType()
    Object actualValue()
}
```

#### Context Methods

| Method | Return | Description |
|---|---|---|
| `jsonPath()` | `String` | JSON path to the problematic value (e.g., `$.user.address.city`) |
| `targetType()` | `Class<?>` | The Java type being deserialized into (e.g., `User.class`) |
| `fieldName()` | `String` | The field name where mapping failed (e.g., `"birthDate"`) |
| `expectedType()` | `Class<?>` | The expected Java type for the field (e.g., `LocalDate.class`) |
| `actualValue()` | `Object` | The actual JSON value that could not be mapped (may be null) |

Any of these may return `null` if the context is not available for a given error. The message is stored via `RuntimeException`'s standard `message` field.

#### Example Messages

```
JSON mapping error at $.user.birthDate: cannot parse LocalDate from "29/04/2026"; expected ISO date yyyy-MM-dd
JSON mapping error at $.items[3].price: cannot assign null to primitive double
JSON mapping error at $.status: cannot map string "ACTIVE" to enum Status; no constant matches
JSON mapping error at $.id: required field 'id' is missing
JSON mapping error at $: unknown property 'foo' in type User (failOnUnknownProperties is enabled)
JSON mapping error at $.tags[2]: expected STRING, got NUMBER
JSON mapping error at $.user: cycle detected (failOnCycles is enabled)
JSON mapping error at $: max depth exceeded (128)
```

#### Construction Pattern

```java
// Intended construction pattern (internal)
throw new JsonMappingException(
    "JSON mapping error at " + path + ": " + detail,
    cause  // nullable
);
```

The mapper tracks the current JSON path (e.g., `$.user.address.city`) as it descends into objects and arrays. When an error occurs, it includes the full path context.

## When Each Exception Is Thrown

### `JsonParseException` — Parser Errors

| Scenario | Example |
|---|---|
| Malformed JSON syntax | Missing commas, unclosed strings, trailing commas |
| Invalid token | `tru` instead of `true` |
| Unterminated structure | Missing closing `}` or `]` |
| Invalid number | Leading zeros, multiple decimal points |
| Invalid unicode escape | `\u00GG` |
| Empty or null input | `""` or `null` input string |
| Max depth exceeded (parse) | Nesting deeper than `maxDepth` |

### `JsonMappingException` — Mapper Errors

| Scenario | Example |
|---|---|
| Type mismatch | JSON string where integer expected |
| Null for primitive | `null` for `int` field when `failOnNullForPrimitives=true` |
| Unknown property | Extra JSON key when `failOnUnknownProperties=true` |
| Missing required field | `@JsonRequired` field absent from JSON |
| Date parse failure | Cannot parse date string with configured format |
| Enum mismatch | No enum constant matches the JSON value |
| Duplicate keys | Duplicate key when `failOnDuplicateKeys=true` |
| Cycle detected | Circular reference when `failOnCycles=true` |
| Max depth exceeded (serialize) | Object graph deeper than `maxDepth` |

## Exception Chaining

All KissJson exceptions support a `cause` parameter. When an underlying exception occurs (e.g., `DateTimeException` from `LocalDate.parse()`), it **must** be chained:

```java
// GOOD: chain the cause
throw new JsonMappingException(
    "JSON mapping error at $.date: " + e.getMessage(),
    e
);

// BAD: swallow the cause
throw new JsonMappingException(
    "JSON mapping error at $.date: date parse failed"
    // cause missing!
);
```

### Rules for Chaining

1. **Always include the original exception as the cause** when wrapping.
2. **Never catch and ignore.** If you catch an exception, either rethrow it or wrap it.
3. **Preserve the full stack trace.** The cause chain lets users see the root problem.
4. **Include context in the wrapping message.** The outer message should add KissJson-specific context (JSON path, types).

## Error Message Format

All error messages follow a consistent format:

### Parse Errors

```
JSON parse error at line {line}, column {column}: {detail}
```

### Mapping Errors

```
JSON mapping error at {jsonPath}: {detail}
```

Where `{detail}` is a human-readable description that includes:
- What was expected (type, format, structure)
- What was actually found (value, type, token)
- How to fix it (when practical)

## Internal Implementation Guidelines

### Parser Error Tracking

The parser maintains:
- **Line counter** — incremented on `\n`.
- **Column counter** — incremented per character, reset on `\n`.
- **Offset counter** — incremented per character, never reset.

These are passed to `JsonParseException` constructors at the point of failure.

### Mapper Path Tracking

The mapper maintains a path stack:
- On entering an object field: push `.fieldName`
- On entering an array index: push `[index]`
- On exiting: pop.
- Current path: `$` + joined stack entries.

This produces paths like `$.users[2].address.city`.

### Never Expose Internal Classes in Exceptions

Exception messages and stack traces must not reference internal class names. Use user-facing terminology:
- Say "field `name`" not "FieldModel{name}"
- Say "type `User`" not "ClassModel@1a2b3c"
- Say "JSON parse error" not "TokenizerException"

## Testing Requirements

Every exception type and scenario must be tested:

1. **Parse errors** — Verify `line()`, `column()`, `offset()`, and `getMessage()`.
2. **Mapping errors** — Verify `jsonPath()`, `targetType()`, `fieldName()`, `getMessage()`.
3. **Exception chaining** — Verify `getCause()` returns the original exception.
4. **No swallowed exceptions** — Verify all error paths produce informative exceptions.
5. **Edge cases** — Empty input, null input, very deep nesting, very long strings.
