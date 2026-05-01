# Getting Started

> **Note:** This document describes the current v1 API for `0.1.0-SNAPSHOT`.

---

## Installation

### Maven

```xml
<dependency>
  <groupId>io.github.arthurhoch</groupId>
  <artifactId>kiss-json</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.github.arthurhoch:kiss-json:0.1.0-SNAPSHOT")
```

### Gradle (Groovy DSL)

```groovy
implementation 'io.github.arthurhoch:kiss-json:0.1.0-SNAPSHOT'
```

KissJson has **zero dependencies** — nothing else is needed.

---

## First Serialize

Define a simple class with fields:

```java
import io.github.arthurhoch.kissjson.Json;

public class User {
    String name;
    int age;
    boolean active;
}

Json json = Json.create();

User user = new User();
user.name = "Alice";
user.age = 30;
user.active = true;

String text = json.stringify(user);
// {"name":"Alice","age":30,"active":true}
```

No getters. No setters. No annotations required. Just fields.

---

## First Deserialize

```java
String text = """
    {"name":"Alice","age":30,"active":true}
    """;

User user = json.parse(text, User.class);
// user.name == "Alice"
// user.age == 30
// user.active == true
```

---

## Nested Objects

```java
public class Address {
    String street;
    String city;
    String zipCode;
}

public class User {
    String name;
    Address address;
}

String text = """
    {
      "name": "Alice",
      "address": {
        "street": "123 Main St",
        "city": "Springfield",
        "zipCode": "62701"
      }
    }
    """;

User user = json.parse(text, User.class);
// user.address.street == "123 Main St"
// user.address.city == "Springfield"
```

---

## Parse a List

```java
String text = """
    [
      {"name":"Alice","age":30},
      {"name":"Bob","age":25}
    ]
    """;

List<User> users = json.parseList(text, User.class);
// users.get(0).name == "Alice"
// users.get(1).name == "Bob"
```

---

## Parse a Map

Parse a JSON object as a `Map<String, Object>`:

```java
String text = """
    {"name":"Alice","age":30}
    """;

Map<String, Object> map = json.parseMap(text);
// map.get("name") == "Alice"
// map.get("age") == 30 (Integer)
```

Parse a JSON object as a `Map<String, T>`:

```java
String text = """
    {
      "admin": {"name":"Alice","age":30},
      "guest": {"name":"Bob","age":25}
    }
    """;

Map<String, User> users = json.parseMap(text, User.class);
// users.get("admin").name == "Alice"
// users.get("guest").name == "Bob"
```

---

## Field Aliases

```java
import io.github.arthurhoch.kissjson.JsonAliases;

public class User {
    @JsonAliases({"userName", "user_name", "username"})
    String name;
}

String text = """
    {"userName":"Alice"}
    """;

User user = json.parse(text, User.class);
// user.name == "Alice"
```

`@JsonAliases` works during deserialization only. The primary field name is used during serialization.

---

## Naming Strategy

```java
import io.github.arthurhoch.kissjson.Json;
import io.github.arthurhoch.kissjson.FieldNaming;

public class UserProfile {
    String firstName;
    String lastName;
}

Json json = Json.builder()
    .fieldNaming(FieldNaming.SNAKE_CASE)
    .build();

UserProfile profile = new UserProfile();
profile.firstName = "Alice";
profile.lastName = "Smith";

String text = json.stringify(profile);
// {"first_name":"Alice","last_name":"Smith"}
```

---

## Date/Time

```java
import java.time.LocalDate;

public class Event {
    String title;
    LocalDate date;
}

Event event = new Event();
event.title = "Launch";
event.date = LocalDate.of(2025, 1, 15);

String text = json.stringify(event);
// {"title":"Launch","date":"2025-01-15"}
```

---

## Error Handling

```java
import io.github.arthurhoch.kissjson.JsonParseException;
import io.github.arthurhoch.kissjson.JsonMappingException;

try {
    User user = json.parse(badJson, User.class);
} catch (JsonParseException e) {
    System.err.println("JSON syntax error at line " + e.line()
        + ", column " + e.column() + ": " + e.getMessage());
} catch (JsonMappingException e) {
    System.err.println("Mapping error at " + e.jsonPath()
        + ": " + e.getMessage());
}
```

---

## Default Behavior

KissJson's defaults are designed to be **forgiving and practical**:

| Behavior | Default | Why |
|----------|---------|-----|
| Unknown JSON properties | **Ignored** | JSON often has extra fields; failing would be fragile |
| Missing fields | **OK** (null/default) | Not all JSON has all fields |
| Null → primitive | **Default value** (0, false) | Practical; avoids NPE |
| Null fields in output | **Included** | Lossless round-trip |
| Duplicate keys | **Last wins** | Matches common parsers |
| Cycles | **Detected, throws** | Prevents infinite loops |

You can make any of these stricter via `Json.builder()` options. See [CONFIGURATION.md](CONFIGURATION.md) for all options.

---

## Next Steps

- [API Reference](API.md) — full public API documentation
- [Examples](EXAMPLES.md) — complete examples for all v1 behaviors
- [Configuration](CONFIGURATION.md) — all builder options
- [Error Handling](ERROR_HANDLING.md) — exception hierarchy and rich errors
