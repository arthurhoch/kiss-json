# 12 — Release and Maven Central Architecture

> **Status:** Current release architecture for `0.1.0-SNAPSHOT`.

## Overview

This document describes how KissJson is versioned, released, and published to Maven Central. The release process is automated via GitHub Actions, triggered by git tags, and uses the Sonatype Central Publisher Portal.

## Versioning

### Scheme

KissJson uses **Semantic Versioning (SemVer)**: `MAJOR.MINOR.PATCH`

- **MAJOR** — Breaking public API changes
- **MINOR** — New features, backward-compatible
- **PATCH** — Bug fixes, backward-compatible

### Development Versions

During development, the version in `pom.xml` uses the `-SNAPSHOT` suffix:

```xml
<version>0.1.0-SNAPSHOT</version>
```

Snapshot versions are **not published** to Maven Central. They exist only in the local build and are used during development.

### Release Versions

Release versions have **no suffix**:

```xml
<version>0.1.0</version>
```

### Version Bump Flow

```
0.1.0-SNAPSHOT  (development)
    │
    ├── Tag: v0.1.0  →  Release workflow  →  Publish 0.1.0
    │
    └── Bump to 0.1.1-SNAPSHOT  (next development cycle)
```

1. Develop at `X.Y.Z-SNAPSHOT`.
2. When ready to release, update `pom.xml` to `X.Y.Z` (remove `-SNAPSHOT`).
3. Commit and tag: `git tag vX.Y.Z`.
4. Push tag → triggers release workflow.
5. After release, bump `pom.xml` to next `X.Y.Z-SNAPSHOT`.

## Maven Central Publishing

### Portal

KissJson uses the **Sonatype Central Publisher Portal** (the modern replacement for OSSRH):

- URL: https://central.sonatype.com/
- Namespace: `io.github.arthurhoch`
- Group ID: `io.github.arthurhoch`
- Artifact ID: `kiss-json`

### Maven Plugin

```xml
<plugin>
    <groupId>org.sonatype.central</groupId>
    <artifactId>central-publishing-maven-plugin</artifactId>
    <version>0.7.0</version>
    <extensions>true</extensions>
    <configuration>
        <publishingServerId>central</publishingServerId>
        <autoPublish>true</autoPublish>
    </configuration>
</plugin>
```

- `autoPublish=true` — Automatically publishes after validation (no manual step in the portal).
- `publishingServerId=central` — References the server credentials in `settings.xml`.

### GPG Signing

All published artifacts must be signed with GPG. The release profile activates GPG signing:

```xml
<profile>
    <id>release</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.sonatype.central</groupId>
                <artifactId>central-publishing-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>publish-to-central</id>
                        <phase>deploy</phase>
                        <goals>
                            <goal>publish</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

### Normal Build Does Not Require Secrets

The normal build (`mvn -B verify`) must **not** require any secrets:
- GPG signing is only activated in the `release` profile.
- Maven Central deployment is only activated in the `release` profile.
- Developers can build, test, and verify without GPG keys or Maven Central credentials.

```bash
# Normal build — no secrets needed
mvn -B verify

# Release deploy — requires secrets (CI only)
mvn -B deploy -Prelease
```

### Published Artifacts

For each release, the following artifacts are published:

| Artifact | Description |
|---|---|
| `kiss-json-X.Y.Z.jar` | Compiled library |
| `kiss-json-X.Y.Z-sources.jar` | Source code (for IDE navigation) |
| `kiss-json-X.Y.Z-javadoc.jar` | Javadoc (for IDE hover docs) |
| `.asc` files | GPG signatures for each artifact |
| `maven-metadata.xml` | Metadata |
| `pom.xml` | Project metadata |

### POM Requirements

The `pom.xml` must include:

- **`<name>`** — `KissJson`
- **`<description>`** — Short description
- **`<url>`** — GitHub repository URL
- **`<licenses>`** — License information
- **`<developers>`** — Developer information
- **`<scm>`** — Source control management (GitHub URL)
- **No production dependencies** — JUnit and benchmark libraries must remain test/profile scoped

## Release Workflow

### Trigger

The release workflow is triggered by pushing a tag matching `v*`:

```yaml
on:
  push:
    tags:
      - 'v*'
```

### Workflow File

`.github/workflows/release-maven-central.yml`

### Required Secrets

The following secrets must be configured in the GitHub repository settings (Settings → Secrets and variables → Actions):

| Secret | Description |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal password / token |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key |
| `GPG_PASSPHRASE` | Passphrase for the GPG key |

### Workflow Steps

```
1. Checkout code
2. Set up JDK 17
3. Import GPG key
4. Configure Maven settings.xml (server credentials)
5. Build and verify: mvn -B verify
6. Deploy to Maven Central: mvn -B deploy -Prelease
7. Create GitHub Release (optional)
```

### Security Considerations

- **Never log secrets.** The workflow must not echo credentials.
- **Use `secrets.*` references.** Never hardcode values.
- **GPG key is imported per-run.** The keyring is ephemeral.
- **Branch protection.** Tags should only be pushed by authorized users.
- **Verify before publishing.** The `verify` phase runs all tests before `deploy`.

## Pre-Release Checklist

Before creating a release tag:

- [ ] All public methods have Javadoc
- [ ] All public methods have tests
- [ ] `mvn -B verify` passes
- [ ] `CHANGELOG.md` is updated
- [ ] Documentation is current
- [ ] No production dependencies added
- [ ] No internal classes exposed as public API
- [ ] Version in `pom.xml` matches the tag (no `-SNAPSHOT`)
- [ ] Implementation and tests are complete for the release scope

## Do Not Publish Until Ready

**Do not publish to Maven Central until implementation and tests are complete.** A premature publish creates a permanent artifact that cannot be deleted from Maven Central. It is better to delay than to publish broken or incomplete code.

Specifically:
- Do not publish an artifact with unimplemented public methods.
- Do not publish an artifact with failing tests.
- Do not publish an artifact with incorrect documentation.
- Do not publish an artifact that depends on non-standard JDK APIs.

## Post-Release Steps

After a successful release:

1. **Bump version** in `pom.xml` to the next `-SNAPSHOT` version.
2. **Commit and push** the version bump.
3. **Update CHANGELOG.md** — Move released items from `Unreleased` to the version heading.
4. **Update docs** if any release-specific documentation is needed.

### Example Post-Release Commit

```
Bump version to 0.1.1-SNAPSHOT
```

## Rollback

If a released artifact has a critical bug:
1. Fix the bug on `main`.
2. Bump the PATCH version.
3. Release the fix as a new version.
4. **Do not delete or overwrite** the published artifact.
5. Document the issue and fix in `CHANGELOG.md`.

Maven Central does not support deleting published artifacts. Corrections are always new versions.
