# Review Checklist

> Checklists for implementation, release, and publication of KissJson.

---

## Before Implementation

Use this checklist before starting any implementation phase.

- [ ] Read `CAVEMAN.md` — compact project summary.
- [ ] Read `AGENTS.md` — AI agent instructions and rules.
- [ ] Read `docs/PRODUCT_SPEC.md` — authoritative v1 scope and contract.
- [ ] Read `.github/architecture/index.md` — architecture reading order.
- [ ] Read `docs/IMPLEMENTATION_PLAN.md` — phased plan and current status.
- [ ] Understand the v1 scope — what is included and what is excluded.
- [ ] Understand the v1 non-goals — things explicitly not in v1.
- [ ] Understand the KISS rules — zero deps, Java 17, field-based, no regex parser.
- [ ] Understand the public API contract — which classes are public, what methods exist.
- [ ] Understand the error model — exception hierarchy, required fields on exceptions.
- [ ] Understand the performance strategy — caching, direct writing, token-based parsing, MethodHandle field access, optimized escaping, date formatter caching.
- [ ] Plan the phase — identify what to implement, what to test, what docs to update.
- [ ] Check if the phase depends on a previous phase that is not yet complete.
- [ ] Verify no production dependencies will be added.
- [ ] Verify no internal classes will be exposed as public API.
- [ ] For performance work, verify the normal build still passes without benchmark dependencies and benchmark-only dependencies remain isolated to the benchmark profile.

### KISS Review Questions

Ask these before accepting any new feature, option, abstraction, or public API surface:

- [ ] Does this keep KissJson focused on JSON only?
- [ ] Does this convert between JSON and normal Java object graphs, or does it drift into framework behavior?
- [ ] Does this add another way to do something already supported?
- [ ] Does this make the public API harder to memorize?
- [ ] Does this introduce HTTP, persistence, validation, routing, dependency injection, logging, telemetry, schema, JSONPath, JSON Patch, or framework integration?
- [ ] Does this expose internals such as parser tokens, caches, class models, field models, codecs, or type converters?
- [ ] Does this add a production dependency?
- [ ] Does this solve a common user need, or only a rare edge case?
- [ ] Can this be deferred?
- [ ] Can this be solved through existing configuration?
- [ ] Would a normal Java developer understand this without reading long documentation?
- [ ] Does the implementation keep Java 17 compatibility and avoid preview features?
- [ ] Does the documentation clearly mark intended v1 behavior as planned until implementation is complete?

---

## Before Release

Use this checklist before tagging a release.

### Code Quality

- [ ] All public methods have Javadoc.
- [ ] Javadoc accurately describes behavior, parameters, return values, and exceptions.
- [ ] No `TODO` or `FIXME` comments in production code.
- [ ] No commented-out code in production files.
- [ ] Code follows Java 17 conventions (no preview features).
- [ ] No production dependencies in `pom.xml` (check `<dependencies>` section).
- [ ] No internal classes exposed as `public` (check package structure).
- [ ] No getters/setters used for field mapping (reflection-based access only).

### Testing

- [ ] All public methods have at least one test.
- [ ] Tests are deterministic (no random, no network, no time-dependent).
- [ ] Tests pass: `mvn -B verify` succeeds.
- [ ] Tests cover error paths (exception scenarios).
- [ ] Tests cover edge cases (null, empty, boundary values).

### Documentation

- [ ] `CHANGELOG.md` updated under `[Unreleased]`.
- [ ] `README.md` reflects current API and usage.
- [ ] All docs match the implemented public API.
- [ ] No docs claim features work that are not implemented.
- [ ] Javadoc and docs are consistent (no contradictions).

### Build

- [ ] `mvn -B verify` passes cleanly (no warnings treated as errors? check config).
- [ ] `mvn -B verify` does not require secrets (GPG, Sonatype, etc.).
- [ ] CI workflow passes on GitHub Actions.
- [ ] No new dependencies added without explicit approval.

### Security Readiness

- [ ] CodeQL workflow exists and runs on push, pull request, and schedule.
- [ ] Dependabot config exists for Maven and GitHub Actions.
- [ ] Semgrep workflow exists and runs separately from normal CI.
- [ ] OWASP Dependency-Check `security` Maven profile exists.
- [ ] Normal CI does not require vulnerability database downloads.
- [ ] Release workflow does not require security scans to access publishing secrets.
- [ ] No production dependencies were added.
- [ ] No secrets, tokens, passwords, or private keys are committed.
- [ ] No dynamic class loading from JSON data.
- [ ] No JavaScript engines.
- [ ] No unsafe deserialization mechanism or polymorphic type metadata.
- [ ] Parser max-depth checks exist.
- [ ] Security docs are updated, including `docs/SECURITY_SCANNING.md`.

### API Surface

- [ ] Public API package is `io.github.arthurhoch.kissjson`.
- [ ] Only approved classes are public: `Json`, `JsonBuilder`, `JsonConfig`, `JsonException`, `JsonParseException`, `JsonMappingException`, `FieldNaming`, `DateFormat`, `EnumMode`, annotations.
- [ ] Internal package `io.github.arthurhoch.kissjson.internal` contains no public classes.
- [ ] No public method was added without updating docs, tests, and changelog.
- [ ] No public method was removed or had its signature changed without updating docs, tests, and changelog.

---

## Before Maven Central Publication

Use this checklist before publishing to Maven Central for the first time.

### Prerequisites

- [ ] Implementation and tests are complete (all 20 phases).
- [ ] `mvn -B verify` passes.
- [ ] All "Before Release" checklist items are done.

### GPG Signing

- [ ] GPG key pair generated: `gpg --full-generate-key`.
- [ ] Key ID identified: `gpg --list-keys`.
- [ ] Private key exported: `gpg --armor --export-secret-keys KEY_ID`.
- [ ] Public key exported: `gpg --armor --export KEY_ID`.
- [ ] Public key published to a keyserver (optional but recommended).

### Sonatype Central Portal

- [ ] Account created at https://central.sonatype.com.
- [ ] Namespace `io.github.arthurhoch` verified (via DNS TXT record or GitHub repository verification).
- [ ] User token generated (Settings > User Token).
- [ ] Token username and password saved securely.

### GitHub Secrets

- [ ] `MAVEN_CENTRAL_USERNAME` — Sonatype Central Portal token username.
- [ ] `MAVEN_CENTRAL_PASSWORD` — Sonatype Central Portal token password.
- [ ] `GPG_PRIVATE_KEY` — exported private key (ASCII-armored).
- [ ] `GPG_PASSPHRASE` — GPG key passphrase.
- [ ] All secrets set in GitHub repository: Settings > Secrets and variables > Actions.

### Release Workflow

- [ ] Release workflow file exists: `.github/workflows/release-maven-central.yml`.
- [ ] Release workflow triggers on `v*` tags.
- [ ] Release workflow runs `mvn -B deploy` with GPG signing and Sonatype Central Portal credentials.
- [ ] Release workflow tested on a `-SNAPSHOT` or pre-release version (optional but recommended).

### POM Metadata

- [ ] `<groupId>` is `io.github.arthurhoch`.
- [ ] `<artifactId>` is `kiss-json`.
- [ ] `<version>` is a release version (no `-SNAPSHOT` suffix).
- [ ] `<name>`, `<description>`, `<url>` are set.
- [ ] `<licenses>` section is present and specifies Apache License 2.0.
- [ ] `<developers>` section is present.
- [ ] `<scm>` section is present with correct repository URL.
- [ ] Sonatype Central Publisher Portal plugin is configured.
- [ ] GPG signing plugin is configured.
- [ ] Javadoc and source JAR plugins are configured.

### Tag and Push

- [ ] Version in `pom.xml` updated (remove `-SNAPSHOT`).
- [ ] `CHANGELOG.md` updated: `[Unreleased]` renamed to version with date.
- [ ] Changes committed: `git commit -m "Release v0.1.0"`.
- [ ] Tag created: `git tag v0.1.0`.
- [ ] Tag pushed: `git push origin v0.1.0`.
- [ ] Release workflow triggered and passes on GitHub Actions.

### Verification

- [ ] Artifact appears on Maven Central: https://central.sonatype.com/artifact/io.github.arthurhoch/kiss-json.
- [ ] POM file is present and correct.
- [ ] Javadoc JAR is present.
- [ ] Sources JAR is present.
- [ ] GPG signature is present.
- [ ] Artifact is downloadable and usable in a test project.

### Post-Release

- [ ] Version in `pom.xml` bumped to next `-SNAPSHOT` (e.g., `0.1.1-SNAPSHOT`).
- [ ] `CHANGELOG.md` has a new `[Unreleased]` section at the top.
- [ ] Changes committed and pushed: `git commit -m "Bump to 0.1.1-SNAPSHOT" && git push origin main`.

---

## Quick Reference

| When | Checklist |
|---|---|
| Before starting a phase | Before Implementation |
| Before tagging a release | Before Release |
| Before first Maven Central publish | Before Release + Before Maven Central Publication |
| Before subsequent publishes | Before Release + verify secrets are still valid |
