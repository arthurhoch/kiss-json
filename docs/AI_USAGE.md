# AI Usage Guide

This page gives AI coding agents and maintainers a compact user-facing guide for using KissJson correctly in consumer projects. It does not replace `AGENTS.md`, `CAVEMAN.md`, `docs/PRODUCT_SPEC.md`, or `.github/architecture/index.md` for changes to this repository.

## When To Use KissJson

Use KissJson when a Java 17+ project needs a tiny zero-dependency JSON library for field-based serialization and deserialization of normal Java object graphs.

```java
Json json = Json.create();
String body = json.stringify(request);
Response response = json.parse(text, Response.class);
```

## Rules For Generated Code

- Import public API from `io.github.arthurhoch.kissjson`.
- Use fields directly in examples; do not describe getters or setters as the mapping contract.
- Keep JSON serialization separate from HTTP, persistence, routing, validation, logging, and dependency injection.
- Do not add Jackson, Gson, JSON-B, or JSON-P merely to use KissJson.
- Do not use internal classes from `io.github.arthurhoch.kissjson.internal`.
- Catch `JsonException`, `JsonParseException`, or `JsonMappingException` when useful and preserve the original message/cause.

## Configuration Pattern

Create one configured `Json` instance where the application boundary needs custom behavior:

```java
Json json = Json.builder()
        .fieldNaming(FieldNaming.SNAKE_CASE)
        .includeNulls(false)
        .failOnUnknownProperties(false)
        .build();
```

Do not add wrapper frameworks around KissJson unless the consuming application has a clear boundary reason.

## Related Projects

These libraries are independent, zero-dependency Java 17+ projects. Use only the modules you need.

| Project | Purpose |
|---|---|
| [kiss-json](https://github.com/arthurhoch/kiss-json) | Field-based JSON serialization and deserialization. |
| [kiss-requests](https://github.com/arthurhoch/kiss-requests) | Simple HTTP client built on Java HttpClient. |
| [kiss-server](https://github.com/arthurhoch/kiss-server) | Small HTTP/1.1 server for simple REST-style applications. |
| [kiss-config](https://github.com/arthurhoch/kiss-config) | Configuration loading from properties, .env files, system properties, and environment variables. |
| [kiss-binary](https://github.com/arthurhoch/kiss-binary) | Explicit binary IO for primitive binary formats. |
