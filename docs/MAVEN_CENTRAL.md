# Maven Central Publication Guide

> **Release hardening guide.** Do not publish until implementation, tests, docs, and release validation are complete.

This guide describes how to publish KissJson artifacts to Maven Central via the Sonatype Central Publisher Portal.

---

## Overview

KissJson uses the **Sonatype Central Publisher Portal** (https://central.sonatype.com) for Maven Central publication. The process is:

1. Build and sign the artifacts locally (or in CI).
2. Publish to the Central Publisher Portal.
3. The portal validates and publishes to Maven Central automatically.

---

## Prerequisites

### 1. Sonatype Central Portal Account

- Go to https://central.sonatype.com and create an account.
- You can sign in with GitHub, Google, or email.

### 2. Namespace Verification

The namespace `io.github.arthurhoch` must be verified before you can publish.

**Option A: GitHub Repository Verification (recommended)**
1. In the Central Portal, add namespace `io.github.arthurhoch`.
2. The portal will ask you to create a public GitHub repository with a specific name (e.g., `ossrh-verification-io-github-arthurhoch`).
3. Create the repository on GitHub.
4. The portal verifies the repository and activates the namespace.

**Option B: DNS TXT Record**
1. Add a DNS TXT record for `arthurhoch.github.io` with the verification code provided by Sonatype.
2. The portal verifies the DNS record and activates the namespace.

### 3. GPG Key Pair

All artifacts published to Maven Central must be signed with a GPG key.

```bash
# Generate a new GPG key pair
gpg --full-generate-key
# Choose: RSA and RSA, 4096 bits, no expiration, your name and email.

# List your keys to find the KEY_ID
gpg --list-keys
# Output includes: pub   rsa4096/KEY_ID  ...

# Export the private key (ASCII-armored) — store this in GitHub Secrets
gpg --armor --export-secret-keys KEY_ID

# Export the public key (ASCII-armored) — may be needed for verification
gpg --armor --export KEY_ID

# (Optional) Publish public key to a keyserver
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
gpg --keyserver keys.openpgp.org --send-keys KEY_ID
```

### 4. Sonatype Central Portal User Token

1. Log in to https://central.sonatype.com.
2. Go to **Settings > User Token**.
3. Click **Generate**.
4. Save the **username** and **password** (this is the token, not your account password).
5. Store these in GitHub Secrets (see below).

---

## GitHub Secrets Configuration

Set the following secrets in your GitHub repository: **Settings > Secrets and variables > Actions**.

| Secret Name | Value | How to Obtain |
|---|---|---|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal token username | Settings > User Token > Generate |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal token password | Settings > User Token > Generate |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key | `gpg --armor --export-secret-keys KEY_ID` |
| `GPG_PASSPHRASE` | GPG key passphrase | The passphrase you set when generating the key |

**Important:**
- Never commit secrets to the repository.
- Use GitHub Secrets (not variables) — secrets are encrypted and not visible in logs.
- Rotate tokens periodically via the Sonatype Central Portal.

---

## Release Process

### Step 1: Update Version

Remove the `-SNAPSHOT` suffix from the version in `pom.xml`:

```xml
<!-- Before -->
<version>X.Y.Z-SNAPSHOT</version>

<!-- After -->
<version>X.Y.Z</version>
```

### Step 2: Update CHANGELOG.md

Rename the `[Unreleased]` section to the version with the date:

```markdown
## [0.1.0] - 2026-05-05

### Added
- Initial release of KissJson.
- ...
```

### Step 3: Commit

```bash
git add pom.xml CHANGELOG.md
git commit -m "Release v0.1.0"
```

### Step 4: Tag

```bash
git tag v0.1.0
```

### Step 5: Push

```bash
git push origin main --tags
# Or push just the tag:
git push origin v0.1.0
```

### Step 6: Monitor Release Workflow

The GitHub Actions release workflow (`.github/workflows/release-maven-central.yml`) is triggered by the `v*` tag. It will:

1. Check out the code at the tagged commit.
2. Set up JDK 17.
3. Import the GPG private key from secrets.
4. Build, test, and verify: `mvn -B verify`.
5. Deploy to the Central Publisher Portal: `mvn -B deploy -Prelease`.
6. Sign all artifacts with GPG.

Monitor the workflow at: **Actions tab > Release workflow**.

If the workflow fails, check:
- Are all secrets set correctly?
- Has the Sonatype token expired?
- Is the GPG passphrase correct?
- Is the namespace `io.github.arthurhoch` verified?

---

## Verification

After the release workflow succeeds:

1. Go to https://central.sonatype.com/artifact/io.github.arthurhoch/kiss-json.
2. Verify the artifact is present with the correct version.
3. Verify the following files are present:
   - `kiss-json-0.1.0.jar` — compiled artifact.
   - `kiss-json-0.1.0-sources.jar` — source code.
   - `kiss-json-0.1.0-javadoc.jar` — Javadoc.
   - `kiss-json-0.1.0.pom` — POM file.
   - `.asc` files — GPG signatures for each artifact.
4. Test in a new project:

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-json</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
import io.github.arthurhoch.kissjson.Json;

Json jsonEngine = Json.create();
String json = jsonEngine.stringify(myObject);
MyClass obj = jsonEngine.parse(json, MyClass.class);
```

---

## Post-Release

After successful publication:

1. Bump the version in `pom.xml` to the next `-SNAPSHOT`:

```xml
<version>0.1.1-SNAPSHOT</version>
```

2. Add a new `[Unreleased]` section at the top of `CHANGELOG.md`:

```markdown
## [Unreleased]

### Added
### Changed
### Fixed
```

3. Commit and push:

```bash
git add pom.xml CHANGELOG.md
git commit -m "Bump to 0.1.1-SNAPSHOT"
git push origin main
```

---

## Troubleshooting

### GPG Passphrase Issues

**Symptom:** Build fails with `gpg: signing failed: No passphrase given` or `gpg: signing failed: Bad passphrase`.

**Fix:**
- Verify `GPG_PASSPHRASE` secret matches the passphrase used when generating the key.
- Ensure the private key was exported with the correct key ID.
- Try re-exporting: `gpg --armor --export-secret-keys KEY_ID`.

### Namespace Not Verified

**Symptom:** Deploy fails with `Namespace io.github.arthurhoch is not verified` or 403 error.

**Fix:**
- Go to https://central.sonatype.com and check the namespace status.
- Complete the verification process (GitHub repo or DNS TXT record).
- Wait for verification to complete (usually instant for GitHub, up to hours for DNS).

### Token Expired

**Symptom:** Deploy fails with 401 Unauthorized.

**Fix:**
- Go to https://central.sonatype.com > Settings > User Token.
- Generate a new token.
- Update `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` in GitHub Secrets.

### Artifact Not Appearing on Maven Central

**Symptom:** Workflow succeeds but artifact is not visible on Maven Central.

**Fix:**
- Check the Central Portal publishing status at https://central.sonatype.com/publishing.
- The portal may take a few minutes to process and publish.
- Ensure the publishing mode is set to "Automatic" (not "Manual" which requires explicit release).

### Normal Build Requires Secrets

**Symptom:** `mvn -B verify` fails because it requires GPG or Sonatype credentials.

**Fix:**
- GPG signing and Sonatype deployment should only be active in the release profile or release workflow.
- The normal build (`mvn -B verify`) must work without any secrets.
- Check that GPG signing is configured in a Maven profile that is only activated during release (e.g., via `-P release` or in the CI workflow).

---

## Security Notes

- **Never commit secrets.** All credentials go in GitHub Secrets, not in code.
- **Rotate tokens.** Periodically regenerate your Sonatype user token.
- **Protect the GPG private key.** It is stored in GitHub Secrets and should not be shared.
- **Normal build is secret-free.** `mvn -B verify` must not require any secrets. This is enforced by the CI workflow.
- **See `SECURITY.md` for the full security policy.**

---

## Important: Do Not Publish Early

> Do not publish to Maven Central until implementation and tests are complete. A premature publish creates a permanent artifact that cannot be deleted. The first release should be a fully functional v1 (or a well-documented alpha/beta if intentional).
