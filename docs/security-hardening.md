# Security Hardening

KissJson uses free GitHub-native and open-source checks. Normal CI, CodeQL, Dependency Review, Dependabot, Semgrep, and OpenSSF Scorecard do not require repository secrets.

## Automated Checks

- CI runs `mvn -B clean verify`, generates Surefire and JaCoCo reports, and uploads report artifacts.
- CodeQL runs with Java 17, Maven cache, `security-and-quality` queries, and the analysis build `mvn -B -DskipTests -Djacoco.skip=true package`.
- Semgrep runs community Java and security-audit rules separately from CI.
- Dependabot monitors Maven and GitHub Actions dependencies.
- Dependency Review checks pull request dependency changes and fails on moderate or higher vulnerability findings.
- OpenSSF Scorecard runs on schedule and manual dispatch, then uploads SARIF to code scanning.

## Local Commands

```bash
mvn -B clean verify
mvn -B test jacoco:report
mvn -B javadoc:javadoc
mvn -Pquality verify
mvn -Psecurity verify
mvn -Pbenchmark test-compile
```

The coverage reports are:

```text
target/site/jacoco/jacoco.xml
target/site/jacoco/index.html
```

## GitHub Repository Settings

Enable these settings in GitHub under **Settings > Code security and analysis**:

- Dependency graph;
- Dependabot alerts;
- Dependabot security updates;
- Code scanning.

No Codecov, Sonar, Semgrep, or Snyk token is required by the current setup. Coverage badges should be added only after a real external coverage service is configured.

## Cleanup And Release Gates

Before deleting code, follow [Safe Code Cleanup](code-cleanup.md). Treat public API as compatibility-sensitive: deprecate first when removal can affect Maven Central consumers.

Before release, run the normal build, Javadocs, coverage generation, and any relevant optional profile. Release secrets must stay limited to the release workflow and must not be used by security workflows.
