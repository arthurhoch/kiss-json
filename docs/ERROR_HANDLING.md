# Error Handling

> **Current v1 contract.** This document describes the error handling behavior for KissJson `0.1.0-SNAPSHOT`.

## Exception Hierarchy

All KissJson exceptions are unchecked and extend `RuntimeException` so they do not require explicit `try/catch` blocks:

```
RuntimeException
└── JsonException
    ├── JsonParseException
    └── JsonMappingException
```

| Exception | When Thrown |
|---|---|
| `JsonException` | Base exception for all KissJson errors. Not thrown directly in normal usage. |
| `JsonParseException` | Invalid JSON input: malformed syntax, unexpected tokens, truncated input. |
| `JsonMappingException` | Valid JSON that cannot be mapped to the target Java type: type mismatches, missing fields, unknown properties. |

All exceptions include a descriptive message and chain the original cause via `getCause()`.

---

## JsonParseException

Thrown when the JSON input is syntactically invalid or cannot be tokenized.

### Fields

| Method | Return Type | Description |
|---|---|---|
| `line()` | `int` | 1-based line number where the error was detected. |
| `column()` | `int` | 1-based column number where the error was detected. |
| `offset()` | `int` | 0-based character offset from the start of the input. |

The exception message is accessed via `getMessage()` (inherited from `RuntimeException`). There is no separate message field — the standard `getMessage()` returns a human-readable string that includes the location and nature of the error.

### Example Messages

```
Unexpected character 'x' at line 3, column 12 (offset 47)
Expected ':' after object key at line 1, column 18 (offset 17)
Unexpected end of input at line 5, column 1 (offset 203)
Invalid number format at line 2, column 9 (offset 31): '1.2.3'
Unterminated string at line 4, column 5 (offset 112)
Trailing content after valid JSON at line 1, column 42 (offset 41)
```

### Usage

```java
try {
    Json json = Json.create();
    MyClass result = json.parse(input, MyClass.class);
} catch (JsonParseException e) {
    System.err.println("Parse error at line " + e.line()
        + ", column " + e.column()
        + ": " + e.getMessage());
    if (e.getCause() != null) {
        System.err.println("Caused by: " + e.getCause());
    }
}
```

---

## JsonMappingException

Thrown when valid JSON cannot be mapped to the target Java type.

### Fields

| Method | Return Type | Description |
|---|---|---|
| `jsonPath()` | `String` | JSON path to the problematic value (e.g., `$.user.address.city`). May be empty if not applicable. |
| `targetType()` | `Class<?>` | The Java type being mapped to. May be `null` if not applicable. |
| `fieldName()` | `String` | The Java field name being mapped. May be `null` if not applicable. |
| `expectedType()` | `Class<?>` | The expected Java type for the value. May be `null` if not applicable. |
| `actualValue()` | `Object` | The actual value encountered (may be `null`). String representation for diagnostic purposes. |

The exception message is accessed via `getMessage()` (inherited from `RuntimeException`). There is no separate message field.

### Example Messages

```
Cannot map JSON string to int at $.age — expected NUMBER but got STRING ("thirty")
Null value for primitive field 'count' (int) at $.items[2].count
Missing required field 'email' in class com.example.User at $.users[0]
Unknown property 'phonenumber' at $.user — did you mean 'phoneNumber'?
Duplicate key 'name' in JSON object at $.user
Cannot deserialize JSON array to class com.example.Address at $.user.address
Cycle detected at $.manager.department.manager — type: Department (path: $.manager)
Max depth (64) exceeded at $.deeply.nested.levels — increase JsonConfig.maxDepth() if needed
Type mismatch at $.price — expected NUMBER but got BOOLEAN (true)
```

### Usage

```java
try {
    Json json = Json.create();
    var result = json.parse(input, MyClass.class);
} catch (JsonMappingException e) {
    System.err.println("Mapping error at " + e.jsonPath()
        + " — field '" + e.fieldName() + "'"
        + " in " + e.targetType().getSimpleName()
        + ": " + e.getMessage());
    if (e.getCause() != null) {
        System.err.println("Caused by: " + e.getCause());
    }
}
```

---

## Error Examples with Good Messages

### Parse Error at Line/Column

```java
String json = """
    {
      "name": "Alice",
      "age": xyz
    }
    """;
// JsonParseException: Unexpected character 'x' at line 3, column 12 (offset 31)
//   line() -> 3
//   column() -> 12
//   offset() -> 31
```

### Mapping Error at JSON Path with Type Info

```java
String json = """
    {
      "name": "Alice",
      "age": "thirty"
    }
    """;
// JsonMappingException: Cannot map JSON string to int at $.age — expected NUMBER but got STRING ("thirty")
//   jsonPath()    -> "$.age"
//   targetType()  -> class com.example.Person
//   fieldName()   -> "age"
//   expectedType() -> int.class
//   actualValue() -> "thirty"
```

### Null for Primitive

```java
String json = """
    {
      "name": "Alice",
      "age": null
    }
    """;
// JsonMappingException: Null value for primitive field 'age' (int) at $.age
//   jsonPath()    -> "$.age"
//   fieldName()   -> "age"
//   expectedType() -> int.class
//   actualValue() -> null
```

### Missing Required Field

```java
@JsonRequired
String email;

Json strict = Json.builder().failOnMissingRequiredFields(true).build();

// JsonMappingException: Missing required field 'email' in class com.example.User at $
//   jsonPath()   -> "$"
//   targetType() -> class com.example.User
//   fieldName()  -> "email"
```

### Duplicate Key

```java
String json = """
    {
      "name": "Alice",
      "name": "Bob"
    }
    """;
// JsonParseException: Duplicate key 'name' in JSON object at line 3, column 7 (offset 32)
//   line()   -> 3
//   column() -> 7
//   offset() -> 32
```

### Cycle Detected

```java
// During serialization of a circular object graph:
// JsonMappingException: Cycle detected at $.manager.department.manager — type: Department
//   jsonPath()   -> "$.manager.department.manager"
//   targetType() -> class com.example.Department
```

### Max Depth Exceeded

```java
// During serialization or deserialization of deeply nested structures:
// JsonMappingException: Max depth (128) exceeded at $.deeply.nested.levels — increase JsonConfig.maxDepth() if needed
//   jsonPath()   -> "$.deeply.nested.levels"
```

---

## Exception Chaining

All KissJson exceptions preserve the original cause via standard Java exception chaining (`initCause()` / constructor with `Throwable cause`). This ensures that underlying issues (e.g., `IllegalAccessException`, `NumberFormatException`, `DateTimeParseException`) are never lost.

```java
try {
    Json json = Json.create();
    var result = json.parse(jsonInput, MyClass.class);
} catch (JsonException e) {
    // e.getMessage() -> KissJson's descriptive message
    // e.getCause()   -> the underlying exception, if any
    logger.error("KissJson error: {}", e.getMessage(), e);
}
```

### Chain Examples

| Scenario | KissJson Exception | Cause |
|---|---|---|
| Invalid number literal | `JsonParseException` | none |
| Malformed date string | `JsonMappingException` | `DateTimeParseException` |
| Field not accessible | `JsonMappingException` | `IllegalAccessException` |
| Number overflow | `JsonMappingException` | `ArithmeticException` |

---

## How to Catch and Inspect Errors in User Code

### Catch All KissJson Errors

```java
try {
    Json json = Json.create();
    MyClass obj = json.parse(jsonInput, MyClass.class);
} catch (JsonException e) {
    // Handles both JsonParseException and JsonMappingException
    System.err.println(e.getMessage());
    if (e.getCause() != null) {
        System.err.println("Root cause: " + e.getCause().getMessage());
    }
}
```

### Catch Parse and Mapping Errors Separately

```java
try {
    Json json = Json.create();
    MyClass obj = json.parse(jsonInput, MyClass.class);
} catch (JsonParseException e) {
    // JSON is malformed — check line/column/offset
    System.err.println("Invalid JSON at line " + e.line() + ", col " + e.column());
    System.err.println("Offset: " + e.offset());
} catch (JsonMappingException e) {
    // JSON is valid but doesn't match the Java type
    System.err.println("Mapping failed at path: " + e.jsonPath());
    System.err.println("Target type: " + e.targetType().getName());
    System.err.println("Field: " + e.fieldName());
    System.err.println("Expected: " + e.expectedType());
    System.err.println("Got: " + e.actualValue());
}
```

### Extract JSON Path from Mapping Errors

```java
try {
    Json json = Json.create();
    List<User> users = json.parseList(jsonInput, User.class);
} catch (JsonMappingException e) {
    // jsonPath() tells you exactly where in the JSON the problem is
    String path = e.jsonPath(); // e.g., "$.users[2].email"
    System.err.println("Problem at " + path + ": " + e.getMessage());
}
```

### Inspect the Cause Chain

```java
try {
    Json json = Json.create();
    LocalDate date = json.parse(jsonInput, LocalDate.class);
} catch (JsonMappingException e) {
    Throwable cause = e.getCause();
    if (cause instanceof DateTimeParseException dtp) {
        System.err.println("Date format error: " + dtp.getMessage());
    }
}
```

---

## Design Principles

1. **Every exception is informative.** No bare "mapping error" or "parse error" messages. Every message includes location (line/col or JSON path) and what went wrong.
2. **The cause is never swallowed.** If an underlying exception exists, it is always chained via `getCause()`.
3. **Exceptions are unchecked.** Extending `RuntimeException` avoids forcing `try/catch` on every call. Users who care about errors can catch `JsonException` (or its subclasses) at the appropriate level.
4. **Context is structured.** Parse errors expose `line()`, `column()`, `offset()`. Mapping errors expose `jsonPath()`, `targetType()`, `fieldName()`, `expectedType()`, `actualValue()`. This makes programmatic error handling practical.
5. **No internal leakage.** Exception messages reference JSON concepts (paths, tokens, types) — never internal class names or implementation details.
