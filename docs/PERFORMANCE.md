# Performance

> **Current v1 contract.** This document describes the performance strategy and design decisions for KissJson v1.

## Design Principles

KissJson is designed to be fast by default, not fast by accident. The guiding principles:

1. **Cache everything.** Reflection results, annotation scans, field metadata, date formatters — computed once per class, reused forever.
2. **Avoid repeated reflection.** Every `Class<?>` is analyzed exactly once. Field access objects are cached and reused. MethodHandle getters/setters are used where available.
3. **No regex.** The parser is character-by-character with a hand-written lexer. No regex engines, no `Pattern` compilation.
4. **No `String.split`.** The parser never uses `String.split` for tokenization. Every token is extracted by direct character inspection.
5. **Minimize allocations.** Direct writing to `StringBuilder`, cached indent strings, manual hex escaping without `String.format`, fast-path string writing, and precomputed field name writing.

---

## Implemented Optimizations

### Public Facade Dispatch in Json.java

The `Json` facade keeps internal classes package-private and invokes them through a private `RuntimeBridge`. Method handles are resolved once during class initialization, so the public API does not expose internals while steady-state calls avoid repeated reflective lookup.

### Optimized String Escaping

String escaping uses a static hex table (`char[] HEX`) and manual bit-shift operations for `\uXXXX` sequences. No `String.format` is used anywhere in the escaping path. The writer first scans for the first character that requires escaping; if none is found, it appends the original string between quotes without per-character escaping work.

- Characters below `0x20` (control): short escapes for `\b`, `\f`, `\n`, `\r`, `\t`; manual hex for others.
- Printable characters: only `"` and `\\` are escaped.
- Valid non-control Unicode characters are written directly; JSON parsers still accept `\uXXXX` input escapes.

### MethodHandle Field Access

`FieldModel` caches `MethodHandle` getters and setters via `MethodHandles.Lookup.unreflectGetter/unreflectSetter`. If a MethodHandle is available, field access goes through it (which the JIT can inline). Falls back to `Field.get/set` if MethodHandle creation fails.

### MethodHandle Constructors

`ClassModel` caches a `MethodHandle` for the no-arg constructor via `MethodHandles.Lookup.unreflectConstructor`. The `ObjectReader` uses `MethodHandle.invoke()` instead of `Constructor.newInstance()`, eliminating `InvocationTargetException` wrapping overhead.

### Typed Primitive Field Setters

`FieldModel` provides type-specialized setter methods (`setInt`, `setLong`, `setBoolean`, `setDouble`, `setFloat`, `setShort`, `setByte`, `setChar`) that call `Field.setXxx` directly. These avoid the boxing overhead of `MethodHandle.invoke(obj, value)` for primitive fields. The simple POJO fast path uses these typed setters exclusively.

### Typed Primitive Field Getters

`FieldModel` provides type-specialized getter methods (`getInt`, `getLong`, `getBoolean`, `getDouble`, `getFloat`, `getShort`, `getByte`, `getChar`) that call `Field.getXxx` directly. The fast serialization path uses these for primitive fields, avoiding boxing in `MethodHandle.invoke()`.

### Zero-Allocation Key Matching (Deserialization)

The `nextKeyOrEnd` method in `JsonReader` matches object keys against known field names by comparing characters directly in the input buffer — no `String` allocation, no `HashMap` lookup. When a key matches, the canonical field name is assigned to `stringValue` for identity comparison on subsequent calls. Unknown keys fall back to the normal `readString()` + HashMap path.

This replaces the `hasNextEntry()` + `readKey()` + `lookupMap.get(key)` pattern in the fast path with a single method call per field.

### Precomputed Class Feature Flags

`ClassModel` precomputes boolean flags at construction time:

- `hasAliases` — whether any field has `@JsonAliases`
- `hasRequiredFields` — whether any field has `@JsonRequired`
- `hasDateFields` — whether any field is a date/time type or has `@JsonDateFormat`
- `hasPrimitiveFields` — whether any field is a primitive type
- `hasNestedObjects` — whether any field is an OBJECT type

These flags enable routing to optimized code paths without per-field inspection at runtime.

### Precomputed Metadata

`ClassModel` and `FieldModel` precompute everything at construction time:

- **Field names**: primary JSON name (after naming strategy) and alias array.
- **Alias lookup map**: `Map<String, FieldModel>` for O(1) deserialization key-to-field resolution.
- **Alias presence flag**: avoids per-object alias tracking allocation for classes with no aliases.
- **Annotation flags**: required, includeNull, excludeNull, ignored — all boolean fields, no annotation lookups at runtime.
- **Date type flag**: `DateCodec.isDateType(type)` called once per field, cached as a boolean.
- **Custom date format**: `@JsonDateFormat` value cached per field.
- **Quoted field prefix**: escaped `"name":` text precomputed per field for serialization.
- **Constructor**: `setAccessible(true)` called once, constructor instance cached.

### Cached Date Formatters

`DateTimeFormatter` instances for custom patterns are cached in a `ConcurrentHashMap<String, DateTimeFormatter>`. Repeated serialization/deserialization with the same `@JsonDateFormat` pattern reuses the same formatter instance.

### Cached Pretty-Print Indents

Pre-computed indent strings (`""`, `"  "`, `"    "`, etc.) up to depth 32 are stored in a static array. Pretty-print indentation avoids per-level loop allocation for common depths.

### Hoisted Config Flags

Frequently accessed config values (`failOnCycles`, `prettyPrint`, `includeNulls`, `maxDepth`, `enumMode`) are hoisted into local fields in `ObjectWriter` at construction time, avoiding virtual method calls in the hot serialization loop.

---

## Class Metadata Cache

### Implementation (v1)

```
ConcurrentHashMap<CacheKey(Class<?>, FieldNaming), ClassModel>
```

The cache key includes both the class and the `FieldNaming` strategy, so the same class with different naming strategies produces different cached models.

When KissJson encounters a class for the first time (during serialization or deserialization), it:

1. Walks the class hierarchy (superclass fields first, then declared fields).
2. Filters out static, transient, synthetic, and `@JsonIgnore` fields.
3. Reads annotations: `@JsonName`, `@JsonAliases`, `@JsonIgnore`, `@JsonRequired`, `@JsonIncludeNull`, `@JsonExcludeNull`, `@JsonDateFormat`.
4. Resolves effective field names based on the configured `FieldNaming` strategy.
5. Creates `MethodHandle` getters/setters for each field.
6. Builds a `ClassModel` containing an ordered array of `FieldModel` entries and a precomputed lookup map.
7. Stores the `ClassModel` in a `ConcurrentHashMap`.

Subsequent operations on the same class skip all reflection and annotation scanning entirely.

### Cache Characteristics

| Property | Value |
|---|---|
| Thread safety | `ConcurrentHashMap` — safe for concurrent reads and writes. |
| Growth | Unbounded. Bounded only by the number of distinct class/strategy pairs. |
| Eviction | None in v1. Classes are loaded once and retained. |
| Initialization cost | First use per class: reflection + annotation scan. |
| Steady-state cost | ConcurrentHashMap lookup — near-zero overhead. |

---

## Direct JSON Writer (Serialization)

### Implementation (v1)

The serialization fast path writes directly to a `StringBuilder` without building an intermediate tree:

```
Java Object -> ClassModel (cached) -> iterate FieldModels -> write directly to StringBuilder
```

Steps:

1. Look up `ClassModel` from cache.
2. Iterate over `FieldModel` entries.
3. For each field, read the value from the Java object via cached MethodHandle or Field accessor.
4. Write the precomputed escaped field name and value directly to the `StringBuilder`.
5. Handle type-specific formatting: strings (with optimized escaping), numbers, booleans, null, nested objects, arrays, collections, maps, date/time.

This avoids creating any intermediate node objects. The output is built in a single pass.

### Pretty Print

When pretty printing is enabled, the writer inserts newlines and indentation using pre-cached indent strings. No separate code path is needed.

---

## Token-Based Parser (Deserialization)

### Implementation (v1)

The parser is a hand-written, character-by-character token reader consumed directly by `ObjectReader`:

- **No regex.** Every token is identified by inspecting individual characters.
- **No `String.split`.** Tokens are extracted by tracking start and end positions in the input `char[]`.
- **Efficient number parsing.** Numbers are parsed as `long` when integral (no decimal/exponent), or as `BigDecimal` otherwise. No intermediate `String` objects for integer parsing path.
- **Line/column/offset tracking.** Maintained for error reporting with negligible overhead.
- **Direct typed mapping.** `parse`, `parseList`, and typed `parseMap` map from tokens to Java objects without a `JsonValue` tree.
- **Validated skipping.** Unknown nested values are skipped from the token stream while still enforcing JSON syntax, duplicate-key policy, and max depth.

### Deserialize Path (v1)

For v1, the deserialization path uses a token-to-object fast path:

```
JSON String -> JsonReader tokens -> ObjectReader -> Java Object
```

This keeps the common typed path focused:

1. **No intermediate tree for typed reads.** The mapper consumes each value as the parser reaches it.
2. **Rich errors remain.** Parse errors keep line/column/offset, and mapping errors keep JSON path where possible.
3. **Simple ownership.** `JsonReader` owns JSON syntax; `ObjectReader` owns Java mapping.

### Allocation-Aware Mapping

The typed reader uses a mutable internal JSON path while mapping successful values. Full path strings are created only when building an exception. For homogeneous POJO lists and typed maps, the reader reuses the resolved `ClassModel` across elements/entries instead of doing a cache lookup for every object.

### Simple POJO Fast Path (Deserialization)

When deserializing a simple POJO or list of simple POJOs with default configuration, KissJson uses a specialized path that:

1. Resolves the `ClassModel` once before reading.
2. Pre-allocates the `ArrayList` with capacity for 128 elements for lists (avoids resizing for typical sizes).
3. Uses `MethodHandle.invoke()` for object construction (avoids `InvocationTargetException` wrapping).
4. Uses typed `Field.setInt/setBoolean/setDouble` setters (avoids boxing for primitive fields).
5. Allocates zero tracking structures per object — no `Set`, no `boolean[]`, no `JsonPath` push/restore.
6. Skips all config flag checks per key-value pair (all checks done once before the loop).
7. Matches keys against known field names directly in the input buffer (zero-allocation key matching).

For single-object deserialization (`readSimpleObject`), the same fast path applies. For nested POJO fields, the fast path recurses — nested objects are also deserialized without HashMap lookups or String allocation for keys.

The fast path also avoids the 17-type `readValue()` dispatch chain and the wasteful depth increment/decrement that the general path performs.

This fast path activates when:
- The element type is a POJO (not `Object`, `String`, primitive, enum, etc.)
- `failOnUnknownProperties` is false (default)
- `failOnMissingRequiredFields` is false (default)
- `failOnDuplicateKeys` is false (default)
- `failOnNullForPrimitives` is false (default)
- `maxDepth` is 128 (default)
- The `ClassModel` has no aliases, no required fields, no date fields
- All field types are simple (String, primitives, wrappers, BigDecimal, BigInteger, enum) or nested POJOs

### Fast Serialization Path

When serializing with default configuration (no cycles, no pretty-print, default null handling, default depth), KissJson uses a specialized `writeFastObjectWithModel` path that:

1. Skips `JsonPath` allocation and tracking entirely.
2. Uses typed `Field.getXxx` getters for primitive fields (no boxing).
3. Dispatches on `FieldType` enum directly (no `instanceof` chain per field value).
4. Pre-grows `StringBuilder` via `ensureCapacity` per object.
5. Writes date values (`LocalDate`, `Instant`) directly without intermediate `DateCodec.serialize` call.
6. Uses a lazy `IdentityHashMap` — only allocated when `failOnCycles=true`.

The fast path activates for top-level objects, lists, and maps when:
- `failOnCycles` is false (default)
- `prettyPrint` is false (default)
- `includeNulls` is false (default)
- `maxDepth` is 128 (default)

### Batch-Copy String Escaping

The string escaper (`JsonWriter.escapeString`) uses batch-copy for runs of safe characters within the escape loop. After processing an escape sequence, it scans forward for the next unsafe character and appends the entire safe run with a single `StringBuilder.append(s, start, end)` call. Pre-computed unicode escape strings (`UNICODE_ESCAPES[]`) replace 6 individual `append()` calls per control character.

---

## Cycle Detection

### Implementation (v1)

During serialization, KissJson detects circular references using an `IdentityHashMap<Object, Object>`:

1. Before serializing an object, check if it is already in the visited map.
2. If present, throw `JsonException` with the JSON path.
3. If not present, add it and serialize its fields.
4. After serializing, remove it from the map.

### Characteristics

| Property | Value |
|---|---|
| Data structure | `IdentityHashMap<Object, Object>` — uses `==` for comparison. |
| Scope | Per-serialization call. Created fresh each time. |
| Overhead | One `IdentityHashMap` lookup per nested object. Constant time. |
| Thread safety | Not shared across threads. |

---

## Performance Budget (Target)

These are rough targets for v1. Actual numbers will be measured with JMH benchmarks.

| Operation | Target | Notes |
|---|---|---|
| Serialize simple POJO (5 fields) | < 1 µs | After class metadata is cached. |
| Deserialize simple POJO | < 5 µs | Token-to-object mapping after class metadata is cached. |
| Class metadata first use | < 100 µs | One-time reflection + annotation scan. |
| Class metadata cached lookup | < 100 ns | ConcurrentHashMap get. |
| Parse 1 KB JSON | < 10 µs | Token-based, no regex. |
| Parse 1 MB JSON | < 5 ms | Linear scaling expected. |

These targets are for single-threaded operation on modern hardware. KissJson does not use threads internally.

---

## Future Optimization Opportunities

These optimizations are **not planned for v1** but are documented for future consideration:

### Recycled Buffers

Reuse `StringBuilder` instances and `char[]` buffers across serialization/deserialization calls. Could be implemented with a `ThreadLocal` pool.

### VarHandle Field Access

Replace `MethodHandle` getters/setters with `VarHandle` for direct field access without reflection overhead.

### Lazy String Deserialization

Defer string unescaping until the value is actually read from the tree. This would avoid unnecessary string allocation for skipped fields.

---

## Benchmarks

### JMH Profile

Benchmarks use JMH (Java Microbenchmark Harness) in a separate Maven profile:

```bash
mvn -Pbenchmark clean test-compile
mvn -Pbenchmark exec:exec
```

JMH and Jackson are **test-scope** dependencies activated only in the `benchmark` profile. They are **never** production dependencies and are not on the normal runtime classpath.

To run only the library comparison benchmark:

```bash
mvn -Pbenchmark exec:exec -Djmh.args="JsonLibraryComparisonBenchmark"
```

Normal verification remains independent of benchmarks:

```bash
mvn -B verify
```

### Benchmark Comparison With Jackson

`JsonLibraryComparisonBenchmark` compares KissJson with Jackson `ObjectMapper` under the same JMH settings as `KissJsonBenchmark`:

- average time mode;
- microsecond output;
- 2 warmup iterations;
- 3 measurement iterations;
- 1 fork;
- benchmark-scoped state.

Jackson is configured once during benchmark setup. The `ObjectMapper` uses direct field visibility to match KissJson's field-based mapping and registers `JavaTimeModule` with ISO date/time output for the date/time scenario.

### What Is Benchmarked

| Benchmark | Description |
|---|---|
| KissJson/Jackson serialize simple POJO | 4 fields (String, int, boolean, double) |
| KissJson/Jackson deserialize simple POJO | From the same compact JSON |
| KissJson/Jackson serialize nested POJO | Object with nested object reference |
| KissJson/Jackson deserialize nested POJO | Same nested JSON to object graph |
| KissJson/Jackson serialize date POJO | LocalDate + Instant |
| KissJson/Jackson deserialize date POJO | Same ISO date strings to Java objects |
| KissJson/Jackson serialize list of 100 | Same 100-element POJO list |
| KissJson/Jackson deserialize list of 100 | Same 100-element JSON array |
| KissJson/Jackson stringify map | Same map with mixed value types |
| KissJson/Jackson parse map | Same untyped map JSON |
| KissJson/Jackson escape string | Same heavy string with control chars + Unicode |

The comparison benchmark includes a setup-time correctness check. It verifies that both libraries produce parseable JSON and can round-trip equivalent field values. JSON strings are not compared byte-for-byte because field order and escaping choices may differ.

### Recent Local Benchmark Run

These numbers were measured locally on May 4, 2026 using:

- Apple M4, arm64.
- Temurin JDK 17.
- Command: `mvn -Pbenchmark exec:exec -Djmh.args='JsonLibraryComparisonBenchmark.<benchmark> -wi 10 -i 10 -f 3'`.
- JMH: average time, microseconds/op, 10 warmup iterations, 10 measurement iterations, 3 forks.

Lower is better. Results with **bold** indicate KissJson is faster than Jackson in that scenario.

| Scenario | KissJson | Jackson | Ratio |
|---|---:|---:|---:|
| **Serialize simple POJO** | **0.107** | 0.135 | **0.79x** |
| **Deserialize simple POJO** | **0.147** | 0.196 | **0.75x** |
| Serialize nested POJO | 0.154 | **0.130** | 1.18x |
| **Deserialize nested POJO** | **0.180** | 0.235 | **0.77x** |
| **Serialize date POJO** | **0.174** | 0.174 | **1.00x** |
| **Deserialize date POJO** | **0.532** | 0.563 | **0.94x** |
| **Serialize list of 100 POJOs** | **7.986** | 8.694 | **0.92x** |
| **Deserialize list of 100 POJOs** | **14.432** | 14.738 | **0.98x** |
| **Stringify map** | **0.122** | 0.135 | **0.90x** |
| **Parse map** | **0.185** | 0.245 | **0.76x** |
| **Escape string** | **0.258** | 0.294 | **0.88x** |

KissJson is faster than Jackson on 9 of 11 benchmarks, ties on 1 (serialize date POJO), and is 18% slower on serialize nested POJO. That gap reflects the fundamental overhead of reflection-based field access vs Jackson's ahead-of-time compiled `BeanPropertyWriter`.

For detailed benchmark methodology and historical comparisons, see [Benchmarks](BENCHMARKS.html).

### Process

1. Do not claim performance superiority over Jackson/Gson without measured results.
2. Benchmarks are for internal validation and regression detection.
3. Run on a dedicated machine with no other load for final measurements.
4. Record machine, JDK version, command, date, warmup, measurement, forks, and units when publishing results.
