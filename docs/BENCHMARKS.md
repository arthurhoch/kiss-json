---
layout: default
title: Benchmarks
---

# Benchmarks

> **Measured results.** Numbers below were measured with JMH on the hardware and JDK described. Do not generalize without re-running on your own hardware.

## Methodology

- **JMH** (Java Microbenchmark Harness) in a separate Maven profile.
- **Jackson** `ObjectMapper` with direct field visibility and `JavaTimeModule` for comparison.
- Both libraries run under identical JMH settings in the same JVM process.
- Correctness is verified at benchmark setup: both libraries must round-trip equivalent field values.

### How to Run

```bash
# Full comparison benchmark
mvn -Pbenchmark clean test-compile exec:exec \
  -Djmh.args='JsonLibraryComparisonBenchmark -wi 5 -i 5 -f 2'

# Single benchmark with more precision
mvn -Pbenchmark clean test-compile exec:exec \
  -Djmh.args='JsonLibraryComparisonBenchmark.kissJsonDeserializePojoList100 -wi 10 -i 10 -f 3'

# Normal build (no JMH needed)
mvn -B verify
```

Jackson is a **test-scope** dependency activated only in the `benchmark` profile. It is never a production dependency.

---

## What Is Benchmarked

| Benchmark | Description | POJO Fields |
|---|---|---|
| Simple POJO | 4 fields: `String name`, `int age`, `boolean active`, `double score` | 4 |
| Nested POJO | 3 fields: `String name`, `int value`, `NestedPojo nested` | 3 |
| Date POJO | 2 fields: `LocalDate localDate`, `Instant instant` | 2 |
| List of 100 POJOs | 100 elements of Simple POJO | 400 total |
| Map | `Map<String, Object>` with mixed value types | 4 entries |
| Escape string | Heavy string with control chars (0-31) and Unicode | 1 |

---

## Latest Results

Measured May 1, 2026 on Apple M4, Temurin JDK 21.0.11.

Settings: `averageTime`, microseconds/op, 5 warmup iterations, 5 measurement iterations, 2 forks.

### Serialization

| Scenario | KissJson (us/op) | Jackson (us/op) | KissJson / Jackson |
|---|---:|---:|---:|
| Simple POJO | 0.169 | 0.195 | **0.87x** |
| Nested POJO | 0.251 | 0.199 | 1.26x |
| Date POJO | 0.307 | 0.264 | 1.16x |
| List of 100 POJOs | 12.300 | 12.491 | **0.98x** |
| Map | 0.208 | 0.210 | **0.99x** |
| Escape string | 0.608 | 0.460 | 1.32x |

### Deserialization

| Scenario | KissJson (us/op) | Jackson (us/op) | KissJson / Jackson |
|---|---:|---:|---:|
| Simple POJO | 0.293 | 0.310 | **0.94x** |
| Nested POJO | 0.431 | 0.368 | 1.17x |
| Date POJO | 0.811 | 0.816 | **0.99x** |
| List of 100 POJOs | 27.080 | 22.961 | 1.18x |
| Map | 0.281 | 0.354 | **0.79x** |

**Bold** ratios below 1.0x indicate KissJson is faster than Jackson.

---

## Optimization History

### Pass 3 (May 1, 2026) — Simple POJO Fast Path

**Changes:**

1. **MethodHandle constructors** — `ClassModel` caches `MethodHandle` for no-arg constructors. `ObjectReader` uses `MethodHandle.invoke()` instead of `Constructor.newInstance()`, eliminating `InvocationTargetException` wrapping.
2. **Typed primitive field setters** — `FieldModel` provides `setInt`, `setLong`, `setBoolean`, `setDouble`, etc. using `Field.setXxx` directly. Avoids boxing for primitive fields in the fast path.
3. **Simple POJO fast path** — `readSimplePojoList` for POJO lists with default config: zero per-object allocations, no `Set` tracking, no `JsonPath` push/restore, no config flag checks per key-value pair, pre-sized `ArrayList`.
4. **Precomputed class feature flags** — `ClassModel` computes `hasAliases`, `hasRequiredFields`, `hasDateFields`, `hasPrimitiveFields`, `hasNestedObjects` at construction time. Enables routing to optimized paths.

**Results vs previous pass:**

| Scenario | Before | After | Delta |
|---|---:|---:|---:|
| Deserialize Simple POJO | 0.311 | 0.293 | -5.8% |
| Deserialize Nested POJO | 0.466 | 0.431 | -7.5% |
| Deserialize PojoList100 | 29.959 | 27.080 | -9.6% |
| Deserialize Date POJO | 0.866 | 0.811 | -6.4% |
| Serialize Simple POJO | 0.180 | 0.169 | -6.1% |
| Serialize PojoList100 | 13.696 | 12.300 | -10.2% |
| Escape string | 0.640 | 0.608 | -5.0% |

### Pass 2 (April 30, 2026) — FieldType Dispatch and Streaming Tokenizer

**Changes:**

1. **Streaming tokenizer** — Replaced tree-based `JsonParser`/`JsonValue` with `JsonReader` token stream. No intermediate tree for typed reads.
2. **`FieldType` enum dispatch** — `FieldModel` precomputes field type (STRING, INT, BOOLEAN, DOUBLE, etc.). `readFieldValue` uses a switch on `FieldType` instead of a chain of `if (type == X.class)` checks.
3. **Lazy `JsonPath`** — Mutable `Object[]` stack instead of `StringBuilder`. Path string built only on exception.
4. **`skipKey()` for unknown properties** — Avoids `String` allocation when skipping unknown keys (when `checkDuplicates=false`).
5. **Direct `longValue()` for integers** — Avoids `BigDecimal` allocation for integral number fields.

**Results vs Pass 1:**

| Scenario | Before | After | Delta |
|---|---:|---:|---:|
| Deserialize PojoList100 | 46.382 | 29.959 | -35.4% |
| Deserialize Simple POJO | 0.462 | 0.311 | -32.7% |
| Deserialize Nested POJO | 0.542 | 0.466 | -14.0% |

### Pass 1 (April 30, 2026) — Initial Baseline

Initial JMH measurements establishing the baseline before optimization work.

---

## Per-Element Cost Breakdown

For PojoList100 deserialization (100 SimplePojo objects, 4 fields each):

| Cost Component | per element | Notes |
|---|---|---|
| Object construction | ~15 ns | `MethodHandle.invoke()` for no-arg constructor |
| Field setting (4 fields) | ~80 ns | `Field.setInt/setBoolean/setDouble` for primitives, `Field.set` for String |
| Token reading (4 keys + 4 values) | ~120 ns | `JsonReader.readKey()`, `nextToken()`, `longValue()` etc. |
| HashMap lookup (4 keys) | ~30 ns | `lookupMap.get(key)` per field |
| ArrayList.add | ~5 ns | Pre-sized, no resizing |
| Other (loop overhead) | ~20 ns | `hasNextEntry()`, `nextEntryOrEnd()` |
| **Total** | **~270 ns** | **27 us for 100 elements** |

The remaining gap vs Jackson (~23 us) comes from Jackson's use of bytecode generation for field access, which eliminates the `Field.setXxx` reflection call overhead entirely.

---

## Environment Notes

- Results are for **single-threaded** operation. KissJson does not use threads internally.
- Class metadata is cached before measurement (first-use cost is not included).
- The JVM is fully warmed up (5+ JMH warmup iterations).
- Do not compare numbers across different hardware, OS, or JDK versions.
- Error bars are typically 2-10% of the mean. Use longer runs (more forks, more iterations) for release-quality conclusions.
