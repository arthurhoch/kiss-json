---
layout: default
title: KissJson
---

<section class="hero">
  <div>
    <p class="eyebrow">KISS Java Libraries</p>
    <h1>KissJson</h1>
    <p class="lead">Tiny zero-dependency Java 17+ JSON serialization and deserialization with direct field mapping, explicit configuration, and useful parse errors.</p>
    <div class="meta-row">
      <span class="tag">Latest stable: 0.1.0</span>
      <span class="tag">Java 17+</span>
      <span class="tag">Apache-2.0</span>
    </div>
    <div class="actions">
      <a class="button" href="GETTING_STARTED.html">Getting Started</a>
      <a class="button secondary" href="API.html">API Reference</a>
      <a class="button secondary" href="https://github.com/arthurhoch/kiss-json">GitHub</a>
    </div>
  </div>
  <div class="panel">
    <p class="panel-title">Maven</p>
<pre><code>&lt;dependency&gt;
  &lt;groupId&gt;io.github.arthurhoch&lt;/groupId&gt;
  &lt;artifactId&gt;kiss-json&lt;/artifactId&gt;
  &lt;version&gt;0.1.0&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
  </div>
</section>

<section class="section two-column">
  <div>
    <h2>Small Surface</h2>
    <p>KissJson maps fields directly without getters, setters, runtime plugins, or transitive dependencies. The API is intended to be easy to remember and easy to inspect.</p>
  </div>
  <div class="panel">
    <p class="panel-title">Quick Example</p>
<pre><code>Json json = Json.create();
String text = json.stringify(user);
User parsed = json.parse(text, User.class);</code></pre>
  </div>
</section>

<section class="section">
  <h2>KISS Principles</h2>
  <div class="feature-grid">
    <article class="feature">
      <h3>Zero Dependencies</h3>
      <p>The production artifact stays independent of external JSON frameworks.</p>
    </article>
    <article class="feature">
      <h3>Explicit Mapping</h3>
      <p>Field names, null handling, dates, enums, aliases, and required fields are configured directly.</p>
    </article>
    <article class="feature">
      <h3>Readable Failures</h3>
      <p>Parse and mapping errors include path, line, column, and cause context.</p>
    </article>
  </div>
</section>

<section class="section">
  <h2>Documentation</h2>
  <div class="doc-grid">
    <a href="GETTING_STARTED.html">Getting Started<span>Install and first JSON round trip.</span></a>
    <a href="API.html">API Reference<span>Public API and annotations.</span></a>
    <a href="EXAMPLES.html">Examples<span>Copyable examples for common cases.</span></a>
    <a href="CONFIGURATION.html">Configuration<span>Builder options and behavior switches.</span></a>
    <a href="ERROR_HANDLING.html">Error Handling<span>Exception hierarchy and debug context.</span></a>
    <a href="PERFORMANCE.html">Performance<span>Performance strategy and expectations.</span></a>
    <a href="BENCHMARKS.html">Benchmarks<span>JMH benchmark methodology and results.</span></a>
    <a href="SECURITY_SCANNING.html">Security Scanning<span>CodeQL, Semgrep, Dependabot, OWASP, and SpotBugs.</span></a>
    <a href="security-hardening.html">Security Hardening<span>Repository hardening and local quality commands.</span></a>
    <a href="code-cleanup.html">Safe Code Cleanup<span>Deletion policy and quality gates.</span></a>
    <a href="TESTING_REPORT.html">Testing Report<span>Current verification state.</span></a>
    <a href="RELEASE.html">Release<span>Release process and Maven Central flow.</span></a>
  </div>
</section>

<section class="section">
  <h2>Related Projects</h2>
  <div class="related-grid">
    <a href="https://github.com/arthurhoch/kiss-json">kiss-json<span>Field-based JSON serialization and deserialization.</span></a>
    <a href="https://github.com/arthurhoch/kiss-requests">kiss-requests<span>Simple HTTP client built on Java HttpClient.</span></a>
    <a href="https://github.com/arthurhoch/kiss-server">kiss-server<span>Small HTTP/1.1 server for simple REST-style applications.</span></a>
    <a href="https://github.com/arthurhoch/kiss-config">kiss-config<span>Configuration from properties, .env, system properties, and environment variables.</span></a>
    <a href="https://github.com/arthurhoch/kiss-binary">kiss-binary<span>Explicit binary IO for primitive binary formats.</span></a>
  </div>
</section>
