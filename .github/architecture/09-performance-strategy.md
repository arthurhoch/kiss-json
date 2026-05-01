# 09 — Performance Strategy

> **Status:** Implemented for v1.

## Overview

KissJson is designed to be fast by default, not fast by complexity. Performance comes from caching metadata, avoiding repeated work, and choosing simple algorithms over clever ones. **Correctness always comes first.** We optimize only after the code is correct and tested.

Performance is subordinate to KISS. Developer time matters more than theoretical machine-time wins from a confusing architecture. Optimizations are welcome when they stay internal and preserve the public mental model:

```java
Json json = Json.create();
String text = json.stringify(user);
User user = json.parse(text, User.class);
```

Users should not need to understand parser internals, token streams, metadata caches, class models, field models, date codecs, type converters, or performance knobs before they can serialize and parse JSON.

## Performance Principles

1. **Cache everything that can be cached.** Reflection results, field lists, name mappings, annotation flags.
2. **Avoid repeated reflection.** Reflect once per class, store the result, reuse forever.
3. **Avoid regex.** No regex in the parser or serializer. Character-by-character processing is faster and more predictable.
4. **Avoid `String.split`.** No `String.split` in the parser. Tokenize directly from the character stream.
5. **Minimize allocations.** Reuse buffers where possible. Avoid intermediate objects on hot paths.
6. **Correctness first, then optimize hotspots.** Profile before optimizing. Do not guess where the bottlenecks are.
7. **Keep optimizations internal.** Do not expose cache, parser, token, field model, or codec APIs to users.
8. **Avoid performance theater.** Do not add complex knobs unless a common user need and measured evidence justify them.

## Caching Strategy

### `ClassModelCache`

The central caching mechanism. Stored as a `ConcurrentHashMap` keyed by composite `(Class<?>, FieldNaming)`.

```
ConcurrentHashMap<CacheKey(Class<?>, FieldNaming), ClassModel>
```
ConcurrentHashMap<Class<?>, ClassModel>
     │
     ▼
ClassModel (per class, created once)
├── List<FieldModel> — all serializable fields
├── Map<String, FieldModel> — primary name → field
├── Map<String, FieldModel> — alias → field (union of all @JsonAliases)
├── FieldNaming — the naming strategy used to compute names
└── Various flags and precomputed data
```

#### What Is Cached Per Class

| Cached Item | Why |
|---|---|
| Field list (ordered) | Reflection `getDeclaredFields()` is expensive; call once |
| Primary JSON names per field | Computed from field name + `FieldNaming` + `@JsonName` |
| Escaped field-name prefix | Precomputed `"name":` text for direct serialization |
| Alias map | `@JsonAliases` resolved once |
| Alias presence flag | Avoids alias-tracking allocation for classes without aliases |
| Annotation flags per field | `@JsonIgnore`, `@JsonRequired`, `@JsonIncludeNull`, etc. |
| Field accessibility | `setAccessible(true)` called once |
| Field types | Precomputed for fast dispatch during serialization/deserialization |
| Superclass fields | Walked once and flattened into the field list |

#### Thread Safety

- `ConcurrentHashMap` handles concurrent access safely.
- `ClassModel` instances are immutable after creation.
- No locking is needed for reads.
- Creation is idempotent — if two threads create a `ClassModel` for the same class simultaneously, one wins and the other is discarded. No corruption.

#### Cache Invalidation

- **No invalidation in v1.** Classes do not change at runtime in normal usage.
- The cache grows proportionally to the number of distinct classes serialized/deserialized.
- If memory becomes a concern in future versions, an LRU eviction policy could be added.

## Serialization Path

### Direct `JsonWriter` — No Intermediate Tree

The serialize path writes directly to a `StringBuilder` without building an intermediate tree:

```
Object → ObjectWriter → JsonWriter → StringBuilder → String
              │               │
              │               └── write methods: writeString, writeNumber,
              │                   writeBoolean, writeNull, writeObjectStart, etc.
              │
              └── Uses ClassModel from cache
                  Uses FieldNaming from config
                  Checks annotations per field
```

#### Why Direct Writing?

- **No intermediate node objects.** Avoids allocating a tree of `JsonValue` nodes.
- **No tree traversal cost.** One pass from Java object to JSON string.
- **Lower GC pressure.** Fewer short-lived objects.
- **Simpler code.** Straightforward recursive descent.

#### `JsonWriter` Design

```java
// Intended internal API (not public)
class JsonWriter {
    JsonWriter(StringBuilder out, JsonConfig config)

    void writeString(String value)      // handles escaping
    void writeNumber(Number value)      // handles all number types
    void writeBoolean(boolean value)
    void writeNull()
    void writeObjectStart()
    void writeObjectEnd()
    void writeArrayStart()
    void writeArrayEnd()
    void writeFieldName(String name)
    void writeSeparator()               // comma between elements
    void writePrettyIndent(int depth)   // for pretty printing
}
```

The writer handles:
- **String escaping** — `"`, `\`, control characters, unicode.
- **Pretty printing** — indentation controlled by `JsonConfig.prettyPrint`.
- **Null handling** — skip or include based on `JsonConfig.includeNulls` and field annotations.

## Deserialization Path (v1)

### Token-Based `JsonParser`

The parser reads characters directly from the input string, producing a stream of tokens. No regex, no `String.split`.

```
String → JsonReader token stream → ObjectReader → Object
              │                    │
              │                    └── Uses ClassModel from cache
              └── Reads chars directly
                  Tracks line, column, offset
                  No regex, no String.split
```

#### Token Types

| Token | Description |
|---|---|
| `OBJECT_START` | `{` |
| `OBJECT_END` | `}` |
| `ARRAY_START` | `[` |
| `ARRAY_END` | `]` |
| `COLON` | `:` |
| `COMMA` | `,` |
| `STRING` | A JSON string value (unescaped) |
| `NUMBER` | A JSON number value (as string, parsed to type on demand) |
| `BOOLEAN` | `true` or `false` |
| `NULL` | `null` |
| `EOF` | End of input |

#### Why Token-Based?

- **Predictable performance.** Character-by-character reading has no backtracking or regex engine overhead.
- **Precise error location.** Line, column, and offset are tracked naturally.
- **No regex dependency.** Avoids regex compilation, pattern matching, and backtracking costs.
- **Simple code.** A state machine with `switch` statements is easier to understand and maintain.

### Direct Token-to-Object Deserialization

The normal deserialization path maps directly from parser tokens to Java objects. This avoids allocating a full internal `JsonValue` tree for typed reads:

1. **Lower allocation.** Typed `parse`, `parseList`, and typed `parseMap` do not allocate intermediate object/array nodes.
2. **Skip unknown values cheaply.** Unknown properties are skipped from the token stream while still validating nested JSON syntax.
3. **Cached metadata.** `ObjectReader` resolves fields through `ClassModelCache` and precomputed primary names and aliases.
4. **Lazy path strings.** Mapping uses a mutable internal path and only creates a full path string for exceptions.
5. **Repeated POJO elements.** Typed POJO lists and maps reuse the same `ClassModel` across elements/entries once the model is resolved.

#### `JsonValue` Design

```java
// Intended internal sealed interface (Java 17 sealed classes)
sealed interface JsonValue permits JsonNull, JsonBool, JsonString, JsonNumber, JsonArray, JsonObject {}

record JsonNull() implements JsonValue
record JsonBool(boolean value) implements JsonValue
record JsonString(String value) implements JsonValue
record JsonNumber(String raw) implements JsonValue  // parse to specific type on demand
record JsonArray(List<JsonValue> values) implements JsonValue
record JsonObject(Map<String, JsonValue> fields) implements JsonValue
```

`JsonValue` may remain package-private for limited generic representation, but it is not part of the typed object fast path and is never public API.

## Cycle Detection

During serialization, cycle detection uses an `IdentityHashMap<Object, Object>`:

```
ObjectWriter.serialize(Object value, ...):
    if (value is already in visited map):
        if (config.failOnCycles()):
            throw JsonMappingException("cycle detected")
        else:
            return  // skip (or write null)
    visited.put(value, PRESENT)
    ... serialize fields ...
    visited.remove(value)
```

### Why `IdentityHashMap`?

- **Uses reference equality (`==`), not `.equals()`.** Two different objects with the same content are not confused.
- **O(1) contains check.** Fast lookup.
- **Cleared after serialization completes.** No memory leak.

### Behavior

- **`failOnCycles = true` (default)** — Throws `JsonMappingException` with the JSON path where the cycle was detected.
- **`failOnCycles = false`** — Skips the already-visited object (writes `null` or skips the field entirely — TBD based on testing what is least surprising).

## Memory Considerations

### What Is Allocated

| Path | Allocations | Notes |
|---|---|---|
| Serialize | `StringBuilder`, `JsonWriter`, visited map | Direct write, minimal garbage |
| Deserialize | Token reader + target objects | No `JsonValue` tree on typed fast path |
| ClassModel cache | One `ClassModel` per class | Permanent, proportional to class count |

### What Is NOT Allocated

- No intermediate `Map<String, Object>` representations.
- No annotation scanning or field discovery per serialization/deserialization (cached).
- No regex pattern compilation (no regex used).
- No `String.split` arrays (no split used).
- No successful-operation JSON path `String` allocation in typed mapping.

## Benchmark Plan

### When to Benchmark

Benchmarking is **optional**, **isolated from the main build**, and never a reason to add production dependencies. It should only be run:
- After v1 implementation is complete and correct.
- When investigating a specific performance concern.
- Before releasing, to establish baseline numbers.

### JMH Setup

- **Separate Maven profile or module** — `benchmarks/` directory or Maven profile.
- **Not part of `mvn verify`.** Benchmarks are not run in CI.
- **JMH is a test/benchmark dependency only.** It is never a production dependency.
- **Compare with other libraries only in benchmarks.** Jackson is currently available only in the `benchmark` profile and must never appear in production dependencies.

### What to Benchmark

1. Serialize simple object (few fields, primitives + strings)
2. Serialize nested object (3-4 levels deep)
3. Serialize large list (1000+ elements)
4. Deserialize simple object
5. Deserialize nested object
6. Deserialize large list
7. Parse JSON string only (no mapping)
8. Round-trip (serialize then deserialize)
9. Compare KissJson and Jackson on the same data shapes when evaluating regressions.

### Benchmark Rules

- **Do not add JMH as a production dependency.** It goes in a separate profile or module.
- **Do not add Jackson or Gson as production dependencies.** They are benchmark-only.
- **Do not optimize based on guesses.** Profile first, optimize the actual bottleneck.
- **Do not sacrifice readability for micro-optimization.** Code clarity wins.
- **Do not expose benchmark-driven internals.** A faster internal cache is acceptable; a public cache API is not.

## Performance Priorities (Ordered)

1. **Correctness** — Wrong fast results are worse than correct slow results.
2. **Clarity** — Simple, readable code is easier to optimize later.
3. **Public API simplicity** — Users should not pay cognitive cost for internal speed.
4. **Hot path optimization** — Focus on serialization and deserialization inner loops.
5. **Allocation reduction** — Reduce GC pressure on the main paths.
6. **Micro-optimization** — Only after profiling identifies a specific issue.

## Anti-Patterns (Do NOT Use)

- **Regex in parser** — Slow, unpredictable, hard to debug.
- **`String.split` in parser** — Allocates arrays, slower than direct scanning.
- **Reflection per call** — Must be cached.
- **Synchronized blocks on hot paths** — Use `ConcurrentHashMap` or lock-free structures.
- **Thread-local buffers** — Adds complexity; prefer stack-local or passed-in buffers.
- **Object pools** — Adds complexity; let the GC do its job in v1.
- **Unsafe / sun.misc** — Not portable, not Java 17 guaranteed.
- **Method handles in v1** — Reflection with `setAccessible(true)` is simpler. However, MethodHandle getters/setters are now used in FieldModel as an optimization with Field.get/set as fallback.
