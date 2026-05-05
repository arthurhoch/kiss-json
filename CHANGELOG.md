# Changelog

All notable changes to KissJson will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

## [0.1.0] - 2026-05-05

### Changed

- Performance: KissJson now beats Jackson on 9 of 11 benchmarks (was 4 of 11).
- Performance: Zero-allocation key matching in deserialization fast path — matches field names directly in input buffer without String allocation or HashMap lookup.
- Performance: Fast deserialization path for single POJOs and nested POJOs (previously only applied to POJO lists).
- Performance: Fast serialization path with typed primitive getters (no boxing), FieldType-based dispatch, and direct date writing.
- Performance: Batch-copy safe character runs in string escaping with pre-computed unicode escape table.
- Performance: Lazy `IdentityHashMap` allocation — only allocated when cycle detection is enabled.
- Performance: `readIntegralLong` returns directly when number was already parsed as long (avoids BigDecimal/BigInteger).
- Performance: Skip ObjectWriter overhead for String/Character serialization.
- Performance: `LocalDate.toString()`/`Instant.toString()` written directly to output for ISO date serialization.
- Performance: `DateCodec.serializeISO` reordered to check most common date types first.

### Added

- Project foundation and Maven build configuration (`pom.xml`).
- Apache License 2.0 (`LICENSE.txt`).
- Security policy (`SECURITY.md`).
- Project governance files (`AGENTS.md`, `CAVEMAN.md`).
- GitHub governance: AI rules, Copilot instructions, markdown index.
- GitHub Actions workflows: CI, CodeQL, Pages, Maven Central release.
- Dependabot configuration for Maven and GitHub Actions updates.
- Semgrep GitHub Actions workflow for separate Java/security scanning.
- OWASP Dependency-Check Maven `security` profile.
- SpotBugs Maven `quality` profile for optional static quality scanning.
- Security scanning documentation (`docs/SECURITY_SCANNING.md`) and release-readiness checklist items.
- Architecture documentation (`.github/architecture/`).
- GitHub Pages documentation skeleton (`docs/`).
- Product specification document.
- Intended public API contract documentation.
- Implementation plan documentation.
- Review checklist documentation.
- Release and Maven Central publication guide.
- Initial v1 JSON parser with line, column, offset, duplicate-key handling, and max-depth enforcement.
- Initial v1 JSON writer with compact output, pretty output, string escaping, null root handling, and cycle detection.
- Field-based object serialization and deserialization with metadata caching.
- Public API classes, enums, annotations, and exception hierarchy.
- Support for primitives, wrappers, String, BigDecimal, BigInteger, char/Character, enums, arrays, List, Map, nested objects, and null.
- Support for date/time types: LocalDate, LocalTime, LocalDateTime, OffsetDateTime, ZonedDateTime, Instant, Duration, Period, Date, Calendar.
- Annotation support: @JsonName, @JsonAliases, @JsonIgnore, @JsonRequired, @JsonIncludeNull, @JsonExcludeNull, @JsonDateFormat.
- Naming strategies: IDENTITY, LOWER_CASE, UPPER_CASE, CAMEL_CASE, SNAKE_CASE, KEBAB_CASE.
- Null handling, unknown property handling, duplicate key handling, max depth, cycle detection, rich parse errors, and rich mapping errors.
- JUnit 5 implementation test suite.

### Changed

- Kept package-private internals hidden behind a private `RuntimeBridge` in `Json.java`.
- Optimized string escaping: manual hex table instead of `String.format` in escape loops.
- Cached `MethodHandle` getters/setters in `FieldModel` with `Field.get/set` fallback.
- Precomputed alias lookup map in `ClassModel` for O(1) field resolution during deserialization.
- Cached `DateTimeFormatter` per custom pattern in `DateCodec` via `ConcurrentHashMap`.
- Hoisted frequently-accessed config flags into local fields in `ObjectWriter`.
- Pre-cached pretty-print indent strings to avoid per-level allocation.
- Precomputed date-type flag per field in `FieldModel`.
- Precomputed escaped field-name prefixes in `FieldModel` for direct serialization.
- Added a string escaping fast path that writes strings without escape-worthy characters in one append and writes valid non-control Unicode directly.
- Reused resolved `ClassModel` metadata for homogeneous POJO list serialization and typed POJO list/map deserialization.
- Replaced hot-path JSON path string concatenation in object mapping and serialization with mutable internal path builders that materialize strings only for exceptions.
- Avoided per-object alias tracking allocation for classes without `@JsonAliases`.
- Avoided numeric scanning for ISO date/time serialization output.
- Updated the Sonatype Central Publishing Maven plugin used by the release profile.

### Added

- JMH benchmark profile (`mvn -Pbenchmark exec:exec`) with 11 benchmark scenarios.
- Benchmark-only Jackson comparison using the same JMH settings, data model, and payload shapes as the KissJson benchmark.
- Performance test suite (7 tests for repeated calls, escaping, caching, large lists).
- Comprehensive performance documentation in `docs/PERFORMANCE.md`.

### Fixed

- Fixed benchmark profile compilation by adding generated JMH annotation sources to the test source roots.
- Aligned `@JsonRequired` enforcement with `failOnMissingRequiredFields`.
- Fixed non-BMP Unicode and `\uXXXX` surrogate-pair parsing/serialization.
- Fixed max-depth checks to fail only when nesting exceeds the configured limit.
- Preserved JSON path context for date/time mapping failures.
- Kept internal parser, value model, class model, field model, cache, naming, date codec, reader, and writer classes out of generated public Javadocs.
- Rejected decimal and overflowing numbers when mapping to integer-only target types.
- Rejected multi-character and empty strings when mapping to `char`/`Character`.
- Rejected non-string map keys during JSON object serialization.
- Threaded `JsonConfig` through an internal simple-POJO list fallback path found by SpotBugs.

[0.1.0]: https://github.com/arthurhoch/kiss-json/releases/tag/v0.1.0
