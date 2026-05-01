# Implementation Plan

> Phased implementation plan for KissJson v1. Each phase is designed to be a small, testable increment.
> Current status: an initial v1 implementation exists for `0.1.0-SNAPSHOT`; this roadmap remains as the historical phase breakdown and should be reconciled as hardening continues.
> Current hardening direction: typed deserialization uses a token-to-object fast path (`JsonReader` -> `ObjectReader`) rather than building a `JsonValue` tree.

---

## Phase 1: Project Skeleton and Governance — **COMPLETE**

Set up the Maven project structure, governance documents, CI/CD, and documentation framework.

**Scope:**
- Maven `pom.xml` with Java 17, JUnit 5, no production dependencies.
- Package structure: `io.github.arthurhoch.kissjson` (public) and `io.github.arthurhoch.kissjson.internal` (internal).
- `CAVEMAN.md`, `AGENTS.md`, `PRODUCT_SPEC.md`.
- Architecture docs in `.github/architecture/`.
- CI workflow (GitHub Actions): build, test, verify.
- Release workflow: tag-based, GPG signing, Sonatype Central Publisher Portal.
- `CHANGELOG.md`, `SECURITY.md`, `LICENSE`.
- This documentation pass.

**Status:** ✅ Complete

---

## Phase 2: Public API Classes

Define all public API classes as shells — method signatures with Javadoc, no implementation.

**Scope:**
- `Json` — main entry point: `stringify()`, `parse()`, `parseList()`, `parseMap()`, static `create()` and `builder()`.
- `JsonBuilder` — fluent builder for configuring `Json` instances.
- `JsonConfig` — immutable configuration object: naming strategy, null handling, unknown properties, duplicate keys, max depth, pretty print, date format, enum mode.
- `FieldNaming` — enum: `IDENTITY`, `LOWER_CASE`, `UPPER_CASE`, `CAMEL_CASE`, `SNAKE_CASE`, `KEBAB_CASE`.
- `DateFormat` — enum: `ISO`, `EPOCH_MILLIS`, `EPOCH_SECONDS`.
- `EnumMode` — enum: `NAME`, `TO_STRING`. No ordinal support in v1.
- Annotations: `@JsonName`, `@JsonAliases`, `@JsonIgnore`, `@JsonRequired`, `@JsonIncludeNull`, `@JsonExcludeNull`, `@JsonDateFormat`.
- Exceptions: `JsonException`, `JsonParseException`, `JsonMappingException`.
- Javadoc for every public method.
- Unit tests for configuration construction and immutability.

**Estimated scope:** Small. Mostly interfaces, enums, and data classes.

---

## Phase 3: JSON Lexer/Parser

Implement the hand-written, token-based JSON parser.

**Scope:**
- `JsonReader` / `JsonParser` (internal): character-by-character parser producing internal tokens.
- Token types: `BEGIN_OBJECT`, `END_OBJECT`, `BEGIN_ARRAY`, `END_ARRAY`, `COLON`, `COMMA`, `STRING`, `NUMBER`, `BOOLEAN`, `NULL`, `EOF`.
- Line, column, offset tracking throughout.
- Strict RFC 8259 validation.
- `JsonParseException` with line/column/offset for all syntax errors.
- Duplicate key detection (configurable: error, keep first, keep last).
- No regex. No `String.split`.
- Unit tests: valid JSON (all value types, nested, arrays, unicode, escaping), invalid JSON (truncated, malformed, trailing content).

**Estimated scope:** Medium. The lexer is the most complex internal component.

---

## Phase 4: JSON Writer

Implement direct `StringBuilder`-based JSON writer.

**Scope:**
- `JsonWriter` (internal): writes JSON values directly to `StringBuilder`.
- Compact output mode.
- Pretty print mode (newlines + indentation).
- String escaping per RFC 8259.
- All JSON value types: object, array, string, number, boolean, null.
- No intermediate tree for the serialization fast path.
- Unit tests: all value types, escaping, pretty print, nested structures.

**Estimated scope:** Medium. Straightforward but requires careful escaping.

---

## Phase 5: Internal JSON Value Model

Define the internal `JsonValue` type hierarchy for limited generic representation. It is not public API and is not used by the typed object fast path.

**Scope:**
- `JsonValue` (internal, sealed or abstract): base type.
- `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`, `JsonObject`, `JsonArray`.
- `JsonObject`: `LinkedHashMap<String, JsonValue>` — preserves insertion order.
- `JsonArray`: `ArrayList<JsonValue>`.
- Type-safe accessor methods: `asString()`, `asInt()`, `asLong()`, `asDouble()`, `asBoolean()`, `asObject()`, `asArray()`.
- Package-private visibility. Users never see these types.
- Unit tests: construction, accessors, type coercion, immutability where applicable.

**Estimated scope:** Small. Simple data classes.

---

## Phase 6: Basic Primitive/String/Null Mapping

Implement mapping for the simplest types: primitives, wrappers, String, null.

**Scope:**
- `ObjectReader` (internal): reads parser tokens and maps to Java types.
- `ObjectWriter` (internal): reads Java field values and delegates to `JsonWriter`.
- Supported types: `int`/`Integer`, `long`/`Long`, `double`/`Double`, `float`/`Float`, `boolean`/`Boolean`, `String`, `null`.
- Null handling: skip null fields by default, include if configured or annotated.
- Type mismatch errors with `JsonMappingException`.
- Unit tests: each type, null handling, type mismatches.

**Estimated scope:** Small–Medium. Establishes the mapping pattern.

---

## Phase 7: Class and Field Metadata Cache

Implement `ClassModel`, `FieldModel`, and `ClassModelCache`.

**Scope:**
- `FieldModel` (internal): wraps a `java.lang.reflect.Field` with precomputed metadata (JSON name, aliases, annotations, naming strategy result).
- `ClassModel` (internal): wraps a `Class<?>` with an ordered list of `FieldModel` entries.
- `ClassModelCache` (internal): `ConcurrentHashMap<Class<?>, ClassModel>`. Thread-safe, no eviction.
- Field discovery: all declared fields (excluding `Object` fields, static fields, synthetic fields).
- Annotation processing: `@JsonName`, `@JsonAliases`, `@JsonIgnore`, `@JsonRequired`, `@JsonIncludeNull`, `@JsonExcludeNull`, `@JsonDateFormat`.
- Naming strategy application: resolve effective JSON field name.
- `setAccessible(true)` called at construction time.
- Unit tests: cache creation, field discovery, annotation processing, naming strategy resolution, thread safety.

**Estimated scope:** Medium. Foundation for all object mapping.

---

## Phase 8: Object Serialization

Implement full object serialization: iterate fields, write values.

**Scope:**
- Connect `ClassModel` + `ObjectWriter` + `JsonWriter`.
- Serialize: look up `ClassModel`, iterate `FieldModel` entries, read field value, write to `JsonWriter`.
- Handle all types from Phase 6 (primitives, wrappers, String, null).
- Pretty print support (via `JsonWriter` flag).
- `@JsonIgnore`: skip the field.
- `@JsonIncludeNull` / `@JsonExcludeNull`: per-field null control.
- Unit tests: simple POJO, null fields, ignored fields, pretty print.

**Estimated scope:** Small. Mostly wiring existing components.

---

## Phase 9: Object Deserialization

Implement full object deserialization: read tokens, map to fields.

**Scope:**
- Connect `JsonReader` + `ObjectReader` + `ClassModel`.
- Deserialize: read JSON tokens, look up `ClassModel`, resolve each object key by precomputed field names and aliases, and set field values directly.
- Handle all types from Phase 6 (primitives, wrappers, String, null).
- `@JsonName`: map from alternate JSON key.
- `@JsonAliases`: try multiple JSON keys in order.
- `@JsonIgnore`: skip the field.
- `@JsonRequired`: throw `JsonMappingException` if field is missing and `failOnMissingRequiredFields` is enabled.
- Unknown properties: skip, warn, or error (configurable).
- JSON path tracking for error messages.
- Unit tests: simple POJO, missing fields, unknown properties, aliases, required fields.

**Estimated scope:** Medium. Error handling and edge cases.

---

## Phase 10: Arrays, List, Map Support

Add support for arrays, `List`, and `Map` types.

**Scope:**
- Array serialization/deserialization: `int[]`, `String[]`, `MyObject[]`, etc.
- `List` serialization/deserialization: `List<String>`, `List<Integer>`, `List<MyObject>`, etc.
- `Map` serialization/deserialization: `Map<String, ?>`. JSON object keys are always strings.
- Generic type resolution: extract element types from field generic signatures.
- `parseList()` and `parseMap()` handle top-level generic containers directly.
- Nested collections: `List<List<String>>`, `Map<String, List<Integer>>`.
- Unit tests: each collection type, nested collections, empty collections, null elements.

**Estimated scope:** Medium. Generic type resolution is the tricky part.

---

## Phase 11: Nested Object Support (Recursive Mapping)

Enable objects containing other objects.

**Scope:**
- Serialize nested objects: recursively serialize field values that are objects.
- Deserialize nested objects: recursively map `JsonObject` values to field types.
- Ensure `ClassModelCache` is populated for nested types.
- JSON path tracking through nested levels.
- Unit tests: simple nesting, deep nesting, null nested objects, mixed nesting with collections.

**Estimated scope:** Small. Mostly recursive application of existing logic.

---

## Phase 12: Date/Time Support

Add support for all 10 date/time types.

**Scope:**
- Types: `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetDateTime`, `ZonedDateTime`, `Instant`, `Duration`, `Period`, `Date`, `Calendar`.
- `DateCodec` (internal): converts between date/time objects and string representations.
- Default format: ISO 8601 for all types.
- `@JsonDateFormat`: per-field format override (pattern string).
- `DateFormat` enum: `ISO` (default), `EPOCH_MILLIS`, `EPOCH_SECONDS`.
- `Date` and `Calendar`: serialize as ISO 8601 string or epoch millis.
- Unit tests: each type, ISO format, custom format, epoch millis, null dates.

**Estimated scope:** Medium. 10 types, each with serialization and deserialization.

---

## Phase 13: Annotation Support

Complete support for all v1 annotations.

**Scope:**
- `@JsonName`: already supported in Phase 9. Verify completeness.
- `@JsonAliases`: already supported in Phase 9. Verify completeness.
- `@JsonIgnore`: already supported in Phase 8/9. Verify completeness.
- `@JsonRequired`: already supported in Phase 9. Verify completeness.
- `@JsonIncludeNull`: already supported in Phase 8. Verify completeness.
- `@JsonExcludeNull`: already supported in Phase 8. Verify completeness.
- `@JsonDateFormat`: already supported in Phase 12. Verify completeness.
- Integration tests: all annotations combined, edge cases, priority rules.
- Documentation: verify Javadoc matches behavior.

**Estimated scope:** Small. Mostly verification and integration testing.

---

## Phase 14: Naming Strategies

Implement all 6 `FieldNaming` strategies.

**Scope:**
- `IDENTITY`: no transformation.
- `LOWER_CASE`: `userName` → `username`.
- `UPPER_CASE`: `userName` → `USERNAME`.
- `CAMEL_CASE`: `user_name` → `userName`.
- `SNAKE_CASE`: `userName` → `user_name`.
- `KEBAB_CASE`: `userName` → `user-name`.
- `NamingStrategy` (internal): pure functions, no regex.
- Integration with `ClassModelCache`: apply strategy when building `FieldModel`.
- `@JsonName` overrides naming strategy.
- Unit tests: each strategy, edge cases (acronyms, consecutive capitals, single character), override by `@JsonName`.

**Estimated scope:** Small. Six pure functions.

---

## Phase 15: Null/Unknown/Duplicate-Key Handling

Implement config-driven behavior for edge cases.

**Scope:**
- Null handling:
  - Serialize: include or exclude null fields (default: include). Configurable globally and per-field via `@JsonIncludeNull`/`@JsonExcludeNull`.
  - Deserialize: null JSON value for primitive field → error (configurable: error or default value).
- Unknown properties:
  - `failOnUnknownProperties = false` (default): silently ignore unknown JSON fields.
  - `failOnUnknownProperties = true`: throw `JsonMappingException` with the property name.
- Duplicate keys:
  - `failOnDuplicateKeys = false` (default): last value wins (standard `LinkedHashMap` behavior).
  - `failOnDuplicateKeys = true`: throw `JsonParseException` on duplicate key.
- Unit tests: each strategy, combinations, defaults.

**Estimated scope:** Small–Medium. Three independent features.

---

## Phase 16: Error Hardening

Ensure all errors are rich, informative, and properly chained.

**Scope:**
- Audit all `JsonParseException` throws: verify line/column/offset are always populated.
- Audit all `JsonMappingException` throws: verify `jsonPath()`, `targetType()`, `fieldName()`, `expectedType()`, `actualValue()` are populated where applicable.
- Exception chaining: ensure every underlying exception is set as `cause`.
- Error messages: review all messages for clarity and consistency.
- JSON path tracking: verify path is accurate through nested objects and arrays.
- Edge cases: empty input, whitespace-only input, null input, extremely large numbers, deeply nested structures.
- Unit tests: verify exception fields for a comprehensive set of error scenarios.

**Estimated scope:** Medium. Systematic audit of all error paths.

---

## Phase 17: Cycle Detection and Max Depth Enforcement

Add safety limits for circular references and deep nesting.

**Scope:**
- Cycle detection during serialization:
  - `IdentityHashMap<Object, Object>` per serialization call.
  - Before serializing an object, check if already visited.
  - Throw `JsonMappingException` with JSON path and type if cycle detected.
- Max depth enforcement:
  - During serialization: increment depth on nested object/array, check against limit.
  - During deserialization: increment depth on nested object/array, check against limit.
  - Default: 128. Configurable via `JsonConfig.maxDepth()`.
  - Throw `JsonMappingException` with JSON path when exceeded.
- Unit tests: simple cycle, indirect cycle, self-reference, max depth boundary, configurable depth.

**Estimated scope:** Small. Well-defined, isolated feature.

---

## Phase 18: Pretty Print Support

Complete pretty print support for serialization.

**Scope:**
- `JsonConfig.prettyPrint()` configuration.
- Indentation: 2 spaces (default) or configurable.
- Newline handling: `\n` (default).
- Output formatting: object fields on separate lines, array elements on separate lines, proper indentation.
- Pretty print with nested objects and arrays.
- Unit tests: simple objects, nested objects, arrays, mixed nesting, empty objects/arrays.

**Estimated scope:** Small. Mostly formatting logic in `JsonWriter`.

---

## Phase 19: Performance Pass

Review hotspots and optimize where needed.

**Scope:**
- Profile the library with representative payloads.
- Identify and optimize hot paths.
- Review `StringBuilder` usage: ensure adequate initial capacity.
- Review `ConcurrentHashMap` usage: verify no contention issues.
- Review string escaping: ensure no unnecessary allocation.
- Review typed deserialization for accidental `JsonValue` tree construction.
- Consider `VarHandle` or `MethodHandle` for field access (if benchmarks justify).
- Verify no regressions in correctness tests.
- Record benchmark results for future reference.

**Estimated scope:** Small–Medium. Depends on profiling results.

---

## Phase 20: Unit Test Completion, Documentation Review, Release Readiness

Final validation before Maven Central publication.

**Scope:**
- **Test audit**: verify every public method has at least one test. Check `docs/PRODUCT_SPEC.md` test matrix.
- **Test quality**: review tests for determinism, independence, clarity.
- **Documentation audit**: verify Javadoc matches implementation. Verify docs match code.
- **CHANGELOG.md**: verify all changes are documented.
- **Public API audit**: verify no internal classes are exposed. Verify package structure.
- **Dependency audit**: verify zero production dependencies in `pom.xml`.
- **Java version audit**: verify code compiles and runs on Java 17 only. No preview features.
- **README.md**: verify installation and usage examples are correct.
- **Maven Central readiness**: verify `pom.xml` has all required metadata (name, description, URL, licenses, developers, scm). Verify GPG signing works. Verify Sonatype Central Portal workflow works.
- **CI/CD**: verify build, test, and release workflows pass on GitHub Actions.

**Estimated scope:** Medium. Thorough but mostly verification.

---

## Summary

| Phase | Description | Status | Est. Scope |
|---|---|---|---|
| 1 | Project skeleton and governance | ✅ Complete | — |
| 2 | Public API classes | Implemented | Small |
| 3 | JSON lexer/parser | Implemented | Medium |
| 4 | JSON writer | Implemented | Medium |
| 5 | Internal JSON value model | Not used in typed fast path | Small |
| 6 | Basic primitive/string/null mapping | Implemented | Small–Medium |
| 7 | Class and field metadata cache | Implemented | Medium |
| 8 | Object serialization | Implemented | Small |
| 9 | Object deserialization | Implemented | Medium |
| 10 | Arrays, List, Map support | Implemented | Medium |
| 11 | Nested object support | Implemented | Small |
| 12 | Date/time support | Implemented | Medium |
| 13 | Annotation support | Implemented | Small |
| 14 | Naming strategies | Implemented | Small |
| 15 | Null/unknown/duplicate-key handling | Implemented | Small–Medium |
| 16 | Error hardening | Implemented | Medium |
| 17 | Cycle detection and max depth | Implemented | Small |
| 18 | Pretty print support | Implemented | Small |
| 19 | Performance pass | Implemented | Small–Medium |
| 20 | Test completion, docs review, release readiness | In hardening | Medium |

Total estimated phases: 20. Phases 2–20 are implementation phases. Phase 1 is complete.
