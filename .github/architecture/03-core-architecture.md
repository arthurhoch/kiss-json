# 03 — Core Architecture

This document describes the internal component design of KissJson. All components described here are **internal** — never exposed as public API.

The architecture exists to support one small public task:

```java
Json json = Json.create();
String text = json.stringify(user);
User user = json.parse(text, User.class);
```

All parser, writer, mapping, cache, naming, date, and type-conversion components are implementation details. They must not become public extension points or framework hooks.

---

## Package Structure

```
io.github.arthurhoch.kissjson/          ← Public API package
├── Json.java                           ← Facade entry point
├── JsonBuilder.java                    ← Builder for Json instances
├── JsonConfig.java                     ← Immutable configuration
├── JsonException.java                  ← Base exception
├── JsonParseException.java             ← Parse error with line/col/offset
├── JsonMappingException.java           ← Mapping error with context
├── FieldNaming.java                    ← Naming strategy enum
├── DateFormat.java                     ← Date format enum
├── EnumMode.java                       ← Enum mode enum
├── JsonName.java                       ← @JsonName annotation
├── JsonAliases.java                    ← @JsonAliases annotation
├── JsonIgnore.java                     ← @JsonIgnore annotation
├── JsonRequired.java                   ← @JsonRequired annotation
├── JsonIncludeNull.java                ← @JsonIncludeNull annotation
├── JsonExcludeNull.java                ← @JsonExcludeNull annotation
└── JsonDateFormat.java                 ← @JsonDateFormat annotation

io.github.arthurhoch.kissjson.internal/ ← Internal package (package-private)
├── JsonParser.java                     ← Token-based recursive descent parser
├── JsonWriter.java                     ← Direct StringBuilder writer
├── JsonValue.java                      ← Internal JSON tree model
├── ObjectReader.java                   ← JSON → Java object mapper
├── ObjectWriter.java                   ← Java object → JSON mapper
├── ClassModel.java                     ← Cached field metadata for a class
├── FieldModel.java                     ← Per-field metadata
├── ClassModelCache.java                ← Concurrent class metadata cache
├── NamingStrategy.java                 ← Field name conversion logic
├── DateCodec.java                      ← Date/time encoding and decoding
├── TypeConverter.java                  ← Type handling and conversion
└── JsonPaths.java                      ← JSON path tracking for errors
```

---

## Component Overview

### KISS Architecture Boundary

KissJson is a JSON library, not an application framework. Internal components may be specialized, but the user-facing model must stay small:

- `Json` is the facade.
- `JsonBuilder` is optional configuration for common behavior.
- `JsonConfig` is a read-only snapshot.
- Everything else is internal.

Do not add built-in integrations for HTTP clients, web frameworks, dependency injection containers, databases, logging systems, telemetry tools, schema validators, JSONPath, or JSON Patch. Users can compose KissJson with those tools by passing strings in and out.

```java
String body = json.stringify(request);
HttpResult result = http.request(HttpMethod.POST, url, headers, body).execute();
Response response = json.parse(result.body(), Response.class);
```

Performance structures such as `JsonReader`, `ClassModelCache`, `FieldModel`, `JsonValue`, `DateCodec`, and `TypeConverter` are allowed because they keep repeated work out of the hot path. They are not public API and must not be required knowledge for normal use.

### `Json` (Public Facade)

The `Json` class is the only entry point users interact with. It holds a `JsonConfig` instance and delegates all work to internal components:

- **Serialization:** `json.stringify(obj)` → `ObjectWriter` → `JsonWriter` → `String`
- **Deserialization:** `json.parse(text, Type.class)` → `JsonReader` token stream → `ObjectReader` → typed Java object

The `Json` facade is stateless with respect to user objects. It does not hold references to serialized or deserialized objects.

### `JsonReader` / `JsonParser` (Internal)

Token-based parser. Reads JSON text as a `char[]` and exposes internal tokens consumed by `ObjectReader`.

The normal typed deserialization path does **not** build a `JsonValue` tree. `JsonValue` may remain as an internal implementation detail for limited generic handling, but it must not be used by `Json.parse(text, Class<T>)`, `Json.parseList(text, Class<T>)`, or `Json.parseMap(text, Class<T>)`.

Responsibilities:
- Parse JSON text character by character.
- Track current offset, line number, and column number.
- Produce internal tokens for object starts/ends, array starts/ends, strings, numbers, booleans, null, colon, comma, and end of input.
- Enforce `maxDepth` limit.
- Enforce `failOnDuplicateKeys` behavior.
- Skip unknown nested values while still validating JSON syntax.
- Throw `JsonParseException` with line, column, and offset on invalid input.

See [04-json-parser.md](04-json-parser.md) for full details.

### `JsonWriter` (Internal)

Direct `StringBuilder`-based writer. Receives calls from `ObjectWriter` during object serialization and produces a JSON string without building an intermediate tree.

Responsibilities:
- Write compact or pretty-printed JSON.
- Escape strings according to JSON specification (quote, backslash, control characters, unicode).
- Format numbers correctly (int, long, double, BigDecimal).
- Write `null`, `true`, `false`.
- Format dates according to `DateFormat` config.
- Format enums according to `EnumMode` config.

See [05-json-writer.md](05-json-writer.md) for full details.

### `JsonValue` (Internal)

Internal tree model for limited generic JSON representation. Not exposed to users and not used by the typed deserialization fast path. Node types:

- **Object:** `Map<String, JsonValue>` (preserves insertion order via `LinkedHashMap`).
- **Array:** `List<JsonValue>`.
- **String:** `String` value.
- **Number:** stored as `long` if integral and fits, otherwise `double` if fits, otherwise `BigDecimal`.
- **Boolean:** `boolean` value.
- **Null:** sentinel.

Provides type-checking accessors: `asString()`, `asInt()`, `asLong()`, `asDouble()`, `asBigDecimal()`, `asBoolean()`, `asObject()`, `asArray()`, `isNull()`.

### `ObjectReader` (Internal)

Maps the internal token stream to a Java object. Uses `ClassModelCache` to look up field metadata, then sets field values via reflection.

Responsibilities:
- Look up `ClassModel` from cache for the target class.
- Read object keys from the token stream.
- Resolve each key by precomputed primary names and aliases.
- Convert the current token value to the field's Java type (recursively for nested objects, arrays, lists, maps).
- Handle null, missing fields, unknown properties, required fields.
- Skip unknown values directly from the token stream when unknown properties are ignored.
- Track JSON path via `JsonPaths` for error context.
- Throw `JsonMappingException` with full context on errors.

See [06-object-mapping.md](06-object-mapping.md) for full details.

### `ObjectWriter` (Internal)

Maps a Java object to JSON. Uses `ClassModelCache` to look up field metadata, then writes field values.

Responsibilities:
- Look up `ClassModel` from cache for the source class.
- Iterate `FieldModel` entries.
- For each field, read the value via reflection.
- Handle null (include/exclude based on config and annotations).
- Handle cycles via `IdentityHashMap` tracking.
- Write field name and value via `JsonWriter`.
- Handle dates, enums, numbers, nested objects, arrays, lists, maps.

### `ClassModel` (Internal)

Cached metadata for a Java class. Contains an array of `FieldModel` entries, one per mappable field.

Responsibilities:
- Scan the class for mappable fields (non-static, non-transient, non-synthetic, not `@JsonIgnore`).
- Include fields from superclasses (walk the hierarchy, stop at `Object`).
- Verify the class has a no-arg constructor (any visibility). Throw `JsonMappingException` if not.
- Create `FieldModel` instances for each field.
- Cache the result for reuse.

### `FieldModel` (Internal)

Metadata for a single field. Created once per field and cached.

Contains:
- `Field` reference (with `setAccessible(true)` already called).
- Primary JSON name (from `@JsonName` or naming strategy applied to field name).
- Alias names (from `@JsonAliases`).
- Field type (`Class<?>`).
- Annotations: `@JsonRequired`, `@JsonIncludeNull`, `@JsonExcludeNull`, `@JsonDateFormat`.
- Whether the field is primitive.
- Generic type information (for `List<T>`, `Map<String, T>` type parameters).

### `ClassModelCache` (Internal)

Concurrent cache mapping `Class<?>` → `ClassModel`. Uses `ConcurrentHashMap` with lazy computation.

Properties:
- Thread-safe. Multiple threads can request `ClassModel` for the same class simultaneously.
- No eviction policy. Classes are loaded once and cached for the lifetime of the JVM.
- `ClassModel` creation is idempotent — concurrent computation for the same class produces the same result.

### `NamingStrategy` (Internal)

Converts Java field names to JSON keys based on the `FieldNaming` enum. Called by `ClassModel` during field metadata creation.

Supported conversions:
- `IDENTITY`: no conversion.
- `LOWER_CASE`: `userName` → `username`.
- `UPPER_CASE`: `userName` → `USERNAME`.
- `CAMEL_CASE`: `user_name` → `userName`.
- `SNAKE_CASE`: `userName` → `user_name`.
- `KEBAB_CASE`: `userName` → `user-name`.

### `DateCodec` (Internal)

Handles serialization and deserialization of date/time types.

Supported types:
- `java.time.LocalDate`
- `java.time.LocalTime`
- `java.time.LocalDateTime`
- `java.time.OffsetDateTime`
- `java.time.ZonedDateTime`
- `java.time.Instant`
- `java.time.Duration`
- `java.time.Period`
- `java.util.Date`
- `java.util.Calendar`

Formats:
- `ISO`: ISO-8601 string representation.
- `EPOCH_MILLIS`: epoch milliseconds (long) for instant-like temporal types.
- `EPOCH_SECONDS`: epoch seconds (long).

### `TypeConverter` (Internal)

Handles type conversions between token values and Java types. Covers:

- Primitive types and wrappers (`int`, `Integer`, `long`, `Long`, `double`, `Double`, `boolean`, `Boolean`, etc.).
- `String` and `char`/`Character`.
- `BigDecimal` and `BigInteger`.
- Enums.
- Arrays (primitive arrays and object arrays).
- `List<T>` (creates `ArrayList`).
- `Map<String, T>` (creates `LinkedHashMap`).
- Nested objects (delegates to `ObjectReader`).

### `JsonPaths` (Internal)

Mutable helper that tracks the current JSON path during parsing and mapping. Builds a path string like `$.users[2].address.city` for error context.

Operations:
- Push object key (`push("city")` → `$.address.city`).
- Push array index (`push(2)` → `$.users[2]`).
- Pop (return to parent level).
- `toString()` returns the current path.
- Used by `ObjectReader` to provide path context in `JsonMappingException`.

---

## Data Flow

### Serialization Flow

```
User code:
  Json json = Json.create();
  String output = json.stringify(userObject);

Internal flow:
  json.stringify(userObject)
    → ObjectWriter.write(userObject, JsonWriter, JsonConfig)
      → ClassModelCache.get(userObject.getClass())
        → ClassModel (with FieldModel[] for all mappable fields)
      → For each FieldModel:
          → Field.get(userObject)  [via reflection]
          → Check null handling (config + annotations)
          → Check cycle detection (IdentityHashMap)
          → Write JSON key via JsonWriter
          → Write JSON value via JsonWriter (recurse for nested objects)
    → JsonWriter.toString()
    → Return JSON string
```

### Deserialization Flow

```
User code:
  User user = json.parse(jsonText, User.class);

Internal flow:
  json.parse(jsonText, User.class)
    → JsonReader(jsonText, JsonConfig)
      → char[] from jsonText
      → Token parsing, tracking offset/line/column
      → Enforce maxDepth
      → Enforce duplicateKeys
    → ObjectReader.read(JsonReader, User.class, JsonConfig)
      → ClassModelCache.get(User.class)
        → ClassModel (with FieldModel[] for all mappable fields)
        → Verify no-arg constructor exists
      → Instantiate User via no-arg constructor
      → For each JSON object key:
          → Resolve FieldModel by precomputed primary name and aliases
          → Check required fields
          → Check unknown properties
          → Convert current token to field type
          → Field.set(instance, value)  [via reflection]
          → Skip ignored unknown nested values without building a tree
    → Return User instance
```

### List Deserialization Flow

```
User code:
  List<User> users = json.parseList(jsonText, User.class);

Internal flow:
  json.parseList(jsonText, User.class)
    → JsonReader(jsonText, JsonConfig)
    → ObjectReader.readList(JsonReader, User.class, JsonConfig)
      → For each array element token:
          → ObjectReader.readValue(JsonReader, User.class, JsonConfig)  [recursive]
      → Return ArrayList<User>
```

### Map Deserialization Flow

```
User code:
  Map<String, User> map = json.parseMap(jsonText, User.class);

Internal flow:
  json.parseMap(jsonText, User.class)
    → JsonReader(jsonText, JsonConfig)
    → ObjectReader.readMap(JsonReader, User.class, JsonConfig)
      → For each object entry token:
          → ObjectReader.readValue(JsonReader, User.class, JsonConfig)  [recursive]
          → Put into LinkedHashMap<String, User>
      → Return LinkedHashMap<String, User>
```

---

## Thread Safety

- `JsonConfig` is immutable. Safe to share across threads.
- `Json` is effectively immutable (holds only `JsonConfig`). Safe to share across threads.
- `ClassModelCache` uses `ConcurrentHashMap`. Thread-safe for concurrent lookups.
- `ClassModel` and `FieldModel` are immutable after creation. Thread-safe.
- `ObjectWriter` and `ObjectReader` are created per-operation. Not shared across threads.
- `JsonParser` is created per-parse operation. Not shared across threads.
- `JsonWriter` is created per-serialize operation. Not shared across threads.

The recommended usage pattern is to create one `Json` instance (or use `Json.create()` for one-off calls) and reuse it across threads.

---

## Status

This document describes the **current v1 contract** for internal architecture in `0.1.0-SNAPSHOT`.
