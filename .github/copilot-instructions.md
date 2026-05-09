# GitHub Copilot Instructions

## Library

KissJson is a tiny, high-performance, zero-dependency Java 17+ JSON library.
It serializes Java objects to JSON and deserializes JSON to Java objects using fields directly.

## Public API Package

`io.github.arthurhoch.kissjson`

## Public API Classes

- `Json` — facade with `create()`, `builder()`, `stringify()`, `parse()`, `parseList()`, `parseMap()`
- `JsonBuilder` — builder for configured `Json` instances
- `JsonConfig` — immutable configuration
- `JsonException`, `JsonParseException`, `JsonMappingException` — error hierarchy
- `FieldNaming` — enum: IDENTITY, LOWER_CASE, UPPER_CASE, CAMEL_CASE, SNAKE_CASE, KEBAB_CASE
- `DateFormat` — enum: ISO, EPOCH_MILLIS, EPOCH_SECONDS
- `EnumMode` — enum: NAME, TO_STRING
- Annotations: `@JsonName`, `@JsonAliases`, `@JsonIgnore`, `@JsonRequired`, `@JsonIncludeNull`, `@JsonExcludeNull`, `@JsonDateFormat`

## Internal Package

`io.github.arthurhoch.kissjson.internal` — never expose as public API.

## Constraints

- Java 17 only. No higher. No preview features.
- Zero production dependencies.
- No Lombok. No annotation processing. No code generation.
- No getters/setters for field mapping. Use reflection.
- No regex in the parser. No `String.split`.
- Field-based mapping only. Include superclass fields. Ignore static, transient, synthetic.
- Private fields supported via `setAccessible(true)`.

## Defaults

Forgiving by default: unknown properties ignored, missing fields OK, null primitives keep Java defaults, duplicate keys last wins, cycles fail, max depth 128.

## Testing

JUnit 5. Every public method must have at least one test. Tests must be deterministic and not require internet.

## Style

- Simple, readable code over clever code.
- Every exception must be informative with context.
- When in doubt, choose the simpler solution.

## Versioned AI Skills

Before creating a release tag, read `.github/skills-release-policy.md` and update the versioned Markdown skill artifacts under `docs/skills/`. Add a new `docs/skills/vX.Y.Z.md` file, update `docs/skills/index.md`, keep older skill files, and verify the complete public API/member index for the release.
