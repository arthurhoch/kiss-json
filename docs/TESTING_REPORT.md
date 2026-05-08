# Testing Report

Date: 2026-05-08

## What Was Tested

- Public `Json` entry points for serialization, deserialization, parsing, and builder usage.
- Field-based object mapping with private fields, superclass fields, nested objects, arrays, collections, maps, primitives, wrappers, enums, dates, and nulls.
- Annotation behavior for custom names, aliases, ignored fields, required fields, date formats, and null inclusion/exclusion.
- Parser and writer edge cases, including string escaping, duplicate keys, unknown properties, max depth, cycles, primitive nulls, pretty printing, and numeric values.
- Error handling with path, line, column, and cause context.
- Project sanity checks for Java 17 compilation, jar/source/javadoc artifacts, Javadocs, security profile wiring, and benchmark profile wiring.

## Commands Run

```bash
mvn -B clean verify
mvn -B test jacoco:report
mvn -B javadoc:javadoc
mvn -B -Pquality -DskipTests verify
mvn -B -Psecurity -Ddependency-check.skip=true -DskipTests verify
mvn -B -Pbenchmark test-compile
```

Results:

- `mvn -B clean verify`: passing, 152 tests, 0 failures, 0 errors.
- `mvn -B test jacoco:report`: passing; reports generated at `target/site/jacoco/jacoco.xml` and `target/site/jacoco/index.html`.
- `mvn -B javadoc:javadoc`: passing.
- `mvn -B -Pquality -DskipTests verify`: passing, SpotBugs completed with 0 findings.
- `mvn -B -Psecurity -Ddependency-check.skip=true -DskipTests verify`: passing profile validation with Dependency-Check database scanning intentionally skipped.
- `mvn -B -Pbenchmark test-compile`: passing. The benchmark profile compiles with benchmark-only Jackson dependencies; full JMH benchmarks were not run.

## Known Limits

- The report records automated verification only; it does not claim fresh benchmark numbers.
- Optional Snyk scanning is not part of the default local verification.
- The OWASP Dependency-Check profile may require network access and a populated vulnerability database, so it was not run as part of this short verification pass.

## Future Tests Recommended

- Add malformed JSON corpus and fuzz-style parser cases.
- Record fresh JMH results with hardware, JDK, warmup, and payload details.
- Add more large-payload and deeply nested object mapping scenarios.
