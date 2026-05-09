# Versioned AI Skill Release Policy

This repository publishes versioned Markdown skill files under <code>docs/skills/</code> so AI agents can use KissJson accurately from other projects.

## Required For Every Release Tag

Before creating a tag such as <code>v0.2.0</code>:

1. Create or refresh <code>docs/skills/v0.2.0.md</code> for the exact release version.
2. Keep every older skill file, including <code>docs/skills/v0.1.0.md</code>. Do not rewrite release history unless correcting a serious documentation defect.
3. Include the Maven coordinate <code>io.github.arthurhoch:kiss-json:&lt;release&gt;</code>.
4. Include usage rules, behavioral contracts, examples, and the complete public API/member index.
5. Regenerate the API index from compiled classes, for example:

~~~bash
mvn -B -DskipTests compile
find target/classes -name '*.class' | sort
# Then run javap -classpath target/classes -public for the public classes.
~~~

6. Update <code>docs/skills/index.md</code> with both links:
   - View: <code>v0.2.0.html</code>
   - Download: <code>https://raw.githubusercontent.com/arthurhoch/kiss-json/main/docs/skills/v0.2.0.md</code>
7. Ensure the GitHub Pages home page links to <code>skills/index.html</code>.
8. If this repository has <code>.github/ALL_MARKDOWN.md</code>, add the new skill file there.
9. Run release documentation checks:

~~~bash
mvn -B clean verify
mvn -B javadoc:javadoc
~~~

## API Accuracy Rules

- Do not invent methods or examples that rely on non-existent overloads.
- Prefer the consumer-facing API. Mark <code>internal</code> packages as implementation detail even if their classes are technically public.
- If a public API changed since the previous release, document the change in the skill file and in release notes.
- If a public API was removed or renamed, confirm that the change follows the project's compatibility and deprecation policy.

## Download Link Template

~~~text
https://raw.githubusercontent.com/arthurhoch/kiss-json/main/docs/skills/vX.Y.Z.md
~~~
