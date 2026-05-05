# 13 — GitHub Pages Architecture

> **Status:** Current GitHub Pages architecture for `0.1.0`.

## Overview

The KissJson documentation site is served via **GitHub Pages** from the `docs/` directory on the `main` branch. It is built with **Jekyll**, uses a **minimal theme**, and is triggered on push to `main` when documentation files change.

## Design Goals

- **Keep it fast and simple.** No complex build pipelines, no custom plugins, no heavy frameworks.
- **Documentation lives in the repo.** Docs are version-controlled alongside the code.
- **Automatic deployment.** Push to `main` → site updates. No manual steps.
- **Separation of concerns.** User-facing docs in `docs/`. Architecture docs in `.github/architecture/`. Governance in `.github/`.

## Site Structure

### Served by GitHub Pages (`docs/`)

The `docs/` directory is the root of the documentation site. These files are publicly accessible at `https://arthurhoch.github.io/kiss-json/` (or the configured custom domain).

```
docs/
├── _config.yml          # Jekyll configuration
├── index.md             # Landing page
├── GETTING_STARTED.md   # Installation and first usage
├── API.md               # Public API reference
├── CONFIGURATION.md     # JsonConfig options
├── ERROR_HANDLING.md    # Error handling guide
├── EXAMPLES.md          # Common usage patterns
├── PERFORMANCE.md       # Performance strategy
├── BENCHMARKS.md        # Benchmark methodology and results
├── MAVEN_CENTRAL.md     # Maven Central publication guide
└── RELEASE.md           # Release process
```

### Not Served Directly (`.github/architecture/`)

Architecture docs are **not** served by GitHub Pages directly. They are internal design documents intended for contributors and AI agents. However, they may be **linked from** the user-facing docs for users who want to understand the design.

```
.github/architecture/
├── index.md              # Architecture index
├── 00-overview.md        # Architecture overview
├── 01-package-structure.md
├── 02-public-api.md
├── ...
└── 13-github-pages.md    # This file
```

Users can view these files on GitHub directly (raw markdown), but they are not part of the styled documentation site.

## Jekyll Configuration

### `docs/_config.yml`

```yaml
title: KissJson
description: A tiny, high-performance, zero-dependency Java 17+ JSON library
url: "https://arthurhoch.github.io"
baseurl: "/kiss-json"
theme: jekyll-theme-minimal
markdown: kramdown
kramdown:
  input: GFM
  hard_wrap: false
gems:
  - jekyll-readme-index
```

The expected Pages URL for the `kiss-json` repository is:

```text
https://arthurhoch.github.io/kiss-json/
```

### Key Decisions

| Decision | Rationale |
|---|---|
| **Theme: `jekyll-theme-minimal`** | Simple, clean, fast, and available in GitHub Pages. No custom CSS needed for v1. |
| **Markdown: `kramdown` with GFM** | GitHub-Flavored Markdown support. Standard for Jekyll on GitHub Pages. |
| **No custom plugins** | GitHub Pages has a plugin whitelist. The current workflow uses the standard `actions/jekyll-build-pages` action without repository-managed Ruby dependencies. |

## Deployment

### Workflow File

`.github/workflows/pages.yml`

### Trigger

The Pages workflow is triggered on push to `main` when files in `docs/` change:

```yaml
on:
  push:
    branches:
      - main
    paths:
      - 'docs/**'
      - '.github/workflows/pages.yml'
```

This avoids unnecessary builds when only code changes.

### Workflow Steps

```
1. Checkout code
2. Configure GitHub Pages
3. Build the `docs/` directory with `actions/jekyll-build-pages`
4. Upload the generated `_site` artifact
5. Deploy to GitHub Pages
```

### Deployment Method

Use the standard GitHub Pages deployment action:

```yaml
- uses: actions/checkout@v4
- uses: actions/configure-pages@v5
- uses: actions/jekyll-build-pages@v1
  with:
    source: docs
    destination: ./_site
- uses: actions/upload-pages-artifact@v3
- uses: actions/deploy-pages@v4
```

## Content Guidelines

### Landing Page (`docs/index.md`)

The landing page should include:
- Project name and tagline
- One-sentence description
- Quick start code example
- Link to full documentation
- Link to API reference

### API Reference (`docs/API.md`)

Auto-generated or manually maintained. If manually maintained, it must be kept in sync with Javadoc.

In v1, the API reference is **manually maintained** and must match the public API exactly. Future versions may automate this from Javadoc.

### Code Examples

- All code examples must use the **current public API**.
- All code examples must be **correct** — ideally verified by copying from tests.
- Include `import` statements when the class is not obvious.
- Use Java 17 syntax only.

### Linking Between Docs

- Use relative links within `docs/`: `[Configuration](CONFIGURATION.md)`
- Use absolute GitHub links to architecture docs: `[Architecture](https://github.com/arthurhoch/kiss-json/blob/main/.github/architecture/00-product-purpose.md)`
- Use Javadoc links where applicable: `{@link Json#stringify(Object)}`

## Custom Domain (Optional)

If a custom domain is desired in the future:

1. Add a `docs/CNAME` file with the domain name.
2. Configure DNS records (CNAME or A record).
3. Enable custom domain in GitHub repository settings.

This is **not planned for v1** but the architecture supports it.

## Maintenance

### Keeping Docs Current

- Docs must be updated whenever the public API changes (see `11-documentation-policy.md`).
- Stale docs are a bug. If the docs are wrong, fix them.
- Use the pre-release checklist to verify docs before publishing.

### Adding New Pages

1. Create the markdown file in `docs/`.
2. Add front matter (title, layout).
3. Add a link to the new page from `docs/index.md` or relevant pages.
4. Verify locally with GitHub Pages tooling or the Pages workflow.
5. Push to `main` → automatic deployment.

### Local Preview

To preview the documentation site locally, use a GitHub Pages compatible Jekyll setup:

```bash
cd docs
bundle exec jekyll serve
# Open http://localhost:4000/kiss-json/
```

## Performance

- **Minimal theme** — No heavy CSS or JavaScript frameworks.
- **No custom plugins** — Fast build times.
- **No build step beyond Jekyll** — No Webpack, no React, no SPA.
- **Markdown files are small** — Fast rendering.
- **No external assets** — No CDN dependencies (beyond what GitHub Pages provides).

The goal is sub-second page loads for documentation pages.
