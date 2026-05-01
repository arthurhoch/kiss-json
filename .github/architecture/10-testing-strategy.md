# 10 — Testing Strategy

> **Status:** Current v1 testing contract for `0.1.0-SNAPSHOT`.

## Overview

Every public method in KissJson must have at least one test. Tests are deterministic, require no internet access, and use JUnit 5 exclusively. This document defines the complete test matrix for v1.

## Testing Rules

1. **JUnit 5 only.** No TestNG, no JUnit 4.
2. **Deterministic.** No reliance on time zones, system properties, or network.
3. **No internet access.** Tests must work offline.
4. **Every public method must have at least one test.** No exceptions.
5. **No disabled tests without documented reason.** If a test is `@Disabled`, the reason must be documented in a code comment and tracked as an issue.
6. **No skipped tests.** All tests must pass on every run.

## Test Structure

Tests live in `src/test/java/io/github/arthurhoch/kissjson/`. The test directory mirrors the source directory structure for internal tests. Public API tests are organized by feature.

### Recommended Test Classes

| Test Class | Tests |
|---|---|
| `JsonCreateTest` | `Json.create()`, `Json.create(config)`, `Json.builder()` |
| `JsonConfigTest` | All `JsonConfig` defaults |
| `StringifyTest` | Serialization of all supported types |
| `ParseTest` | Deserialization of all supported types |
| `RoundTripTest` | Serialize then deserialize, verify equality |
| `AnnotationsTest` | All annotation behaviors |
| `NamingStrategiesTest` | All 6 naming strategies |
| `NullHandlingTest` | Include/exclude nulls, per-field overrides |
| `UnknownPropertiesTest` | Ignore by default, fail when configured |
| `DuplicateKeysTest` | Last wins by default, fail when configured |
| `MaxDepthTest` | Exceeding depth on parse and serialize |
| `CycleDetectionTest` | Circular references |
| `ParseErrorTest` | `JsonParseException` with line/column/offset |
| `MappingErrorTest` | `JsonMappingException` with JSON path |
| `ParseListTest` | `parseList()` for various element types |
| `ParseMapTest` | `parseMap()` for various key/value types |
| `PrivateFieldTest` | Private fields are mapped correctly |
| `SuperclassFieldsTest` | Inherited fields are included |
| `PrivateConstructorTest` | Objects with private no-arg constructors |
| `FieldFilteringTest` | Static, transient, synthetic fields are ignored |
| `PrettyPrintTest` | Pretty printing output format |
| `DateTimeTest` | All date/time types with all format options |
| `EnumModeTest` | `NAME` and `TO_STRING` enum modes |

## Complete Test Matrix

### 1. API Construction

| Test | Description |
|---|---|
| `createWithDefaults` | `Json.create()` uses all defaults |
| `createWithConfig` | `Json.create(config)` uses provided config |
| `builderReturnsJsonBuilder` | `Json.builder()` returns a `JsonBuilder` |
| `builderChainAllMethods` | Chain all builder methods, verify config |

### 2. JsonConfig Defaults

| Test | Description |
|---|---|
| `defaultFieldNaming` | `IDENTITY` |
| `defaultIncludeNulls` | `true` |
| `defaultFailOnUnknownProperties` | `false` |
| `defaultFailOnMissingRequiredFields` | `false` |
| `defaultFailOnNullForPrimitives` | `false` |
| `defaultFailOnDuplicateKeys` | `false` |
| `defaultFailOnCycles` | `true` |
| `defaultMaxDepth` | `128` |
| `defaultPrettyPrint` | `false` |
| `defaultDateFormat` | `ISO` |
| `defaultZoneId` | `UTC` |
| `defaultEnumMode` | `NAME` |

### 3. Stringify (Serialization)

| Test | Description |
|---|---|
| `stringifyNull` | `null` → `"null"` |
| `stringifyString` | `"hello"` → `"\"hello\""` |
| `stringifyStringEscaping` | Escaping of `"`, `\`, control chars, unicode |
| `stringifyInt` | `42` → `"42"` |
| `stringifyLong` | `9999999999L` → correct output |
| `stringifyDouble` | `3.14` → `"3.14"` |
| `stringifyFloat` | `1.5f` → correct output |
| `stringifyShort` | Short value → correct output |
| `stringifyByte` | Byte value → correct output |
| `stringifyBoolean` | `true` → `"true"`, `false` → `"false"` |
| `stringifyChar` | `'a'` → `"\"a\""` |
| `stringifyIntegerWrapper` | `Integer.valueOf(42)` → `"42"` |
| `stringifyDoubleWrapper` | `Double.valueOf(3.14)` → correct output |
| `stringifyBooleanWrapper` | `Boolean.TRUE` → `"true"` |
| `stringifyCharacterWrapper` | `Character.valueOf('x')` → `"\"x\""` |
| `stringifyBigDecimal` | Large decimal → correct output |
| `stringifyBigInteger` | Large integer → correct output |
| `stringifyEnum` | `MyEnum.VALUE` → `"\"VALUE\""` |
| `stringifyEnumToString` | With `EnumMode.TO_STRING`, uses `.toString()` |
| `stringifyIntArray` | `new int[]{1, 2, 3}` → `"[1,2,3]"` |
| `stringifyStringArray` | `new String[]{"a", "b"}` → `["a","b"]` |
| `stringifyObjectArray` | Nested objects in array |
| `stringifyList` | `List<String>` → JSON array |
| `stringifyListOfObjects` | `List<MyObject>` → array of objects |
| `stringifyMap` | `Map<String, Integer>` → JSON object |
| `stringifyMapWithObjectValues` | `Map<String, MyObject>` → object of objects |
| `stringifyNestedObject` | Object with nested object field |
| `stringifyDeeplyNestedObject` | Multiple levels of nesting |
| `stringifyNullFieldIncluded` | With `includeNulls=true`, null field appears |
| `stringifyNullFieldExcluded` | With `includeNulls=false`, null field absent |
| `stringifyEmptyObject` | Object with no serializable fields → `"{}"` |
| `stringifyEmptyList` | Empty list → `"[]"` |
| `stringifyEmptyMap` | Empty map → `"{}"` |
| `stringifyEmptyString` | `""` → `"\"\""` |

### 4. Parse (Deserialization)

| Test | Description |
|---|---|
| `parseNull` | `"null"` → `null` |
| `parseString` | `"\"hello\""` → `"hello"` |
| `parseStringUnescaping` | Escaped sequences decoded correctly |
| `parseInt` | `"42"` → `42` (into `int` field) |
| `parseLong` | Large number into `long` field |
| `parseDouble` | `"3.14"` → `3.14` |
| `parseFloat` | Parse into `float` field |
| `parseShort` | Parse into `short` field |
| `parseByte` | Parse into `byte` field |
| `parseBoolean` | `"true"` → `true`, `"false"` → `false` |
| `parseChar` | `"\"a\""` → `'a'` |
| `parseIntegerWrapper` | Parse into `Integer` field |
| `parseDoubleWrapper` | Parse into `Double` field |
| `parseBooleanWrapper` | Parse into `Boolean` field |
| `parseCharacterWrapper` | Parse into `Character` field |
| `parseBigDecimal` | Parse into `BigDecimal` field |
| `parseBigInteger` | Parse into `BigInteger` field |
| `parseEnum` | `"\"VALUE\""` → `MyEnum.VALUE` |
| `parseEnumToString` | With `EnumMode.TO_STRING` |
| `parseIntArray` | `[1, 2, 3]` → `int[]` |
| `parseStringArray` | `["a", "b"]` → `String[]` |
| `parseObjectArray` | Array of objects |
| `parseList` | JSON array → `List<String>` |
| `parseListOfObjects` | JSON array of objects → `List<MyObject>` |
| `parseMap` | JSON object → `Map<String, Integer>` |
| `parseMapWithObjectValues` | JSON object of objects → `Map<String, MyObject>` |
| `parseNestedObject` | Nested JSON object → nested Java object |
| `parseDeeplyNestedObject` | Multiple nesting levels |
| `parseNullField` | JSON `null` → Java `null` field |
| `parseNullFieldPrimitiveDefault` | JSON `null` for primitive → default value (0, false) |
| `parseEmptyObject` | `{}` → object with default field values |
| `parseEmptyArray` | `[]` → empty list/array |
| `parseEmptyString` | `""` as string field |

### 5. Round-Trip (Serialize → Deserialize)

| Test | Description |
|---|---|
| `roundTripString` | String in, String out |
| `roundTripInt` | int in, int out |
| `roundTripDouble` | double in, double out |
| `roundTripBoolean` | boolean in, boolean out |
| `roundTripBigDecimal` | BigDecimal in, BigDecimal out |
| `roundTripBigInteger` | BigInteger in, BigInteger out |
| `roundTripEnum` | Enum in, Enum out |
| `roundTripObject` | Simple object with mixed fields |
| `roundTripNestedObject` | Nested object |
| `roundTripList` | List of objects |
| `roundTripMap` | Map of objects |
| `roundTripArray` | Primitive and object arrays |
| `roundTripWithNamingStrategy` | Object with `SNAKE_CASE` round-trips correctly |
| `roundTripWithAliases` | Object serialized with name, deserialized via alias |

### 6. Annotations

| Test | Description |
|---|---|
| `jsonNameRenamesField` | `@JsonName("custom_name")` → field appears as `"custom_name"` in JSON |
| `jsonNameOverridesNamingStrategy` | `@JsonName` takes priority over `FieldNaming` |
| `jsonAliasesDeserializesMultipleNames` | `@JsonAliases({"old", "legacy"})` accepts both names |
| `jsonAliasesSerializeUsesPrimaryName` | Serialization uses the primary name, not aliases |
| `jsonIgnoreExcludesField` | `@JsonIgnore` → field absent from JSON |
| `jsonIgnoreExcludesFromDeserialize` | `@JsonIgnore` → JSON value is not mapped |
| `jsonRequiredPresent` | Required field present → no error |
| `jsonRequiredMissingFailEnabled` | Missing required field + `failOnMissingRequiredFields=true` → `JsonMappingException` |
| `jsonRequiredMissingFailDisabled` | Missing required field + `failOnMissingRequiredFields=false` → field gets default |
| `jsonIncludeNullOverridesGlobal` | `@JsonIncludeNull` on a field includes null even when global `includeNulls=false` |
| `jsonExcludeNullOverridesGlobal` | `@JsonExcludeNull` on a field excludes null even when global `includeNulls=true` |
| `jsonDateFormatCustom` | `@JsonDateFormat("dd/MM/yyyy")` → uses custom format for that field |
| `jsonDateFormatOverridesGlobal` | `@JsonDateFormat` overrides global `DateFormat` |

### 7. Naming Strategies

All six naming strategies must be tested:

| Test | Description |
|---|---|
| `identityNaming` | `userName` → `"userName"` |
| `snakeCaseNaming` | `userName` → `"user_name"` |
| `upperSnakeCaseNaming` | `userName` → `"USER_NAME"` |
| `camelCaseNaming` | `UserName` (already camel) → `"userName"` (lower camel) |
| `upperCamelCaseNaming` | `userName` → `"UserName"` |
| `kebabCaseNaming` | `userName` → `"user-name"` |

Each strategy tested with:
- Simple field name
- Acronym-heavy name (`xmlHttpRequest` → varies by strategy)
- Single-letter field name
- Already-matching field name (no-op)

### 8. Null Handling

| Test | Description |
|---|---|
| `includeNullsGlobal` | `includeNulls=true` → null fields in JSON |
| `excludeNullsGlobal` | `includeNulls=false` → null fields absent |
| `includeNullPerField` | `@JsonIncludeNull` overrides global `false` |
| `excludeNullPerField` | `@JsonExcludeNull` overrides global `true` |
| `nullInList` | `[1, null, 3]` → `List<Integer>` with null element |
| `nullInMap` | `{"key": null}` → `Map` with null value |
| `nullPrimitiveDefault` | JSON null for `int` field → `0` |
| `nullPrimitiveFail` | JSON null for `int` + `failOnNullForPrimitives=true` → exception |
| `nullWrapperField` | JSON null for `Integer` field → `null` |

### 9. Unknown Properties

| Test | Description |
|---|---|
| `unknownPropertyIgnoredByDefault` | Extra JSON field → silently ignored |
| `unknownPropertyFailWhenConfigured` | Extra JSON field + `failOnUnknownProperties=true` → `JsonMappingException` |
| `unknownPropertyWithAlias` | JSON field matches alias → not unknown |

### 10. Duplicate Keys

| Test | Description |
|---|---|
| `duplicateKeysLastWinsDefault` | `{"a":1,"a":2}` → `a=2` |
| `duplicateKeysFailWhenConfigured` | `{"a":1,"a":2}` + `failOnDuplicateKeys=true` → `JsonParseException` |

### 11. Max Depth

| Test | Description |
|---|---|
| `parseWithinDepth` | Nesting within limit → success |
| `parseExceedsDepth` | Nesting exceeds `maxDepth` → `JsonParseException` |
| `serializeWithinDepth` | Object graph within limit → success |
| `serializeExceedsDepth` | Object graph exceeds `maxDepth` → `JsonMappingException` |
| `customMaxDepth` | `maxDepth=5` → fails at depth 6 |

### 12. Cycle Detection

| Test | Description |
|---|---|
| `cycleDetectedByDefault` | Circular reference + `failOnCycles=true` → `JsonMappingException` |
| `cycleDetectionErrorMessage` | Error message includes JSON path |
| `noCycleInList` | Same object appears twice in a list (not a cycle, just a reference) — should succeed or skip based on config |

### 13. Parse Errors

| Test | Description |
|---|---|
| `parseErrorLineColumn` | Malformed JSON → `JsonParseException` with correct `line()` and `column()` |
| `parseErrorOffset` | Malformed JSON → correct `offset()` |
| `parseErrorMessage` | Message contains useful detail |
| `parseEmptyInput` | `""` → `JsonParseException` |
| `parseNullInput` | `null` input → `JsonParseException` or `NullPointerException` |
| `parseUnterminatedString` | Missing closing `"` → error with location |
| `parseUnterminatedObject` | Missing `}` → error with location |
| `parseUnterminatedArray` | Missing `]` → error with location |
| `parseInvalidToken` | `tru` → error with location |
| `parseTrailingComma` | `{"a":1,}` → error (strict JSON) |
| `parseMissingColon` | `{"a" 1}` → error with location |
| `parseInvalidNumber` | `01` or `1.2.3` → error |

### 14. Mapping Errors

| Test | Description |
|---|---|
| `mappingErrorJsonPath` | Type mismatch → `JsonMappingException` with correct `jsonPath()` |
| `mappingErrorTargetType` | Error includes correct `targetType()` |
| `mappingErrorFieldName` | Error includes correct `fieldName()` |
| `mappingErrorTypeMismatch` | String where int expected → error |
| `mappingErrorCauseChained` | Underlying exception available via `getCause()` |
| `mappingErrorNullForPrimitive` | Null for `int` with strict mode → error |
| `mappingErrorUnknownProperty` | Unknown property with strict mode → error |
| `mappingErrorMissingRequired` | Missing required field → error |

### 15. `parseList`

| Test | Description |
|---|---|
| `parseListStrings` | `["a","b","c"]` → `List<String>` |
| `parseListIntegers` | `[1,2,3]` → `List<Integer>` |
| `parseListObjects` | Array of JSON objects → `List<MyObject>` |
| `parseListEmpty` | `[]` → empty `List` |
| `parseListNested` | `[[1,2],[3,4]]` → `List<List<Integer>>` |
| `parseListWithNulls` | `[1,null,3]` → `List<Integer>` with null |

### 16. `parseMap`

| Test | Description |
|---|---|
| `parseMapStringInteger` | `{"a":1,"b":2}` → `Map<String, Integer>` |
| `parseMapStringObject` | JSON object of objects → `Map<String, MyObject>` |
| `parseMapEmpty` | `{}` → empty `Map` |
| `parseMapWithNulls` | `{"a":null}` → `Map` with null value |
| `parseMapEnumKeys` | `{"ACTIVE":1}` → `Map<MyEnum, Integer>` |
| `parseMapValueType` | `parseMap(json, String.class, Integer.class)` correctly typed |

### 17. Private Field Support

| Test | Description |
|---|---|
| `privateFieldSerialized` | Private field is included in JSON |
| `privateFieldDeserialized` | Private field is set from JSON |

### 18. Superclass Fields

| Test | Description |
|---|---|
| `superclassFieldsIncluded` | Fields from parent class appear in JSON |
| `superclassFieldsDeserialized` | Parent class fields are set from JSON |
| `multiLevelInheritance` | Grandparent → parent → child, all fields included |

### 19. Private No-Arg Constructor

| Test | Description |
|---|---|
| `privateConstructorWorks` | Object with private no-arg constructor deserializes correctly |

### 20. Field Filtering

| Test | Description |
|---|---|
| `staticFieldIgnored` | `static` fields are not serialized or deserialized |
| `transientFieldIgnored` | `transient` fields are not serialized or deserialized |
| `syntheticFieldIgnored` | Compiler-generated synthetic fields are ignored |

### 21. Pretty Print

| Test | Description |
|---|---|
| `prettyPrintObject` | Pretty-printed JSON has proper indentation |
| `prettyPrintArray` | Pretty-printed array has proper formatting |
| `prettyPrintNested` | Deeply nested pretty print is correct |
| `prettyPrintDisabled` | `prettyPrint=false` → compact output |

### 22. Date/Time Types

All 10 date/time types tested with ISO format, `EPOCH_MILLIS`, and `EPOCH_SECONDS` (where applicable):

| Type | ISO Format | EPOCH_MILLIS | EPOCH_SECONDS |
|---|---|---|---|
| `LocalDate` | `"2026-04-29"` | N/A | N/A |
| `LocalTime` | `"14:30:00"` | N/A | N/A |
| `LocalDateTime` | `"2026-04-29T14:30:00"` | N/A | N/A |
| `OffsetDateTime` | `"2026-04-29T14:30:00+02:00"` | epoch millis | epoch seconds |
| `ZonedDateTime` | `"2026-04-29T14:30:00+02:00[Europe/Paris]"` | epoch millis | epoch seconds |
| `Instant` | `"2026-04-29T12:30:00Z"` | epoch millis | epoch seconds |
| `Duration` | ISO-8601 duration | N/A | N/A |
| `Period` | ISO-8601 period | N/A | N/A |
| `Date` | ISO (with timezone) | epoch millis | epoch seconds |
| `Calendar` | ISO (with timezone) | epoch millis | epoch seconds |

For each type and format:
- **Serialize** — Verify correct JSON output
- **Deserialize** — Verify correct Java object
- **Round-trip** — Serialize then deserialize, verify equality

### 23. `EnumMode`

| Test | Description |
|---|---|
| `enumModeName` | `EnumMode.NAME` → uses `Enum.name()` |
| `enumModeToString` | `EnumMode.TO_STRING` → uses `Enum.toString()` |
| `enumDeserializeByName` | JSON `"VALUE"` maps to `MyEnum.VALUE` |
| `enumDeserializeByToString` | JSON matches `toString()` output |

## Test Data Guidelines

- Use **realistic** test data, not `foo`, `bar`, `baz` when meaningful data is more readable.
- Use **varied** field names to test naming strategies thoroughly.
- Use **edge cases**: empty strings, very long strings, special characters, boundary numbers.
- Use **deterministic** values. No `System.currentTimeMillis()` or `UUID.randomUUID()` in tests.
- Each test must be **independent**. No shared mutable state between tests.

## Test Naming Convention

Use descriptive method names that state what is being tested:

```java
@Test
void shouldSerializeNullFieldWhenIncludeNullsIsTrue() { ... }

@Test
void shouldThrowJsonParseExceptionForMalformedJson() { ... }

@Test
void shouldMapSnakeCaseFieldsWhenConfigured() { ... }
```

Or use a simpler style:

```java
@Test
void stringifyNullField() { ... }

@Test
void parseErrorReportsLineAndColumn() { ... }

@Test
void snakeCaseNamingStrategy() { ... }
```

Either style is acceptable as long as it is consistent within each test class.

## Continuous Integration

All tests run on every push and pull request via `mvn -B verify`. No test may be skipped or disabled without a documented reason.
