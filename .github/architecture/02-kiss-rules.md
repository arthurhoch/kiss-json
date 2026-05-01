# 02 — KISS Rules

This document defines the non-negotiable constraints for KissJson. Every rule exists to keep the library small, simple, and predictable. These rules are **intended v1 contract** and apply to all implementation.

KISS means **Keep It Simple, Stupid**. In the Unix tradition, software should be small, understandable, maintainable, composable, and focused. A good tool does one thing well instead of becoming a monolithic framework. Complexity must be justified, not assumed.

For KissJson, the one thing is:

> Serialize JSON and deserialize JSON for normal Java objects using fields directly.

Everything else is secondary. If a proposed feature makes KissJson harder to understand, harder to compose, or harder to keep dependency-free, it must be rejected or deferred unless it solves a common JSON use case clearly.

---

## Rule 0: Do One Thing Well

KissJson exists to convert between JSON text and normal Java object graphs. It must not grow into unrelated application infrastructure.

KissJson must not handle:

- HTTP clients or servers.
- Persistence, database access, or repositories.
- Validation frameworks or schema systems.
- Routing, dependency injection, service discovery, logging, or telemetry.
- JSONPath, JSON Patch, or binary JSON formats.
- Framework integration for Spring, Quarkus, Micronaut, Jakarta, CDI, or similar runtimes.

**Rationale:** A focused JSON library composes well with other tools. A library that absorbs unrelated concerns becomes a framework.

---

## Rule 1: Zero Production Dependencies

KissJson must ship with zero external dependencies in its compiled JAR. This means:

- No `pom.xml` `<dependencies>` with `compile` or `runtime` scope (except `test` scope for JUnit 5).
- No shading or relocation of third-party code.
- No optional dependencies that degrade functionality when missing.
- No reflection libraries, no bytecode manipulation libraries, no caching libraries.
- The only imports in production code are `java.*` and `jdk.*` packages.

**Rationale:** A JSON library should not pull in a dependency tree. It should be one JAR that works everywhere.

---

## Rule 2: Java 17 Only

- Source level: Java 17.
- Target level: Java 17.
- No higher version features (no unnamed patterns from Java 21, no string templates from Java 23, etc.).
- No preview features enabled.
- `--release 17` in the compiler configuration.

**Rationale:** Java 17 is the current LTS baseline. Staying on 17 ensures maximum compatibility.

---

## Rule 3: Field-Based Mapping — No Getters, No Setters

Object mapping reads and writes object fields directly using reflection. This means:

- `Field.setAccessible(true)` is called for private fields.
- No getter method is ever invoked during serialization.
- No setter method is ever invoked during deserialization.
- No JavaBean property introspection is used.
- No `PropertyDescriptor`, no `Introspector`, no `BeanInfo`.

If a field cannot be made accessible (e.g., due to a SecurityManager), a `JsonMappingException` is thrown with the class name, field name, and the security exception as the cause.

**Rationale:** Fields are the source of truth. Getters and setters add indirection, can contain side effects, and diverge from the actual data.

---

## Rule 4: No Framework Integration

KissJson does not integrate with any framework:

- No Spring `@Component`, `@Bean`, or auto-configuration.
- No Jakarta / JAX-RS provider.
- No Micronaut factory.
- No Quarkus extension.
- No CDI, no SPI, no service loader for user extensions.
- No classpath scanning.
- No modules, mixins, views, or framework-style extension registry.
- No module system (`module-info.java` is not required in v1).

Users who want framework integration should wrap KissJson themselves.

**Rationale:** Framework integration couples the library to framework versions and conventions. KissJson stays independent.

---

## Rule 5: No Internal Class Exposure

All internal classes must be package-private (no `public` modifier) within `io.github.arthurhoch.kissjson.internal`. This means:

- Users cannot import internal classes (they are not visible outside their package).
- Users cannot cast to internal types.
- Users cannot extend internal classes.
- The public API surface is exactly what is defined in [01-public-api-contract.md](01-public-api-contract.md).

**Rationale:** A small public API is a stable public API. Internal freedom to refactor is essential.

---

## Rule 6: No Regex, No String.split in Parser

The JSON parser must not use:

- `java.util.regex.Pattern` or `String.matches()`.
- `String.split()` for tokenization.
- Any regex-based approach to parsing.

The parser is a hand-written, character-by-character recursive descent parser. It reads a `char[]` array, tracks an offset, and advances character by character.

**Rationale:** Regex parsers are hard to debug, hard to extend, and have surprising performance characteristics. A character-level parser is transparent and correct.

---

## Rule 7: Rich Errors with Context

Every exception must include enough context for the developer to fix the problem immediately:

- **Parse errors** (`JsonParseException`): message, line number, column number, character offset.
- **Mapping errors** (`JsonMappingException`): JSON path (`$.user.address.city`), target class, field name, expected type, actual value, root cause.
- **Base errors** (`JsonException`): message and cause chain.

Error messages must be:
- Human-readable (not serialized stack traces).
- Specific (not "unexpected error" or "mapping failed").
- Actionable (tell the developer what to fix, not just what broke).

Exceptions must never swallow the root cause. Always chain with `initCause()` or pass to the constructor.

**Rationale:** Unhelpful error messages are the single biggest complaint about existing JSON libraries. KissJson must be better.

---

## Rule 8: Safe Defaults

Default behavior must be forgiving and practical:

| Behavior | Default | Strict Alternative |
|----------|---------|---------------------|
| Unknown JSON properties | Silently ignored | `failOnUnknownProperties(true)` throws |
| Missing JSON fields | Java field keeps default value | `@JsonRequired` + `failOnMissingRequiredFields(true)` throws |
| Null for primitive fields | Keep Java default (0, false, '\0') | `failOnNullForPrimitives(true)` throws |
| Duplicate keys in JSON | Last value wins | `failOnDuplicateKeys(true)` throws |
| Reference cycles | Throw with clear message | Always on by default (`failOnCycles(true)`) |
| ISO date/time formatting | Enabled | `dateFormat(...)` selects another supported mode |
| Timezone for legacy date types | UTC | `zoneId(...)` selects another zone |
| Enum representation | `Enum.name()` | `enumMode(EnumMode.TO_STRING)` |
| Max nesting depth | 128 levels | Configurable via `maxDepth()` |

**Rationale:** Libraries that throw by default on common situations (extra fields, null values) annoy developers. Safe defaults mean the library works out of the box.

---

## Rule 9: Prefer Simple Over Clever

When faced with a design choice:

- Choose the implementation that is easiest to read.
- Choose the approach with the fewest moving parts.
- Choose the solution that handles 95% of cases well rather than 100% of cases with complexity.
- Avoid premature optimization. Optimize only after profiling.
- Avoid abstraction layers that exist "just in case."
- Avoid configurable strategies when one good default suffices.
- Prefer composition over built-in integration.

Do not add rare framework-like features in v1:

- Custom serializers/deserializers.
- Polymorphic type metadata.
- Object identity/reference handling.
- `$id` / `$ref`.
- Mixins, views, modules, service loaders, or classpath scanning.
- Automatic getter/setter mapping.
- Annotation processing or code generation.

**Examples of simple over clever:**

- A `StringBuilder`-based writer is simpler than an `Appendable` abstraction with multiple implementations.
- A `ConcurrentHashMap` cache is simpler than a custom cache with eviction policies.
- Direct field access via reflection is simpler than code-generated accessors.

---

## Rule 10: Developer Time Over Machine Time

Performance matters, but not at the cost of a confusing architecture or public API. Internal optimizations are encouraged only when they preserve the simple user model:

```java
Json json = Json.create();
String text = json.stringify(user);
User user = json.parse(text, User.class);
```

The following optimizations are acceptable because they stay internal:

- Metadata caching.
- Direct writing.
- Token-based parsing.
- Precomputed field names and aliases.
- No regex parser.
- No `String.split` parser.
- No repeated annotation scanning.

The following are not acceptable as user-facing complexity:

- Public parser, token, class model, field model, cache, or codec APIs.
- Performance knobs that normal users must understand before serializing JSON.
- Multiple public ways to perform the same common operation.

---

## Rule 11: Every Public Method Has Javadoc and Tests

- Every `public` method in the public API must have Javadoc that describes what it does, its parameters, return value, and any exceptions thrown.
- Every `public` method must have at least one test case.
- Javadoc must include `@param`, `@return`, and `@throws` tags where applicable.
- Tests must be deterministic and not require internet access.
- Tests use JUnit 5 only.

**Rationale:** Untested code is broken code. Undocumented code is unusable code.

---

## Rule 12: Public API Changes Update Docs, Tests, and Changelog

When any public API changes:

1. Update Javadoc on the changed method/class.
2. Update or add tests covering the change.
3. Update `CHANGELOG.md` under the `Unreleased` section.
4. Update architecture docs if the change affects contracts.
5. Update `docs/PRODUCT_SPEC.md` if the change affects the product specification.
6. Verify the change does not break existing tests.

**Rationale:** Coordinated updates prevent documentation drift and test gaps.

---

## Rule 13: No Lombok, Annotation Processing, or Code Generation

The codebase must not use:

- **Lombok** or any annotation processor that modifies bytecode.
- **`javac` annotation processing** for generating code.
- **Code generation tools** of any kind (except standard `javac` compilation).
- **`annotationProcessorPaths`** in the Maven configuration.

All code is written by hand. All boilerplate is written by hand.

**Rationale:** Generated code is invisible code. It makes debugging harder, adds build complexity, and violates the KISS principle. If boilerplate exists, write it explicitly so it can be read and understood.

---

## Enforcement

These rules are non-negotiable for v1. If a rule conflicts with a feature, the feature is simplified or removed — not the rule. Any PR that violates a KISS rule must be revised before merging.
