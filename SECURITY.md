# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| 0.1.x   | Supported |

KissJson `0.1.0` is published to Maven Central. Security support applies to released `0.1.x` artifacts and the active development line.

## Responsible Disclosure

If you discover a security vulnerability in KissJson:

1. Do not publicly disclose the vulnerability before a fix is available.
2. Open a private security advisory on GitHub, or contact the maintainer directly.
3. Provide a clear description of the vulnerability, affected versions, and steps to reproduce.
4. Allow reasonable time for a response and fix before any public disclosure.

Use GitHub Security Advisories for sensitive reports when possible. Do not open a public issue for an undisclosed vulnerability.

## Zero Production Dependency Policy

KissJson has zero production dependencies. The default build uses JUnit 5 exclusively for tests. Benchmark, security, and quality tooling are isolated in Maven profiles and do not ship with the library.

This means:

- The attack surface from transitive dependencies is zero.
- No third-party library vulnerabilities can affect production users.
- The only code running in production is KissJson code and the Java standard library.

This policy must be maintained for all releases. Adding any production dependency requires explicit approval and a security review.

## Security Model

KissJson avoids common deserialization risks by design:

- no dynamic class loading from JSON data;
- no polymorphic type metadata;
- no `$id` / `$ref` graph reconstruction;
- no JavaScript engines;
- no network access;
- no framework integration;
- bounded parser and writer recursion through max-depth checks;
- internal implementation classes are not public API.

## Security Scanning

The normal build remains fast and secret-free:

```bash
mvn -B verify
```

Security and quality checks are isolated:

```bash
mvn -Psecurity verify
mvn -Pquality verify
```

GitHub Actions run CodeQL and Semgrep in separate workflows. Dependabot tracks Maven and GitHub Actions dependencies weekly. OWASP Dependency-Check runs only through the `security` Maven profile. Snyk is optional/manual unless maintainers configure it later.

See `docs/SECURITY_SCANNING.md` for details.

## No Secrets

- Do not commit secrets, API keys, passwords, or credentials to this repository.
- Maven Central publication secrets are stored in GitHub repository secrets only.
- GPG private keys are stored in GitHub repository secrets only.
- The normal build (`mvn -B verify`) must never require secrets.
- Security scanning workflows must not use Maven Central or GPG publishing secrets.

## Maven Central Artifacts

Published artifacts are signed with GPG and hosted on Maven Central via the Sonatype Central Publisher Portal.

Users should verify artifact signatures when security is critical.

## Reporting

Report security issues via GitHub Security Advisories or by contacting the maintainer.
