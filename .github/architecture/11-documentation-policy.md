# 11 — Documentation Policy

> **Status:** Current documentation policy for `0.1.0-SNAPSHOT`.

## Overview

Documentation is a first-class concern in KissJson. Docs must be accurate, current, and honest. This policy defines what documentation exists, who it is for, how it is maintained, and what happens when the public API changes.

## Documentation Principles

1. **All docs in English.** No exceptions.
2. **Docs must match the implemented public API.** If the code and docs disagree, the code is correct and the docs must be updated.
3. **Do not claim features work if they are not implemented.** Honesty over marketing.
4. **Use "Intended v1 contract"** for planned API that is not yet implemented.
5. **Use "Planned for v1 implementation"** for planned features that are not yet built.
6. **Every public class and method must have Javadoc.** No public API without documentation.

## Documentation Inventory

### User-Facing Documentation (`docs/`)

Served via GitHub Pages. The primary user-facing documentation.

| File | Purpose |
|---|---|
| `docs/index.md` | Landing page, overview, quick start |
| `docs/GETTING_STARTED.md` | Installation, first usage |
| `docs/API.md` | Complete public API reference |
| `docs/CONFIGURATION.md` | `JsonConfig` options and defaults |
| `docs/ERROR_HANDLING.md` | Exception hierarchy and error messages |
| `docs/EXAMPLES.md` | Common usage patterns |
| `docs/PERFORMANCE.md` | Performance strategy |
| `docs/BENCHMARKS.md` | Benchmark methodology and results |
| `docs/MAVEN_CENTRAL.md` | Maven Central publication guide |
| `docs/RELEASE.md` | Release process |
| `docs/_config.yml` | Jekyll configuration |

### Architecture Documentation (`.github/architecture/`)

Internal design documentation. Not served by GitHub Pages directly, but linked from `docs/`.

| File | Purpose |
|---|---|
| `00-product-purpose.md` | Product purpose |
| `01-public-api-contract.md` | Public API design |
| `02-kiss-rules.md` | KISS constraints |
| `03-core-architecture.md` | Core architecture |
| `04-json-parser.md` | Parser architecture |
| `05-json-writer.md` | Writer architecture |
| `06-object-mapping.md` | Object mapping rules |
| `07-configuration-model.md` | Configuration model |
| `08-error-handling.md` | Error handling design |
| `09-performance-strategy.md` | Performance strategy |
| `10-testing-strategy.md` | Testing strategy |
| `11-documentation-policy.md` | This file |
| `12-release-and-maven-central.md` | Release and Maven Central |
| `13-github-pages.md` | GitHub Pages setup |
| `index.md` | Architecture index with reading order |

### Governance Files

| File | Purpose |
|---|---|
| `AGENTS.md` | Primary AI agent instructions (root level) |
| `CAVEMAN.md` | Compact project summary (root level) |
| `CHANGELOG.md` | Change log (root level) |
| `README.md` | Project README (root level) |
| `SECURITY.md` | Security policy (root level) |
| `ALL_MARKDOWN.md` | Master list of all markdown files |
| `CONTRIBUTING.md` | Contribution guidelines |
| `PULL_REQUEST_TEMPLATE.md` | PR template |
| `workflows/ci.yml` | CI workflow |
| `workflows/pages.yml` | GitHub Pages workflow |
| `workflows/release-maven-central.yml` | Release workflow |

### Source Code Documentation

- **Javadoc on every public class and method.** No exceptions.
- **Package-info.java** for each public package.
- No Javadoc required on internal classes, but encouraged for complex logic.

## Documentation Maintenance Rules

### When the Public API Changes

If any public API changes, the following must be updated:

1. **All affected doc files.** Any documentation that references the changed API.
2. **All affected examples.** Code examples in docs must compile and work.
3. **`CHANGELOG.md`.** Every change to public API must be recorded under `Unreleased`.
4. **Javadoc.** Update the Javadoc on the changed class/method.
5. **Tests.** Update or add tests for the changed behavior.

### Change Protocol

```
1. Make the code change
2. Update Javadoc on changed public API
3. Update affected docs/ files
4. Update affected examples
5. Update CHANGELOG.md
6. Update tests
7. Run mvn -B verify
8. Verify docs are consistent with code
```

### ChangeLog Format

`CHANGELOG.md` follows the [Keep a Changelog](https://keepachangelog.com/) format:

```markdown
## [Unreleased]

### Added
- New feature descriptions

### Changed
- Changed behavior descriptions

### Fixed
- Bug fix descriptions

### Removed
- Removed feature descriptions
```

## Unimplemented Feature Marking

When documenting features that are planned but not yet implemented:

### In Architecture Docs

Use a status banner at the top of the file:

```markdown
> **Status:** Intended v1 contract. Not yet implemented.
```

Or:

```markdown
> **Status:** Planned for v1 implementation.
```

### In User-Facing Docs

Use a clear note:

```markdown
> **Note:** This feature is planned for v1 but not yet implemented.
```

### In Javadoc

Use `@apiNote` tag:

```java
/**
 * Serializes the given object to a JSON string.
 * @apiNote Intended v1 contract. Not yet implemented.
 */
```

### In README

If a feature is listed in the README but not yet implemented, it must be in a separate section:

```markdown
## Planned Features (v1)

- [ ] Custom date formats
- [ ] Pretty printing
```

Do NOT list unimplemented features in the "Features" section as if they work.

## Style Guide

### Markdown

- Use GitHub-Flavored Markdown (GFM).
- Use fenced code blocks with language tags.
- Use tables for structured data.
- Use headings hierarchically (`#` → `##` → `###`).
- Keep paragraphs short.
- Use bold for emphasis on key terms, not italics.

### Javadoc

- First sentence is a summary (period-terminated).
- Use `@param` for every parameter.
- Use `@return` for non-void methods.
- Use `@throws` for every declared exception.
- Use `@see` for related methods/classes.
- Use `@apiNote` for implementation status.
- Include code examples in Javadoc where helpful.

Example:

```java
/**
 * Serializes the given Java object to a JSON string.
 *
 * <p>Example:</p>
 * <pre>{@code
 * String json = json.stringify(user);
 * }</pre>
 *
 * @param value the object to serialize; may be {@code null}
 * @return the JSON string representation
 * @throws JsonMappingException if a cycle is detected and failOnCycles is true
 * @see #parse(String, Class)
 */
public String stringify(Object value) { ... }
```

### Code Examples in Docs

- Must be **complete enough** to understand the usage.
- Must use the **current public API** (no internal classes).
- Must be **correct** — ideally copy-pasted from working test code.
- Must include **imports** if the class is not obvious.

## Master File List

All markdown files in the repository must be listed in `.github/ALL_MARKDOWN.md`. When a new markdown file is created, it must be added to this list. This ensures no documentation is orphaned or forgotten.

## Review Checklist

Before merging any change that affects documentation:

- [ ] Javadoc updated for all changed public API
- [ ] `docs/` files updated for all changed behavior
- [ ] Examples updated and verified to compile
- [ ] `CHANGELOG.md` updated under `Unreleased`
- [ ] Unimplemented features marked correctly
- [ ] No claims of working features that are not implemented
- [ ] `ALL_MARKDOWN.md` updated if new files were added
- [ ] Spelling and grammar checked
