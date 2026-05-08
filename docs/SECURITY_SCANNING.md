# Security Scanning

KissJson keeps security tooling useful but isolated. The default developer build stays fast and does not download vulnerability databases:

```bash
mvn -B clean verify
```

Security-heavy checks run separately:

```bash
mvn -Psecurity verify
```

Optional static quality checks run separately:

```bash
mvn -Pquality verify
```

None of these commands require Maven Central publishing credentials or GPG secrets.

## GitHub Actions

### CI

The normal CI workflow runs only:

```bash
mvn -B clean verify
```

It generates Surefire and JaCoCo reports, then uploads them as workflow artifacts. It does not run OWASP Dependency-Check, SpotBugs, release publishing, or any secret-backed tooling.

### Coverage

JaCoCo runs during Maven `verify`. Local coverage reports are generated at:

```text
target/site/jacoco/jacoco.xml
target/site/jacoco/index.html
```

Use the HTML report for review and the XML report for Codecov or Sonar if those services are configured later. No Codecov or Sonar token is required for the current repository setup.

### CodeQL

CodeQL runs in `.github/workflows/codeql.yml` on:

- pushes to `main`;
- pull requests targeting `main`;
- a weekly schedule;
- manual dispatch.

The workflow uses Java 17, Maven cache, `security-and-quality` queries, and this analysis build:

```bash
mvn -B -DskipTests -Djacoco.skip=true package
```

It does not publish artifacts and does not require repository secrets.

### Semgrep

Semgrep runs in `.github/workflows/semgrep.yml` on:

- pushes to `main`;
- pull requests targeting `main`;
- a weekly schedule.

It uses Semgrep Community Edition rules for Java and security-audit checks. It is separate from the release workflow and does not require a Semgrep token.

Dependabot-authored runs are skipped to avoid GitHub Actions permission issues on automated dependency PRs.

### Dependabot

Dependabot is configured in `.github/dependabot.yml` for:

- Maven dependencies in `/`;
- GitHub Actions dependencies in `/`.

Updates are scheduled weekly and labeled `dependencies` and `security`.

### Dependency Review

Dependency Review runs on pull requests and checks dependency diffs for known vulnerabilities. It fails on moderate or higher severity findings and does not require repository secrets.

### OpenSSF Scorecard

OpenSSF Scorecard runs on schedule and manual dispatch. It uploads SARIF to code scanning and uses only GitHub-provided permissions.

See [Security Hardening](security-hardening.md) for the repository settings that must be enabled in GitHub.

## OWASP Dependency-Check

The Maven `security` profile runs OWASP Dependency-Check:

```bash
mvn -Psecurity verify
```

Reports are written under:

```text
target/dependency-check-report/
```

The profile produces HTML and JSON reports and fails the build on CVSS 7.0 or higher findings.

The first run can be slow because Dependency-Check downloads vulnerability metadata. This is why the profile is not part of normal CI.

KissJson has zero production dependencies, so dependency findings should normally be limited to test, benchmark, or build-time tooling.

## SpotBugs

The Maven `quality` profile runs SpotBugs:

```bash
mvn -Pquality verify
```

The profile is optional and isolated from the normal build. It scans production classes with a high threshold to keep false-positive noise low.

Reports are written to `target/spotbugsXml.xml` and `target/site/spotbugs.html`.

Do not add suppressions unless a finding is reviewed and the reason is documented.

## Snyk

Snyk is optional. It is not required for normal CI and no Snyk token is committed or expected by default.

Maintainers may connect the GitHub repository to Snyk manually or run the Snyk CLI locally if they have an account. Snyk can scan dependencies and, depending on plan, source code. Because KissJson has zero production dependencies, dependency findings should be minimal; Snyk Code or Semgrep are more useful for source-level issues.

Do not add a mandatory Snyk workflow unless repository secrets and account ownership are explicitly configured.

## Release Separation

The Maven Central release workflow is separate from security scanning workflows. It requires only Maven Central and GPG secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

Security workflows must not use those secrets.

## Maintenance Rules

- Do not add production dependencies for scanning.
- Do not make `mvn -B clean verify` download vulnerability databases.
- Do not suppress security findings without a documented reason.
- Keep CodeQL, Dependabot, Dependency Review, OpenSSF Scorecard, Semgrep, and OWASP configuration consistent with this document.
- Never commit credentials, tokens, private keys, or generated secret files.
