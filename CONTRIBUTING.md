# Contributing

## Before You Start

Read `AGENTS.md`, `CAVEMAN.md`, `docs/PRODUCT_SPEC.md`, and `.github/architecture/index.md` before making non-trivial changes.

KissJson is a tiny, zero-production-dependency Java 17+ JSON library. Contributions must keep the public API small, preserve field-based mapping, and avoid framework behavior.

## Build

```bash
mvn -B verify
```

For public API, Javadoc, release, security, or documentation-sensitive changes, also run the relevant documented checks:

```bash
mvn -B javadoc:javadoc
mvn -Pbenchmark clean test-compile
mvn -Psecurity verify
mvn -Pquality verify
```

## Rules

- Keep zero production dependencies.
- Preserve Java 17 compatibility.
- Do not add framework integrations, JSONPath, JSON Schema, JSON Patch, validation frameworks, HTTP handling, persistence, routing, dependency injection, logging, telemetry, or service discovery.
- Do not expose internal parser, writer, mapper, cache, or model classes as public API.
- Do not use getters or setters for mapping.
- Update tests, docs, examples, Javadocs, and `CHANGELOG.md` for public behavior changes.
- Do not commit secrets, `target/`, IDE files, local logs, `.DS_Store`, or generated build output.

## Dependency Changes

Production dependencies are not allowed. Test, benchmark, release, security, and build plugins must stay isolated from the published runtime artifact and must be documented when they affect contributor workflows.

## Documentation

Documentation must match the implemented public API. Do not claim planned or unimplemented behavior as working.
