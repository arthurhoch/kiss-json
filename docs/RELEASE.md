# Release Process

> **Release guide.** Version `0.1.0` is published on Maven Central. Future releases must complete implementation, tests, docs, and release validation before publishing.

This document describes the versioning, changelog, and release process for KissJson.

---

## Versioning

KissJson follows **Semantic Versioning** (semver): `MAJOR.MINOR.PATCH`.

| Component | Meaning | Example |
|---|---|---|
| `MAJOR` | Breaking changes to the public API. | `1.0.0` → `2.0.0` |
| `MINOR` | New features, backward-compatible. | `0.1.0` → `0.2.0` |
| `PATCH` | Bug fixes, backward-compatible. | `0.1.0` → `0.1.1` |

### SNAPSHOT Versions

During development, the version in `pom.xml` carries a `-SNAPSHOT` suffix:

```xml
<version>0.1.1-SNAPSHOT</version>
```

- `-SNAPSHOT` indicates unreleased, in-development code.
- `-SNAPSHOT` artifacts are not published to Maven Central.
- Remove `-SNAPSHOT` only when preparing a release.

### Initial Release

The first release was `0.1.0`. The `0.x.x` range indicates that the API is not yet stable and may change between minor versions. Once the API is considered stable, release `1.0.0`.

---

## Changelog

KissJson uses the **[Keep a Changelog](https://keepachangelog.com/)** format.

### Format

```markdown
# Changelog

## [Unreleased]

### Added
- New feature description.

### Changed
- Change description.

### Fixed
- Fix description.

## [0.1.0] - 2026-05-05

### Added
- Initial release.
```

### Rules

- The `[Unreleased]` section is always at the top.
- New changes go under `[Unreleased]` as they are implemented.
- When releasing, rename `[Unreleased]` to `[VERSION] - YYYY-MM-DD`.
- Add a fresh `[Unreleased]` section after releasing.
- Use `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security` categories as applicable.
- Every change to the public API must be documented in the changelog.

---

## Pre-Release Checklist

Before starting the release process, verify:

- [ ] **All tests pass:** `mvn -B verify` succeeds with no failures.
- [ ] **Docs updated:** Documentation matches the current public API.
- [ ] **CHANGELOG updated:** All changes since the last release are documented under `[Unreleased]`.
- [ ] **No new production dependencies:** Verify `pom.xml` has zero production `<dependencies>`.
- [ ] **No internal classes exposed:** Verify only approved classes are `public`.
- [ ] **Java 17 only:** Code compiles and runs on Java 17. No preview features.
- [ ] **Javadoc complete:** Every public method has Javadoc.
- [ ] **Test coverage:** Every public method has at least one test.
- [ ] **CHANGELOG review:** Read through `[Unreleased]` entries — are they accurate and complete?

See `docs/REVIEW_CHECKLIST.md` for the full checklist.

---

## Release Steps

### 1. Update Version in pom.xml

Remove the `-SNAPSHOT` suffix:

```xml
<!-- Before -->
<version>X.Y.Z-SNAPSHOT</version>

<!-- After -->
<version>X.Y.Z</version>
```

### 2. Update CHANGELOG.md

Rename `[Unreleased]` to the version with today's date:

```markdown
## [0.1.0] - 2026-05-05

### Added
- Initial release of KissJson.
- Serialize/deserialize Java objects to/from JSON.
- ... (list all changes)
```

### 3. Commit

```bash
git add pom.xml CHANGELOG.md
git commit -m "Release v0.1.0"
```

### 4. Tag

```bash
git tag v0.1.0
```

Tags use the `v` prefix: `v0.1.0`, `v0.1.1`, `v0.2.0`, `v1.0.0`, etc.

### 5. Push

```bash
git push origin main --tags
```

This pushes both the commit and the tag. The tag triggers the release workflow on GitHub Actions.

### 6. Monitor Release Workflow

1. Go to the **Actions** tab on GitHub.
2. Find the **Release** workflow run triggered by the `v0.1.0` tag.
3. Monitor the workflow:
   - **Build and test:** `mvn -B verify` passes.
   - **GPG signing:** Artifacts are signed.
   - **Deploy:** Artifacts are published to the Central Publisher Portal.
4. If the workflow fails:
   - Check the logs for the failing step.
   - Fix the issue.
   - Delete the tag: `git tag -d v0.1.0 && git push origin :refs/tags/v0.1.0`.
   - Fix the problem, commit, and re-tag.

---

## Post-Release

After the release workflow succeeds and the artifact is on Maven Central:

### 1. Bump Version to Next SNAPSHOT

```xml
<version>0.1.1-SNAPSHOT</version>
```

### 2. Add New Unreleased Section to CHANGELOG.md

```markdown
## [Unreleased]

### Added
### Changed
### Fixed
```

### 3. Commit and Push

```bash
git add pom.xml CHANGELOG.md
git commit -m "Bump to 0.1.1-SNAPSHOT"
git push origin main
```

---

## Do Not Publish Future Releases Early

> **Do not publish a functional release to Maven Central until implementation and tests are complete.**
>
> Once an artifact is published to Maven Central, it cannot be deleted or replaced (only superseded by a new version). Publishing an incomplete or broken artifact creates a permanent bad experience for users.
>
> If you need to test the release workflow, use the Central Portal's validation flow. Do not tag a release version until the library is ready.

---

## Quick Reference

| Step | Command |
|---|---|
| Update version | Edit `pom.xml`: remove `-SNAPSHOT` |
| Update changelog | Rename `[Unreleased]` to `[VERSION] - DATE` |
| Commit | `git commit -m "Release v0.1.0"` |
| Tag | `git tag v0.1.0` |
| Push | `git push origin main --tags` |
| Verify | Check GitHub Actions + Maven Central |
| Post-release | Bump version to next `-SNAPSHOT`, commit, push |
