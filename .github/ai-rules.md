# AI Behavior Rules

Strict rules for AI coding agents working on KissJson.

## Mandatory

- Read required docs before making changes. Reading order:
  1. `CAVEMAN.md`
  2. `AGENTS.md`
  3. `docs/PRODUCT_SPEC.md`
  4. `.github/architecture/index.md`
  5. `docs/IMPLEMENTATION_PLAN.md`
- Every change must follow the change protocol in `AGENTS.md`.

## Do Not

- Do not invent features outside v1 scope.
- Do not add dependencies unless explicitly approved.
- Do not create a framework.
- Do not expose internal classes as public API.
- Do not over-engineer. Prefer simple, readable code.
- Do not silently change public API behavior.
- Do not skip tests.
- Do not skip documentation updates.
- Do not skip changelog updates.
- Do not use getters or setters for field mapping.
- Do not use regex or `String.split` in the parser.
- Do not add Java versions above 17.
- Do not use preview features.
- Do not commit secrets.
- Do not claim unimplemented features as working.

## Always

- Always choose the simpler solution when in doubt.
- Always preserve Java 17 compatibility.
- Always keep public API small.
- Always update docs, tests, and changelog when changing public API.
- Always use field-based mapping.
- Always include context in error messages.
- Always run `mvn -B verify` before considering work complete.
