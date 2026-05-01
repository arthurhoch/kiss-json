# Examples

> **Note:** All examples below demonstrate the current v1 API for `0.1.0-SNAPSHOT`.

---

## Simple Object

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

// Serialize
String text = json.stringify(user);
// {"name":"Alice","age":30,"active":true}

// Deserialize
User parsed = json.parse(text, User.class);
// parsed.name == "Alice"
// parsed.age == 30
// parsed.active == true
```

---

## Nested Object

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

User user = new User();
user.name = "Alice";
user.address = new Address();
user.address.street = "123 Main St";
user.address.city = "Springfield";
user.address.zipCode = "62701";

String text = json.stringify(user);
// {"name":"Alice","address":{"street":"123 Main St","city":"Springfield","zipCode":"62701"}}

User parsed = json.parse(text, User.class);
// parsed.address.city == "Springfield"
```

---

## List of Objects

```java
User alice = new User();
alice.name = "Alice";
alice.age = 30;
alice.active = true;

User bob = new User();
bob.name = "Bob";
bob.age = 25;
bob.active = false;

List<User> users = List.of(alice, bob);
String text = json.stringify(users);
// [{"name":"Alice","age":30,"active":true},{"name":"Bob","age":25,"active":false}]

// Deserialize
String jsonText = """
    [
      {"name":"Alice","age":30},
      {"name":"Bob","age":25}
    ]
    """;
List<User> parsed = json.parseList(jsonText, User.class);
// parsed.size() == 2
// parsed.get(0).name == "Alice"
```

---

## Map Parse

```java
// Untyped map
String text = """
    {"name":"Alice","age":30,"active":true}
    """;
Map<String, Object> map = json.parseMap(text);
// map.get("name") == "Alice" (String)
// map.get("age") == 30 (Integer)
// map.get("active") == true (Boolean)

// Typed map
String usersText = """
    {
      "admin": {"name":"Alice","age":30},
      "guest": {"name":"Bob","age":25}
    }
    """;
Map<String, User> users = json.parseMap(usersText, User.class);
// users.get("admin").name == "Alice"
// users.get("guest").name == "Bob"
```

---

## All Primitive Types

```java
public class Primitives {
    byte b;
    short s;
    int i;
    long l;
    float f;
    double d;
    boolean bool;
    char c;
}

Primitives p = new Primitives();
p.b = 1;
p.s = 2;
p.i = 3;
p.l = 4L;
p.f = 5.5f;
p.d = 6.6;
p.bool = true;
p.c = 'A';

String text = json.stringify(p);
// {"b":1,"s":2,"i":3,"l":4,"f":5.5,"d":6.6,"bool":true,"c":"A"}

Primitives parsed = json.parse(text, Primitives.class);
// parsed.b == 1, parsed.s == 2, parsed.i == 3, parsed.l == 4
// parsed.f == 5.5f, parsed.d == 6.6, parsed.bool == true, parsed.c == 'A'
```

---

## BigDecimal and BigInteger

```java
public class Numbers {
    BigDecimal decimal;
    BigInteger integer;
}

Numbers n = new Numbers();
n.decimal = new BigDecimal("3.141592653589793");
n.integer = new BigInteger("12345678901234567890");

String text = json.stringify(n);
// {"decimal":3.141592653589793,"integer":12345678901234567890}

Numbers parsed = json.parse(text, Numbers.class);
// parsed.decimal.compareTo(new BigDecimal("3.141592653589793")) == 0
```

---

## char / Character

```java
public class CharExample {
    char primitive;
    Character wrapper;
}

CharExample c = new CharExample();
c.primitive = 'A';
c.wrapper = 'B';

String text = json.stringify(c);
// {"primitive":"A","wrapper":"B"}

CharExample parsed = json.parse(text, CharExample.class);
// parsed.primitive == 'A'
// parsed.wrapper == 'B'
```

---

## Enum Field

```java
public enum Status {
    ACTIVE, INACTIVE, PENDING
}

public class User {
    String name;
    Status status;
}

User user = new User();
user.name = "Alice";
user.status = Status.ACTIVE;

String text = json.stringify(user);
// {"name":"Alice","status":"ACTIVE"}

User parsed = json.parse(text, User.class);
// parsed.status == Status.ACTIVE
```

---

## Array Field

```java
public class Team {
    String name;
    String[] members;
}

Team team = new Team();
team.name = "Alpha";
team.members = new String[]{"Alice", "Bob", "Charlie"};

String text = json.stringify(team);
// {"name":"Alpha","members":["Alice","Bob","Charlie"]}

Team parsed = json.parse(text, Team.class);
// parsed.members[0] == "Alice"
// parsed.members.length == 3
```

---

## List Field

```java
public class Group {
    String name;
    List<String> tags;
}

Group group = new Group();
group.name = "developers";
group.tags = List.of("java", "json", "kiss");

String text = json.stringify(group);
// {"name":"developers","tags":["java","json","kiss"]}

Group parsed = json.parse(text, Group.class);
// parsed.tags.get(0) == "java"
```

---

## Map Field

```java
public class Config {
    String name;
    Map<String, String> properties;
}

Config config = new Config();
config.name = "app";
config.properties = Map.of("timeout", "30", "retries", "3");

String text = json.stringify(config);
// {"name":"app","properties":{"timeout":"30","retries":"3"}}

Config parsed = json.parse(text, Config.class);
// parsed.properties.get("timeout") == "30"
```

---

## @JsonName

```java
import io.github.arthurhoch.kissjson.JsonName;

public class User {
    @JsonName("user_name")
    String name;

    @JsonName("user_age")
    int age;
}

User user = new User();
user.name = "Alice";
user.age = 30;

String text = json.stringify(user);
// {"user_name":"Alice","user_age":30}

String input = """
    {"user_name":"Bob","user_age":25}
    """;
User parsed = json.parse(input, User.class);
// parsed.name == "Bob"
// parsed.age == 25
```

---

## @JsonAliases

```java
import io.github.arthurhoch.kissjson.JsonAliases;

public class User {
    @JsonAliases({"userName", "user_name", "username"})
    String name;
}

// All of these parse correctly:
json.parse("{\"name\":\"Alice\"}", User.class);
json.parse("{\"userName\":\"Alice\"}", User.class);
json.parse("{\"user_name\":\"Alice\"}", User.class);
json.parse("{\"username\":\"Alice\"}", User.class);
```

---

## @JsonIgnore

```java
import io.github.arthurhoch.kissjson.JsonIgnore;

public class User {
    String name;
    @JsonIgnore String password;
}

User user = new User();
user.name = "Alice";
user.password = "secret123";

String text = json.stringify(user);
// {"name":"Alice"}
// password is not included

String input = """
    {"name":"Bob","password":"hack"}
    """;
User parsed = json.parse(input, User.class);
// parsed.name == "Bob"
// parsed.password == null (ignored during deserialization)
```

---

## @JsonRequired

```java
import io.github.arthurhoch.kissjson.JsonRequired;

public class User {
    @JsonRequired String email;
    String name;
}

// OK
json.parse("{\"email\":\"alice@example.com\",\"name\":\"Alice\"}", User.class);

Json strict = Json.builder().failOnMissingRequiredFields(true).build();

// Throws JsonMappingException — email is missing
strict.parse("{\"name\":\"Alice\"}", User.class);
// JsonMappingException: Required field 'email' is missing at $ [target=User, field=email]
```

---

## @JsonIncludeNull / @JsonExcludeNull

```java
import io.github.arthurhoch.kissjson.JsonIncludeNull;
import io.github.arthurhoch.kissjson.JsonExcludeNull;

public class User {
    String name;
    @JsonIncludeNull String middleName;
    @JsonExcludeNull String nickname;
}
```

With `includeNulls = false`:

```java
Json json = Json.builder().includeNulls(false).build();

User user = new User();
user.name = "Alice";
// middleName and nickname are null

String text = json.stringify(user);
// {"name":"Alice","middleName":null}
// nickname is excluded (null + @JsonExcludeNull)
// middleName is included (null + @JsonIncludeNull overrides includeNulls=false)
```

With `includeNulls = true` (default):

```java
Json json = Json.create();

User user = new User();
user.name = "Alice";

String text = json.stringify(user);
// {"name":"Alice","middleName":null}
// nickname is excluded because @JsonExcludeNull overrides includeNulls=true
```

---

## @JsonDateFormat

```java
import io.github.arthurhoch.kissjson.JsonDateFormat;
import java.time.LocalDate;

public class Event {
    String title;
    @JsonDateFormat("dd/MM/yyyy")
    LocalDate date;
}

Event event = new Event();
event.title = "Launch";
event.date = LocalDate.of(2025, 1, 15);

String text = json.stringify(event);
// {"title":"Launch","date":"15/01/2025"}

Event parsed = json.parse(text, Event.class);
// parsed.date == LocalDate.of(2025, 1, 15)
```

---

## FieldNaming Strategies

```java
import io.github.arthurhoch.kissjson.FieldNaming;

public class UserProfile {
    String firstName;
    String lastName;
    int zipCode;
}

UserProfile profile = new UserProfile();
profile.firstName = "Alice";
profile.lastName = "Smith";
profile.zipCode = 62701;
```

### IDENTITY (default)

```java
Json json = Json.builder().fieldNaming(FieldNaming.IDENTITY).build();
// {"firstName":"Alice","lastName":"Smith","zipCode":62701}
```

### LOWER_CASE

```java
Json json = Json.builder().fieldNaming(FieldNaming.LOWER_CASE).build();
// {"firstname":"Alice","lastname":"Smith","zipcode":62701}
```

### UPPER_CASE

```java
Json json = Json.builder().fieldNaming(FieldNaming.UPPER_CASE).build();
// {"FIRSTNAME":"Alice","LASTNAME":"Smith","ZIPCODE":62701}
```

### CAMEL_CASE

```java
Json json = Json.builder().fieldNaming(FieldNaming.CAMEL_CASE).build();
// {"firstName":"Alice","lastName":"Smith","zipCode":62701}
```

### SNAKE_CASE

```java
Json json = Json.builder().fieldNaming(FieldNaming.SNAKE_CASE).build();
// {"first_name":"Alice","last_name":"Smith","zip_code":62701}
```

### KEBAB_CASE

```java
Json json = Json.builder().fieldNaming(FieldNaming.KEBAB_CASE).build();
// {"first-name":"Alice","last-name":"Smith","zip-code":62701}
```

---

## DateFormat

### ISO (default)

```java
import io.github.arthurhoch.kissjson.DateFormat;

public class Event {
    String title;
    Instant timestamp;
}

Json json = Json.builder().dateFormat(DateFormat.ISO).build();

Event event = new Event();
event.title = "Launch";
event.timestamp = Instant.parse("2025-01-15T10:30:00Z");

String text = json.stringify(event);
// {"title":"Launch","timestamp":"2025-01-15T10:30:00Z"}
```

### EPOCH_MILLIS

```java
Json json = Json.builder().dateFormat(DateFormat.EPOCH_MILLIS).build();
// Instant, OffsetDateTime, ZonedDateTime, Date, and Calendar serialize as long epoch millisecond values.
// LocalDate, LocalTime, LocalDateTime, Duration, and Period remain ISO strings.
```

### EPOCH_SECONDS

```java
Json json = Json.builder().dateFormat(DateFormat.EPOCH_SECONDS).build();
// Instant, OffsetDateTime, ZonedDateTime, Date, and Calendar serialize as long epoch second values.
// LocalDate, LocalTime, LocalDateTime, Duration, and Period remain ISO strings.
```

---

## EnumMode

### NAME (default)

```java
import io.github.arthurhoch.kissjson.EnumMode;

public enum Priority {
    HIGH, MEDIUM, LOW
}

public class Task {
    String title;
    Priority priority;
}

Json json = Json.builder().enumMode(EnumMode.NAME).build();

Task task = new Task();
task.title = "Deploy";
task.priority = Priority.HIGH;

String text = json.stringify(task);
// {"title":"Deploy","priority":"HIGH"}

Task parsed = json.parse(text, Task.class);
// parsed.priority == Priority.HIGH
```

### TO_STRING

```java
public enum Priority {
    HIGH("high-priority"),
    MEDIUM("medium-priority"),
    LOW("low-priority");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

Json json = Json.builder().enumMode(EnumMode.TO_STRING).build();

Task task = new Task();
task.title = "Deploy";
task.priority = Priority.HIGH;

String text = json.stringify(task);
// {"title":"Deploy","priority":"high-priority"}
```

---

## Pretty Print

```java
User user = new User();
user.name = "Alice";
user.age = 30;
user.active = true;

Json json = Json.builder().prettyPrint(true).build();

String text = json.stringify(user);
// {
//   "name": "Alice",
//   "age": 30,
//   "active": true
// }
```

---

## Cycle Detection

```java
public class Node {
    String value;
    Node next;
}

Node a = new Node();
a.value = "A";
Node b = new Node();
b.value = "B";
a.next = b;
b.next = a; // cycle!

Json json = Json.create();

try {
    json.stringify(a);
} catch (JsonException e) {
    // Cycle detected: A -> B -> A
    System.err.println(e.getMessage());
}

// Disable cycle detection (NOT recommended)
Json lenient = Json.builder().failOnCycles(false).build();
// Will throw StackOverflowError instead
```

---

## Max Depth

```java
public class Node {
    String value;
    Node child;
}

// Build a deeply nested structure
Node root = new Node();
Node current = root;
for (int i = 0; i < 200; i++) {
    current.value = "level" + i;
    current.child = new Node();
    current = current.child;
}

Json json = Json.builder().maxDepth(128).build();

try {
    json.stringify(root);
} catch (JsonException e) {
    // Max depth 128 exceeded
    System.err.println(e.getMessage());
}
```

---

## Null Handling (Include/Exclude)

### Include nulls (default)

```java
public class User {
    String name;
    String email;
}

User user = new User();
user.name = "Alice";
// email is null

Json json = Json.create();
String text = json.stringify(user);
// {"name":"Alice","email":null}
```

### Exclude nulls

```java
Json json = Json.builder().includeNulls(false).build();

String text = json.stringify(user);
// {"name":"Alice"}
```

---

## Unknown Properties (Ignore/Fail)

### Ignore (default)

```java
public class User {
    String name;
}

String input = """
    {"name":"Alice","phone":"555-1234","role":"admin"}
    """;

User user = json.parse(input, User.class);
// user.name == "Alice"
// phone and role are silently ignored
```

### Fail

```java
Json strict = Json.builder().failOnUnknownProperties(true).build();

try {
    strict.parse(input, User.class);
} catch (JsonMappingException e) {
    // Unknown property 'phone' at $ [target=User]
    System.err.println(e.getMessage());
}
```

---

## Duplicate Keys (Last Wins / Fail)

### Last wins (default)

```java
String input = """
    {"name":"Alice","name":"Bob"}
    """;

User user = json.parse(input, User.class);
// user.name == "Bob" (last value wins)
```

### Fail

```java
Json strict = Json.builder().failOnDuplicateKeys(true).build();

try {
    strict.parse(input, User.class);
} catch (JsonParseException e) {
    // Duplicate key 'name' at line 1, column 22
    System.err.println(e.getMessage());
}
```

---

## Date/Time Types

```java
import java.time.*;
import java.util.Date;
import java.util.Calendar;

public class Temporal {
    LocalDate localDate;
    LocalTime localTime;
    LocalDateTime localDateTime;
    OffsetDateTime offsetDateTime;
    ZonedDateTime zonedDateTime;
    Instant instant;
    Duration duration;
    Period period;
    Date utilDate;
    Calendar calendar;
}

Temporal t = new Temporal();
t.localDate = LocalDate.of(2025, 1, 15);
t.localTime = LocalTime.of(10, 30, 0);
t.localDateTime = LocalDateTime.of(2025, 1, 15, 10, 30, 0);
t.offsetDateTime = OffsetDateTime.of(2025, 1, 15, 10, 30, 0, ZoneOffset.ofHours(2));
t.zonedDateTime = ZonedDateTime.of(2025, 1, 15, 10, 30, 0, ZoneId.of("America/Chicago"));
t.instant = Instant.parse("2025-01-15T10:30:00Z");
t.duration = Duration.ofHours(2).plusMinutes(30);
t.period = Period.of(1, 2, 3);
t.utilDate = new Date(1736934600000L);
t.calendar = Calendar.getInstance();

String text = json.stringify(t);
// {
//   "localDate":"2025-01-15",
//   "localTime":"10:30:00",
//   "localDateTime":"2025-01-15T10:30:00",
//   "offsetDateTime":"2025-01-15T10:30:00+02:00",
//   "zonedDateTime":"2025-01-15T10:30:00-06:00[America/Chicago]",
//   "instant":"2025-01-15T10:30:00Z",
//   "duration":"PT2H30M",
//   "period":"P1Y2M3D",
//   "utilDate":"2025-01-15T10:30:00Z",
//   "calendar":"2025-01-15T10:30:00Z"
// }

Temporal parsed = json.parse(text, Temporal.class);
```

---

## Error Handling Examples

### JsonParseException — syntax error

```java
String badJson = """
    {"name": "Alice", "age": }
    """;

try {
    json.parse(badJson, User.class);
} catch (JsonParseException e) {
    // Unexpected character '}' at line 1, column 27 (offset 26)
    System.out.println("Line:   " + e.line());     // 1
    System.out.println("Column: " + e.column());   // 27
    System.out.println("Offset: " + e.offset());   // 26
    System.out.println("Message: " + e.getMessage());
}
```

### JsonMappingException — type mismatch

```java
String wrongType = """
    {"name":"Alice","age":"thirty"}
    """;

try {
    json.parse(wrongType, User.class);
} catch (JsonMappingException e) {
    // Cannot map STRING to int at $.age [target=User, field=age, expected=int, actual="thirty"]
    System.out.println("Path:     " + e.jsonPath());     // $.age
    System.out.println("Target:   " + e.targetType());   // class User
    System.out.println("Field:    " + e.fieldName());    // age
    System.out.println("Expected: " + e.expectedType()); // int
    System.out.println("Actual:   " + e.actualValue());  // "thirty"
}
```

### JsonMappingException — missing required field

```java
String missing = """
    {"name":"Alice"}
    """;

public class RequiredUser {
    String name;
    @JsonRequired String email;
}

try {
    Json strict = Json.builder().failOnMissingRequiredFields(true).build();
    strict.parse(missing, RequiredUser.class);
} catch (JsonMappingException e) {
    // Required field 'email' is missing at $ [target=RequiredUser, field=email]
    System.out.println(e.getMessage());
}
```

---

## Superclass Fields

```java
public class BaseEntity {
    long id;
    String createdAt;
}

public class User extends BaseEntity {
    String name;
    String email;
}

User user = new User();
user.id = 1;
user.createdAt = "2025-01-15";
user.name = "Alice";
user.email = "alice@example.com";

String text = json.stringify(user);
// {"id":1,"createdAt":"2025-01-15","name":"Alice","email":"alice@example.com"}
// BaseEntity fields are included
```

---

## Private Fields

```java
public class User {
    private String name;
    private int age;
}

// Private fields are mapped via setAccessible(true)
String text = """
    {"name":"Alice","age":30}
    """;

User user = json.parse(text, User.class);
// user.name == "Alice" (field set via reflection)
// user.age == 30
```

---

*All examples above demonstrate the current v1 API for `0.1.0-SNAPSHOT`.*
