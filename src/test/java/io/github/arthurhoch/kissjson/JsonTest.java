package io.github.arthurhoch.kissjson;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    private final Json json = Json.create();

    enum Color { RED, GREEN, BLUE }

    enum Status {
        ACTIVE("active"), INACTIVE("inactive");
        private final String label;
        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    static class User {
        String name;
        int age;
        boolean active;
        User() {}
    }

    static class Address {
        String city;
        String country;
        Address() {}
    }

    static class UserWithAddress {
        String name;
        Address address;
        UserWithAddress() {}
    }

    static class WrapperHolder {
        Integer intVal;
        Long longVal;
        Double doubleVal;
        Float floatVal;
        Boolean boolVal;
        Short shortVal;
        Byte byteVal;
        WrapperHolder() {}
    }

    static class BigDecimalHolder {
        BigDecimal value;
        BigDecimalHolder() {}
    }

    static class BigIntegerHolder {
        BigInteger value;
        BigIntegerHolder() {}
    }

    static class CharHolder {
        char c;
        Character bigC;
        CharHolder() {}
    }

    static class EnumHolder {
        Color color;
        EnumHolder() {}
    }

    static class EnumToStringHolder {
        Status status;
        EnumToStringHolder() {}
    }

    static class ArrayHolder {
        int[] ints;
        String[] strings;
        ArrayHolder() {}
    }

    static class ListHolder {
        List<String> names;
        ListHolder() {}
    }

    static class MapHolder {
        Map<String, String> entries;
        MapHolder() {}
    }

    static class PrimitiveHolder {
        int count;
        long value;
        double rate;
        boolean flag;
        PrimitiveHolder() {}
    }

    static class AnnotatedUser {
        @JsonName("full_name") String name;
        @JsonAliases({"fullName", "displayName"}) String alias;
        @JsonIgnore String secret;
        @JsonRequired String id;
        String email;
        AnnotatedUser() {}
    }

    static class IncludeNullOverride {
        @JsonIncludeNull String alwaysIncluded;
        String normalField;
        IncludeNullOverride() {}
    }

    static class ExcludeNullOverride {
        @JsonExcludeNull String alwaysExcluded;
        String normalField;
        ExcludeNullOverride() {}
    }

    static class Parent {
        String parentField;
        Parent() {}
    }

    static class Child extends Parent {
        private String childField;
        private Child() {}
    }

    static class Node {
        String value;
        Node next;
        Node() {}
    }

    static class DateHolder {
        LocalDate localDate;
        LocalTime localTime;
        LocalDateTime localDateTime;
        OffsetDateTime offsetDateTime;
        ZonedDateTime zonedDateTime;
        Instant instant;
        Duration duration;
        Period period;
        Date legacyDate;
        Calendar calendar;
        DateHolder() {}
    }

    static class CustomDateHolder {
        @JsonDateFormat("dd/MM/yyyy") LocalDate date;
        CustomDateHolder() {}
    }

    static class EpochHolder {
        Instant instant;
        EpochHolder() {}
    }

    static class NamingSnakeUser {
        String userName;
        NamingSnakeUser() {}
    }

    static class NamingKebabUser {
        String userName;
        NamingKebabUser() {}
    }

    static class NamingLowerUser {
        String userName;
        NamingLowerUser() {}
    }

    static class NamingUpperUser {
        String userName;
        NamingUpperUser() {}
    }

    static class NamingCamelUser {
        String user_name;
        NamingCamelUser() {}
    }

    static class StaticTransientHolder {
        static String className = "test";
        transient String temp;
        String name;
        StaticTransientHolder() {}
    }

    @Nested
    @DisplayName("Basic API")
    class BasicApi {

        @Test
        @DisplayName("Json.create() returns non-null")
        void createReturnsNonNull() {
            assertNotNull(Json.create());
        }

        @Test
        @DisplayName("Json.builder() returns non-null builder")
        void builderReturnsNonNull() {
            assertNotNull(Json.builder());
        }

        @Test
        @DisplayName("Json.builder().build() creates instance")
        void builderBuildCreatesInstance() {
            assertNotNull(Json.builder().build());
        }

        @Test
        @DisplayName("Json.config() returns config with correct defaults")
        void configReturnsCorrectDefaults() {
            JsonConfig cfg = json.config();
            assertEquals(FieldNaming.IDENTITY, cfg.fieldNaming());
            assertTrue(cfg.includeNulls());
            assertFalse(cfg.failOnUnknownProperties());
            assertFalse(cfg.failOnMissingRequiredFields());
            assertFalse(cfg.failOnNullForPrimitives());
            assertFalse(cfg.failOnDuplicateKeys());
            assertTrue(cfg.failOnCycles());
            assertEquals(128, cfg.maxDepth());
            assertFalse(cfg.prettyPrint());
            assertEquals(DateFormat.ISO, cfg.dateFormat());
            assertEquals(ZoneId.of("UTC"), cfg.zoneId());
            assertEquals(EnumMode.NAME, cfg.enumMode());
        }
    }

    @Nested
    @DisplayName("Stringify primitives and null")
    class StringifyPrimitives {

        @Test
        @DisplayName("stringify(null) returns \"null\"")
        void stringifyNull() {
            assertEquals("null", json.stringify(null));
        }

        @Test
        @DisplayName("stringify(\"hello\") returns quoted string")
        void stringifyString() {
            assertEquals("\"hello\"", json.stringify("hello"));
        }

        @Test
        @DisplayName("stringify(42) returns \"42\"")
        void stringifyInt() {
            assertEquals("42", json.stringify(42));
        }

        @Test
        @DisplayName("stringify(true) returns \"true\"")
        void stringifyBoolean() {
            assertEquals("true", json.stringify(true));
        }

        @Test
        @DisplayName("stringify(3.14) returns \"3.14\"")
        void stringifyDouble() {
            assertEquals("3.14", json.stringify(3.14));
        }

        @Test
        @DisplayName("stringify(Long.MAX_VALUE) returns correct number")
        void stringifyLong() {
            assertEquals(String.valueOf(Long.MAX_VALUE), json.stringify(Long.MAX_VALUE));
        }
    }

    @Nested
    @DisplayName("POJO round-trip")
    class PojoRoundTrip {

        @Test
        @DisplayName("Simple POJO with String, int, boolean fields round-trips")
        void simplePojoRoundTrip() {
            User user = new User();
            user.name = "Alice";
            user.age = 30;
            user.active = true;

            String result = json.stringify(user);
            assertEquals("{\"name\":\"Alice\",\"age\":30,\"active\":true}", result);

            User parsed = json.parse(result, User.class);
            assertEquals("Alice", parsed.name);
            assertEquals(30, parsed.age);
            assertTrue(parsed.active);
        }

        @Test
        @DisplayName("Null POJO fields are included by default")
        void nullFieldsIncludedByDefault() {
            User user = new User();
            String result = json.stringify(user);
            assertTrue(result.contains("\"name\":null"));
        }

        @Test
        @DisplayName("Null POJO fields are excluded when includeNulls=false")
        void nullFieldsExcludedWhenConfigured() {
            Json excludeNull = Json.builder().includeNulls(false).build();
            User user = new User();
            String result = excludeNull.stringify(user);
            assertFalse(result.contains("\"name\""));
            assertTrue(result.contains("\"age\":0"));
            assertTrue(result.contains("\"active\":false"));
        }
    }

    @Nested
    @DisplayName("Nested objects")
    class NestedObjects {

        @Test
        @DisplayName("Nested object serialization and deserialization")
        void nestedObjectRoundTrip() {
            Address addr = new Address();
            addr.city = "NYC";
            addr.country = "US";

            UserWithAddress outer = new UserWithAddress();
            outer.name = "Bob";
            outer.address = addr;

            String result = json.stringify(outer);
            assertEquals("{\"name\":\"Bob\",\"address\":{\"city\":\"NYC\",\"country\":\"US\"}}", result);

            UserWithAddress parsed = json.parse(result, UserWithAddress.class);
            assertEquals("Bob", parsed.name);
            assertNotNull(parsed.address);
            assertEquals("NYC", parsed.address.city);
            assertEquals("US", parsed.address.country);
        }

        @Test
        @DisplayName("Null nested object is included as null")
        void nullNestedObject() {
            UserWithAddress outer = new UserWithAddress();
            outer.name = "Bob";
            outer.address = null;

            String result = json.stringify(outer);
            assertTrue(result.contains("\"address\":null"));
        }
    }

    @Nested
    @DisplayName("Collections")
    class Collections {

        @Test
        @DisplayName("List serialization and parseList")
        void listRoundTrip() {
            List<String> names = List.of("Alice", "Bob", "Charlie");
            String result = json.stringify(names);
            assertEquals("[\"Alice\",\"Bob\",\"Charlie\"]", result);

            List<String> parsed = json.parseList(result, String.class);
            assertEquals(List.of("Alice", "Bob", "Charlie"), parsed);
        }

        @Test
        @DisplayName("parseList with Integer elements")
        void parseListOfInteger() {
            List<Integer> parsed = json.parseList("[1,2,3]", Integer.class);
            assertEquals(List.of(1, 2, 3), parsed);
        }

        @Test
        @DisplayName("Map serialization and parseMap (untyped)")
        void mapUntypedRoundTrip() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Alice");
            map.put("age", 30);
            map.put("active", true);

            String result = json.stringify(map);
            assertEquals("{\"name\":\"Alice\",\"age\":30,\"active\":true}", result);

            Map<String, Object> parsed = json.parseMap(result);
            assertEquals("Alice", parsed.get("name"));
            assertEquals(30L, parsed.get("age"));
            assertEquals(true, parsed.get("active"));
        }

        @Test
        @DisplayName("parseMap with typed values")
        void mapTypedRoundTrip() {
            Map<String, Integer> parsed = json.parseMap("{\"a\":1,\"b\":2,\"c\":3}", Integer.class);
            assertEquals(1, parsed.get("a"));
            assertEquals(2, parsed.get("b"));
            assertEquals(3, parsed.get("c"));
        }

        @Test
        @DisplayName("Map serialization rejects non-string keys")
        void mapSerializationRejectsNonStringKeys() {
            Map<Object, String> map = new LinkedHashMap<>();
            map.put(1, "one");

            assertThrows(JsonException.class, () -> json.stringify(map));
        }

        @Test
        @DisplayName("parseList with POJO elements")
        void parseListOfPojos() {
            String arr = "[{\"name\":\"Alice\",\"age\":30,\"active\":true},{\"name\":\"Bob\",\"age\":25,\"active\":false}]";
            List<User> users = json.parseList(arr, User.class);
            assertEquals(2, users.size());
            assertEquals("Alice", users.get(0).name);
            assertEquals(30, users.get(0).age);
            assertEquals("Bob", users.get(1).name);
        }

        @Test
        @DisplayName("parseList maps nested object values directly")
        void parseListWithNestedObjectValues() {
            String arr = "[{\"name\":\"Alice\",\"address\":{\"city\":\"NYC\",\"country\":\"US\"}}]";
            List<UserWithAddress> users = json.parseList(arr, UserWithAddress.class);
            assertEquals(1, users.size());
            assertEquals("Alice", users.get(0).name);
            assertEquals("NYC", users.get(0).address.city);
            assertEquals("US", users.get(0).address.country);
        }

        @Test
        @DisplayName("parseMap maps typed nested object values directly")
        void parseMapWithTypedNestedObjectValues() {
            String input = "{\"u1\":{\"name\":\"Alice\",\"address\":{\"city\":\"NYC\",\"country\":\"US\"}}}";
            Map<String, UserWithAddress> users = json.parseMap(input, UserWithAddress.class);
            assertEquals("Alice", users.get("u1").name);
            assertEquals("NYC", users.get("u1").address.city);
        }

        @Test
        @DisplayName("parseMap keeps nested untyped arrays and objects")
        void parseMapUntypedNestedValues() {
            String input = "{\"outer\":{\"items\":[{\"name\":\"a\"},{\"name\":\"b\"}],\"count\":2}}";
            Map<String, Object> result = json.parseMap(input);
            assertInstanceOf(Map.class, result.get("outer"));
            @SuppressWarnings("unchecked")
            Map<String, Object> outer = (Map<String, Object>) result.get("outer");
            assertInstanceOf(List.class, outer.get("items"));
            assertEquals(2L, outer.get("count"));
        }

        @Test
        @DisplayName("List<String> field in POJO")
        void listFieldInPojo() {
            ListHolder holder = new ListHolder();
            holder.names = List.of("a", "b", "c");
            String result = json.stringify(holder);
            assertEquals("{\"names\":[\"a\",\"b\",\"c\"]}", result);

            ListHolder parsed = json.parse("{\"names\":[\"a\",\"b\",\"c\"]}", ListHolder.class);
            assertEquals(List.of("a", "b", "c"), parsed.names);
        }

        @Test
        @DisplayName("Map<String, String> field in POJO")
        void mapFieldInPojo() {
            MapHolder holder = new MapHolder();
            holder.entries = new LinkedHashMap<>();
            holder.entries.put("key", "value");
            String result = json.stringify(holder);
            assertEquals("{\"entries\":{\"key\":\"value\"}}", result);

            MapHolder parsed = json.parse("{\"entries\":{\"key\":\"value\"}}", MapHolder.class);
            assertEquals("value", parsed.entries.get("key"));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("parse throws NullPointerException for null JSON string")
        void parseNullJson() {
            assertThrows(NullPointerException.class, () -> json.parse(null, User.class));
        }

        @Test
        @DisplayName("parse throws NullPointerException for null target type")
        void parseNullType() {
            assertThrows(NullPointerException.class, () -> json.parse("{}", null));
        }

        @Test
        @DisplayName("parse throws JsonParseException for invalid JSON")
        void parseInvalidJson() {
            assertThrows(JsonParseException.class, () -> json.parse("{invalid}", User.class));
        }

        @Test
        @DisplayName("parse throws JsonParseException for truncated JSON")
        void parseTruncatedJson() {
            assertThrows(JsonParseException.class, () -> json.parse("{\"name\":", User.class));
        }

        @Test
        @DisplayName("parseList rejects trailing content")
        void parseListRejectsTrailingContent() {
            assertThrows(JsonParseException.class, () -> json.parseList("[1,2] true", Integer.class));
        }

        @Test
        @DisplayName("parseMap rejects trailing content")
        void parseMapRejectsTrailingContent() {
            assertThrows(JsonParseException.class, () -> json.parseMap("{\"a\":1} []"));
        }

        @Test
        @DisplayName("Trailing commas are rejected")
        void trailingCommasRejected() {
            assertThrows(JsonParseException.class, () -> json.parse("{\"name\":\"Alice\",}", User.class));
            assertThrows(JsonParseException.class, () -> json.parseList("[1,]", Integer.class));
        }

        @Test
        @DisplayName("parse null JSON value returns null object")
        void parseNullJsonValue() {
            User result = json.parse("null", User.class);
            assertNull(result);
        }

        @Test
        @DisplayName("parseList throws NullPointerException for null JSON")
        void parseListNullJson() {
            assertThrows(NullPointerException.class, () -> json.parseList(null, String.class));
        }

        @Test
        @DisplayName("parseMap throws NullPointerException for null JSON")
        void parseMapNullJson() {
            assertThrows(NullPointerException.class, () -> json.parseMap(null));
        }

        @Test
        @DisplayName("JsonParseException contains line, column, offset")
        void parseExceptionHasLocationInfo() {
            JsonParseException ex = assertThrows(JsonParseException.class,
                    () -> json.parse("{bad}", User.class));
            assertTrue(ex.line() > 0);
            assertTrue(ex.column() > 0);
            assertTrue(ex.offset() >= 0);
        }

        @Test
        @DisplayName("Invalid nested JSON reports location while skipping")
        void invalidNestedJsonReportsLocationWhileSkipping() {
            JsonParseException ex = assertThrows(JsonParseException.class,
                    () -> json.parse("{\"name\":\"Alice\",\"ignored\":{\"a\" 1}}", User.class));
            assertTrue(ex.line() > 0);
            assertTrue(ex.column() > 0);
            assertTrue(ex.offset() >= 0);
        }
    }

    @Nested
    @DisplayName("Wrapper types")
    class WrapperTypes {

        @Test
        @DisplayName("Round-trip with all wrapper types")
        void wrapperTypesRoundTrip() {
            WrapperHolder holder = new WrapperHolder();
            holder.intVal = 42;
            holder.longVal = 123456789L;
            holder.doubleVal = 3.14;
            holder.floatVal = 2.5f;
            holder.boolVal = true;
            holder.shortVal = 100;
            holder.byteVal = 10;

            String result = json.stringify(holder);
            WrapperHolder parsed = json.parse(result, WrapperHolder.class);

            assertEquals(42, parsed.intVal);
            assertEquals(123456789L, parsed.longVal);
            assertEquals(3.14, parsed.doubleVal, 0.001);
            assertEquals(2.5f, parsed.floatVal, 0.001);
            assertTrue(parsed.boolVal);
            assertEquals((short) 100, parsed.shortVal);
            assertEquals((byte) 10, parsed.byteVal);
        }

        @Test
        @DisplayName("Null wrapper types are preserved as null")
        void nullWrapperTypes() {
            WrapperHolder holder = new WrapperHolder();
            String result = json.stringify(holder);
            WrapperHolder parsed = json.parse(result, WrapperHolder.class);
            assertNull(parsed.intVal);
            assertNull(parsed.longVal);
            assertNull(parsed.doubleVal);
            assertNull(parsed.boolVal);
        }
    }

    @Nested
    @DisplayName("BigDecimal and BigInteger")
    class BigNumbers {

        @Test
        @DisplayName("BigDecimal serialization and deserialization")
        void bigDecimalRoundTrip() {
            BigDecimalHolder holder = new BigDecimalHolder();
            holder.value = new BigDecimal("123456789.123456789");
            String result = json.stringify(holder);
            assertTrue(result.contains("123456789.123456789"));

            BigDecimalHolder parsed = json.parse(result, BigDecimalHolder.class);
            assertEquals(new BigDecimal("123456789.123456789"), parsed.value);
        }

        @Test
        @DisplayName("BigInteger serialization and deserialization")
        void bigIntegerRoundTrip() {
            BigIntegerHolder holder = new BigIntegerHolder();
            holder.value = new BigInteger("999999999999999999999999999");
            String result = json.stringify(holder);
            assertTrue(result.contains("999999999999999999999999999"));

            BigIntegerHolder parsed = json.parse(result, BigIntegerHolder.class);
            assertEquals(new BigInteger("999999999999999999999999999"), parsed.value);
        }

        @Test
        @DisplayName("BigDecimal negative value")
        void bigDecimalNegative() {
            BigDecimalHolder holder = new BigDecimalHolder();
            holder.value = new BigDecimal("-99.99");
            String result = json.stringify(holder);
            BigDecimalHolder parsed = json.parse(result, BigDecimalHolder.class);
            assertEquals(new BigDecimal("-99.99"), parsed.value);
        }

        @Test
        @DisplayName("Decimal numbers are rejected for integer targets")
        void decimalNumberRejectedForIntegerTarget() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"count\":1.25}", PrimitiveHolder.class));
        }

        @Test
        @DisplayName("Decimal numbers are rejected for BigInteger targets")
        void decimalNumberRejectedForBigIntegerTarget() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"value\":1.25}", BigIntegerHolder.class));
        }

        @Test
        @DisplayName("Oversized numbers are rejected for long targets")
        void oversizedNumberRejectedForLongTarget() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"value\":9223372036854775808}", PrimitiveHolder.class));
        }
    }

    @Nested
    @DisplayName("char and Character")
    class CharTests {

        @Test
        @DisplayName("char serialized as single-char string")
        void charSerialization() {
            CharHolder holder = new CharHolder();
            holder.c = 'A';
            holder.bigC = 'B';
            String result = json.stringify(holder);
            assertTrue(result.contains("\"c\":\"A\""));
            assertTrue(result.contains("\"bigC\":\"B\""));
        }

        @Test
        @DisplayName("char deserialized from single-char string")
        void charRoundTrip() {
            CharHolder parsed = json.parse("{\"c\":\"X\",\"bigC\":\"Y\"}", CharHolder.class);
            assertEquals('X', parsed.c);
            assertEquals(Character.valueOf('Y'), parsed.bigC);
        }

        @Test
        @DisplayName("char deserialization rejects multi-character strings")
        void charRejectsMultiCharacterString() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"c\":\"XY\"}", CharHolder.class));
        }

        @Test
        @DisplayName("Character deserialization rejects empty strings")
        void characterRejectsEmptyString() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"bigC\":\"\"}", CharHolder.class));
        }
    }

    @Nested
    @DisplayName("Enum handling")
    class EnumTests {

        @Test
        @DisplayName("Enum NAME mode (default) round-trip")
        void enumNameMode() {
            EnumHolder holder = new EnumHolder();
            holder.color = Color.GREEN;
            String result = json.stringify(holder);
            assertTrue(result.contains("\"color\":\"GREEN\""));

            EnumHolder parsed = json.parse(result, EnumHolder.class);
            assertEquals(Color.GREEN, parsed.color);
        }

        @Test
        @DisplayName("Enum TO_STRING mode round-trip")
        void enumToStringMode() {
            Json toStringJson = Json.builder().enumMode(EnumMode.TO_STRING).build();
            EnumToStringHolder holder = new EnumToStringHolder();
            holder.status = Status.ACTIVE;
            String result = toStringJson.stringify(holder);
            assertTrue(result.contains("\"status\":\"active\""));

            EnumToStringHolder parsed = toStringJson.parse(result, EnumToStringHolder.class);
            assertEquals(Status.ACTIVE, parsed.status);
        }

        @Test
        @DisplayName("Unknown enum value throws JsonMappingException")
        void unknownEnumValue() {
            assertThrows(JsonMappingException.class,
                    () -> json.parse("{\"color\":\"PURPLE\"}", EnumHolder.class));
        }
    }

    @Nested
    @DisplayName("String escaping")
    class StringEscaping {

        @Test
        @DisplayName("Quotes are escaped")
        void escapeQuotes() {
            assertEquals("\"hello \\\"world\\\"\"", json.stringify("hello \"world\""));
        }

        @Test
        @DisplayName("Backslash is escaped")
        void escapeBackslash() {
            assertEquals("\"path\\\\file\"", json.stringify("path\\file"));
        }

        @Test
        @DisplayName("Newlines are escaped")
        void escapeNewlines() {
            assertEquals("\"line1\\nline2\"", json.stringify("line1\nline2"));
        }

        @Test
        @DisplayName("Tabs are escaped")
        void escapeTabs() {
            assertEquals("\"col1\\tcol2\"", json.stringify("col1\tcol2"));
        }

        @Test
        @DisplayName("Carriage return is escaped")
        void escapeCr() {
            assertEquals("\"a\\rb\"", json.stringify("a\rb"));
        }

        @Test
        @DisplayName("Unicode characters are preserved")
        void escapeUnicode() {
            String result = json.stringify("caf\u00e9");
            assertEquals("\"caf\u00e9\"", result);
            assertEquals("caf\u00e9", json.parse(result, String.class));
        }

        @Test
        @DisplayName("Escaped string round-trips through parse")
        void escapedStringRoundTrip() {
            String original = "hello \"world\" with \\backslash\\ and \nnewline";
            String serialized = json.stringify(original);
            String deserialized = json.parse(serialized, String.class);
            assertEquals(original, deserialized);
        }

        @Test
        @DisplayName("Unicode surrogate pair escapes parse correctly")
        void unicodeSurrogatePairEscapesParse() {
            String parsed = json.parse("\"\\uD834\\uDD1E\"", String.class);
            assertEquals("\uD834\uDD1E", parsed);
        }

        @Test
        @DisplayName("Non-BMP Unicode string round-trips")
        void nonBmpUnicodeRoundTrip() {
            String original = "\uD83D\uDE00";
            String serialized = json.stringify(original);
            String deserialized = json.parse(serialized, String.class);
            assertEquals(original, deserialized);
        }
    }

    @Nested
    @DisplayName("Arrays")
    class ArrayTests {

        @Test
        @DisplayName("int[] serialization and deserialization")
        void intArrayRoundTrip() {
            ArrayHolder holder = new ArrayHolder();
            holder.ints = new int[]{1, 2, 3};
            holder.strings = null;

            String result = json.stringify(holder);
            assertTrue(result.contains("\"ints\":[1,2,3]"));

            ArrayHolder parsed = json.parse("{\"ints\":[4,5,6]}", ArrayHolder.class);
            assertArrayEquals(new int[]{4, 5, 6}, parsed.ints);
        }

        @Test
        @DisplayName("String[] serialization and deserialization")
        void stringArrayRoundTrip() {
            ArrayHolder holder = new ArrayHolder();
            holder.ints = null;
            holder.strings = new String[]{"a", "b", "c"};

            String result = json.stringify(holder);
            assertTrue(result.contains("\"strings\":[\"a\",\"b\",\"c\"]"));

            ArrayHolder parsed = json.parse("{\"strings\":[\"x\",\"y\"]}", ArrayHolder.class);
            assertArrayEquals(new String[]{"x", "y"}, parsed.strings);
        }

        @Test
        @DisplayName("Empty array")
        void emptyArray() {
            ArrayHolder parsed = json.parse("{\"ints\":[]}", ArrayHolder.class);
            assertNotNull(parsed.ints);
            assertEquals(0, parsed.ints.length);
        }
    }

    @Nested
    @DisplayName("Null handling with includeNulls")
    class NullHandling {

        @Test
        @DisplayName("includeNulls=true includes null fields (default)")
        void includeNullsTrue() {
            User user = new User();
            user.name = null;
            String result = json.stringify(user);
            assertTrue(result.contains("\"name\":null"));
        }

        @Test
        @DisplayName("includeNulls=false excludes null fields")
        void includeNullsFalse() {
            Json noNulls = Json.builder().includeNulls(false).build();
            User user = new User();
            user.name = null;
            user.age = 5;
            String result = noNulls.stringify(user);
            assertFalse(result.contains("\"name\""));
            assertTrue(result.contains("\"age\":5"));
        }
    }

    @Nested
    @DisplayName("@JsonName annotation")
    class JsonNameTest {

        @Test
        @DisplayName("@JsonName customizes JSON key on serialization")
        void jsonNameSerialization() {
            AnnotatedUser user = new AnnotatedUser();
            user.name = "Alice";
            user.id = "1";
            user.secret = "hidden";
            user.email = "a@b.com";
            user.alias = "Al";

            String result = json.stringify(user);
            assertTrue(result.contains("\"full_name\":\"Alice\""));
            assertFalse(result.contains("\"name\":"));
            assertFalse(result.contains("\"secret\":"));
        }

        @Test
        @DisplayName("@JsonName customizes JSON key on deserialization")
        void jsonNameDeserialization() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\",\"email\":\"a@b.com\"}",
                    AnnotatedUser.class);
            assertEquals("Alice", parsed.name);
            assertEquals("1", parsed.id);
        }
    }

    @Nested
    @DisplayName("@JsonAliases annotation")
    class JsonAliasesTest {

        @Test
        @DisplayName("@JsonAliases accepts alternative keys during deserialization")
        void jsonAliasesFirstAlias() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\",\"fullName\":\"Al\"}",
                    AnnotatedUser.class);
            assertEquals("Al", parsed.alias);
        }

        @Test
        @DisplayName("@JsonAliases accepts second alias")
        void jsonAliasesSecondAlias() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\",\"displayName\":\"Captain\"}",
                    AnnotatedUser.class);
            assertEquals("Captain", parsed.alias);
        }

        @Test
        @DisplayName("Primary name takes precedence over aliases")
        void primaryNamePrecedence() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\",\"alias\":\"primary\",\"fullName\":\"secondary\"}",
                    AnnotatedUser.class);
            assertEquals("primary", parsed.alias);
        }
    }

    @Nested
    @DisplayName("@JsonIgnore annotation")
    class JsonIgnoreTest {

        @Test
        @DisplayName("@JsonIgnore excludes field from serialization")
        void jsonIgnoreSerialization() {
            AnnotatedUser user = new AnnotatedUser();
            user.name = "Alice";
            user.id = "1";
            user.secret = "hidden";
            String result = json.stringify(user);
            assertFalse(result.contains("secret"));
        }

        @Test
        @DisplayName("@JsonIgnore excludes field from deserialization")
        void jsonIgnoreDeserialization() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\",\"secret\":\"hidden\"}",
                    AnnotatedUser.class);
            assertNull(parsed.secret);
        }
    }

    @Nested
    @DisplayName("@JsonRequired annotation")
    class JsonRequiredTest {

        @Test
        @DisplayName("@JsonRequired is not enforced by default")
        void jsonRequiredMissingAllowedByDefault() {
            String missing = "{\"full_name\":\"Alice\",\"email\":\"a@b.com\"}";
            AnnotatedUser parsed = json.parse(missing, AnnotatedUser.class);
            assertEquals("Alice", parsed.name);
            assertNull(parsed.id);
        }

        @Test
        @DisplayName("@JsonRequired throws when strict required-field enforcement is enabled")
        void jsonRequiredMissingThrowsWhenConfigured() {
            Json strict = Json.builder().failOnMissingRequiredFields(true).build();
            String missing = "{\"full_name\":\"Alice\",\"email\":\"a@b.com\"}";
            JsonMappingException ex = assertThrows(JsonMappingException.class,
                    () -> strict.parse(missing, AnnotatedUser.class));
            assertEquals("id", ex.fieldName());
        }

        @Test
        @DisplayName("@JsonRequired passes when field is present")
        void jsonRequiredPresent() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\"}",
                    AnnotatedUser.class);
            assertEquals("1", parsed.id);
        }

        @Test
        @DisplayName("@JsonRequired passes when strict enforcement is enabled and field is present")
        void jsonRequiredPresentWithStrictEnforcement() {
            Json strict = Json.builder().failOnMissingRequiredFields(true).build();
            AnnotatedUser parsed = strict.parse(
                    "{\"full_name\":\"Alice\",\"id\":\"1\"}",
                    AnnotatedUser.class);
            assertEquals("1", parsed.id);
        }

        @Test
        @DisplayName("@JsonRequired passes when field value is null")
        void jsonRequiredNullValue() {
            AnnotatedUser parsed = json.parse(
                    "{\"full_name\":\"Alice\",\"id\":null}",
                    AnnotatedUser.class);
            assertNull(parsed.id);
        }
    }

    @Nested
    @DisplayName("@JsonIncludeNull and @JsonExcludeNull")
    class NullOverrideAnnotations {

        @Test
        @DisplayName("@JsonIncludeNull overrides global includeNulls=false")
        void jsonIncludeNullOverride() {
            Json noNulls = Json.builder().includeNulls(false).build();
            IncludeNullOverride holder = new IncludeNullOverride();
            holder.alwaysIncluded = null;
            holder.normalField = null;
            String result = noNulls.stringify(holder);
            assertTrue(result.contains("\"alwaysIncluded\":null"));
            assertFalse(result.contains("\"normalField\""));
        }

        @Test
        @DisplayName("@JsonExcludeNull overrides global includeNulls=true")
        void jsonExcludeNullOverride() {
            ExcludeNullOverride holder = new ExcludeNullOverride();
            holder.alwaysExcluded = null;
            holder.normalField = null;
            String result = json.stringify(holder);
            assertFalse(result.contains("\"alwaysExcluded\""));
            assertTrue(result.contains("\"normalField\":null"));
        }
    }

    @Nested
    @DisplayName("Annotation metadata")
    class AnnotationMetadata {

        @Test
        @DisplayName("Annotations target fields and have runtime retention")
        void annotationsTargetFieldsAndHaveRuntimeRetention() {
            Class<?>[] annotations = {
                    JsonName.class,
                    JsonAliases.class,
                    JsonIgnore.class,
                    JsonRequired.class,
                    JsonIncludeNull.class,
                    JsonExcludeNull.class,
                    JsonDateFormat.class
            };

            for (Class<?> annotation : annotations) {
                Target target = annotation.getAnnotation(Target.class);
                Retention retention = annotation.getAnnotation(Retention.class);

                assertNotNull(target, annotation.getName());
                assertArrayEquals(new ElementType[] {ElementType.FIELD}, target.value(), annotation.getName());
                assertNotNull(retention, annotation.getName());
                assertEquals(RetentionPolicy.RUNTIME, retention.value(), annotation.getName());
            }
        }
    }

    @Nested
    @DisplayName("Unknown properties")
    class UnknownProperties {

        @Test
        @DisplayName("Unknown properties are ignored by default")
        void unknownPropertiesIgnored() {
            User parsed = json.parse("{\"name\":\"Alice\",\"age\":30,\"active\":true,\"unknown\":42}",
                    User.class);
            assertEquals("Alice", parsed.name);
        }

        @Test
        @DisplayName("Unknown nested values are skipped by default")
        void unknownNestedValuesSkipped() {
            String input = """
                    {
                      "name": "Alice",
                      "ignored": {"deep": [{"x": 1}, {"x": [true, false, null]}]},
                      "age": 30,
                      "active": true
                    }
                    """;
            User parsed = json.parse(input, User.class);
            assertEquals("Alice", parsed.name);
            assertEquals(30, parsed.age);
            assertTrue(parsed.active);
        }

        @Test
        @DisplayName("Unknown properties fail when failOnUnknownProperties=true")
        void unknownPropertiesFail() {
            Json strict = Json.builder().failOnUnknownProperties(true).build();
            assertThrows(JsonMappingException.class,
                    () -> strict.parse("{\"name\":\"Alice\",\"age\":30,\"active\":true,\"extra\":42}", User.class));
        }
    }

    @Nested
    @DisplayName("Null for primitives")
    class NullForPrimitives {

        @Test
        @DisplayName("Null for primitives keeps default when failOnNullForPrimitives=false")
        void nullPrimitiveKeepsDefault() {
            String input = "{\"count\":null,\"value\":null,\"rate\":null,\"flag\":null}";
            PrimitiveHolder parsed = json.parse(input, PrimitiveHolder.class);
            assertEquals(0, parsed.count);
            assertEquals(0L, parsed.value);
            assertEquals(0.0, parsed.rate);
            assertFalse(parsed.flag);
        }

        @Test
        @DisplayName("Null for primitives fails when failOnNullForPrimitives=true")
        void nullPrimitiveFails() {
            Json strict = Json.builder().failOnNullForPrimitives(true).build();
            assertThrows(JsonMappingException.class,
                    () -> strict.parse("{\"count\":null,\"value\":0,\"rate\":0.0,\"flag\":false}",
                            PrimitiveHolder.class));
        }
    }

    @Nested
    @DisplayName("Duplicate keys")
    class DuplicateKeys {

        @Test
        @DisplayName("Duplicate keys last wins by default")
        void duplicateKeysLastWins() {
            User parsed = json.parse("{\"name\":\"Alice\",\"name\":\"Bob\",\"age\":30,\"active\":true}",
                    User.class);
            assertEquals("Bob", parsed.name);
        }

        @Test
        @DisplayName("Duplicate keys fail when failOnDuplicateKeys=true")
        void duplicateKeysFail() {
            Json strict = Json.builder().failOnDuplicateKeys(true).build();
            assertThrows(JsonParseException.class,
                    () -> strict.parse("{\"name\":\"Alice\",\"name\":\"Bob\",\"age\":30,\"active\":true}", User.class));
        }

        @Test
        @DisplayName("Duplicate keys fail in parseMap when configured")
        void duplicateKeysFailInParseMap() {
            Json strict = Json.builder().failOnDuplicateKeys(true).build();
            assertThrows(JsonParseException.class,
                    () -> strict.parseMap("{\"a\":1,\"a\":2}"));
        }

        @Test
        @DisplayName("Duplicate keys fail inside skipped unknown values when configured")
        void duplicateKeysFailInsideSkippedUnknownValues() {
            Json strict = Json.builder().failOnDuplicateKeys(true).build();
            assertThrows(JsonParseException.class,
                    () -> strict.parse("{\"name\":\"Alice\",\"ignored\":{\"x\":1,\"x\":2}}", User.class));
        }
    }

    @Nested
    @DisplayName("Cycle detection")
    class CycleDetection {

        @Test
        @DisplayName("Cycle detection throws when failOnCycles=true (default)")
        void cycleDetectionThrows() {
            Node node = new Node();
            node.value = "A";
            node.next = node;
            assertThrows(JsonException.class, () -> json.stringify(node));
        }

        @Test
        @DisplayName("Cycle detection disabled causes StackOverflowError or JsonException")
        void cycleDetectionDisabled() {
            Json noCycle = Json.builder().failOnCycles(false).build();
            Node node = new Node();
            node.value = "A";
            node.next = node;
            try {
                noCycle.stringify(node);
                fail("Expected StackOverflowError or JsonException");
            } catch (StackOverflowError | JsonException e) {
                // expected
            }
        }

        @Test
        @DisplayName("No cycle does not throw")
        void noCycleDoesNotThrow() {
            Node a = new Node();
            a.value = "A";
            Node b = new Node();
            b.value = "B";
            a.next = b;

            String result = json.stringify(a);
            assertTrue(result.contains("\"value\":\"A\""));
            assertTrue(result.contains("\"value\":\"B\""));
        }
    }

    @Nested
    @DisplayName("Max depth")
    class MaxDepth {

        @Test
        @DisplayName("Max depth exceeded during serialization throws JsonException")
        void maxDepthExceededSerialization() {
            Json shallow = Json.builder().maxDepth(1).build();
            UserWithAddress outer = new UserWithAddress();
            outer.name = "A";
            outer.address = new Address();
            outer.address.city = "NYC";
            outer.address.country = "US";
            assertThrows(JsonException.class, () -> shallow.stringify(outer));
        }

        @Test
        @DisplayName("Max depth exceeded during parsing throws")
        void maxDepthExceededParsing() {
            Json shallow = Json.builder().maxDepth(2).build();
            assertThrows(JsonParseException.class,
                    () -> shallow.parse("{\"name\":\"A\",\"address\":{\"city\":{\"x\":\"y\"}}}", UserWithAddress.class));
        }

        @Test
        @DisplayName("Root object at maxDepth=1 succeeds")
        void rootObjectAtMaxDepthOneSucceeds() {
            Json shallow = Json.builder().maxDepth(1).build();
            User parsed = shallow.parse("{\"name\":\"Alice\"}", User.class);
            assertEquals("Alice", parsed.name);
        }

        @Test
        @DisplayName("Within max depth succeeds")
        void withinMaxDepthSucceeds() {
            Json shallow = Json.builder().maxDepth(3).build();
            UserWithAddress outer = new UserWithAddress();
            outer.name = "A";
            outer.address = new Address();
            outer.address.city = "NYC";
            outer.address.country = "US";
            assertDoesNotThrow(() -> shallow.stringify(outer));
        }
    }

    @Nested
    @DisplayName("Pretty print")
    class PrettyPrint {

        @Test
        @DisplayName("Pretty print formats with indentation")
        void prettyPrintFormatting() {
            Json pretty = Json.builder().prettyPrint(true).build();
            User user = new User();
            user.name = "Alice";
            user.age = 30;
            user.active = true;

            String result = pretty.stringify(user);
            assertTrue(result.contains("\n"));
            assertTrue(result.contains("  \"name\": \"Alice\""));
            assertTrue(result.contains("  \"age\": 30"));
            assertTrue(result.contains("  \"active\": true"));
        }

        @Test
        @DisplayName("Pretty print nested objects")
        void prettyPrintNested() {
            Json pretty = Json.builder().prettyPrint(true).build();
            UserWithAddress outer = new UserWithAddress();
            outer.name = "Bob";
            outer.address = new Address();
            outer.address.city = "NYC";
            outer.address.country = "US";

            String result = pretty.stringify(outer);
            assertTrue(result.contains("    \"city\": \"NYC\""));
        }
    }

    @Nested
    @DisplayName("Private fields, superclass, private constructor")
    class ReflectionFeatures {

        @Test
        @DisplayName("Private fields are serialized and deserialized")
        void privateFields() {
            Child child = new Child();
            child.parentField = "from parent";
            child.childField = "from child";

            String result = json.stringify(child);
            assertTrue(result.contains("\"parentField\":\"from parent\""));
            assertTrue(result.contains("\"childField\":\"from child\""));

            Child parsed = json.parse(result, Child.class);
            assertEquals("from parent", parsed.parentField);
            assertEquals("from child", parsed.childField);
        }

        @Test
        @DisplayName("Superclass fields are included")
        void superclassFields() {
            Child child = new Child();
            child.parentField = "inherited";
            child.childField = "own";

            String result = json.stringify(child);
            assertTrue(result.contains("\"parentField\":\"inherited\""));
        }

        @Test
        @DisplayName("Private no-arg constructor is supported")
        void privateConstructor() {
            assertDoesNotThrow(() -> json.parse("{\"parentField\":\"x\",\"childField\":\"y\"}", Child.class));
        }
    }

    @Nested
    @DisplayName("Static and transient fields")
    class StaticTransientFields {

        @Test
        @DisplayName("Static fields are ignored")
        void staticFieldsIgnored() {
            StaticTransientHolder holder = new StaticTransientHolder();
            holder.name = "Alice";
            holder.temp = "temporary";
            String result = json.stringify(holder);
            assertFalse(result.contains("className"));
            assertTrue(result.contains("\"name\":\"Alice\""));
        }

        @Test
        @DisplayName("Transient fields are ignored")
        void transientFieldsIgnored() {
            StaticTransientHolder holder = new StaticTransientHolder();
            holder.temp = "temporary";
            holder.name = "Alice";
            String result = json.stringify(holder);
            assertFalse(result.contains("temp"));
        }
    }

    @Nested
    @DisplayName("Date/time types")
    class DateTimeTypes {

        @Test
        @DisplayName("LocalDate round-trip with ISO format")
        void localDateRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.localDate = LocalDate.of(2024, 1, 15);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"localDate\":\"2024-01-15\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(LocalDate.of(2024, 1, 15), parsed.localDate);
        }

        @Test
        @DisplayName("LocalTime round-trip with ISO format")
        void localTimeRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.localTime = LocalTime.of(10, 30, 0);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"localTime\":\"10:30:00\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(LocalTime.of(10, 30, 0), parsed.localTime);
        }

        @Test
        @DisplayName("LocalDateTime round-trip with ISO format")
        void localDateTimeRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"localDateTime\":\"2024-01-15T10:30:00\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), parsed.localDateTime);
        }

        @Test
        @DisplayName("OffsetDateTime round-trip with ISO format")
        void offsetDateTimeRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.offsetDateTime = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"offsetDateTime\":\"2024-01-15T10:30:00Z\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC), parsed.offsetDateTime);
        }

        @Test
        @DisplayName("ZonedDateTime round-trip with ISO format")
        void zonedDateTimeRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.zonedDateTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
            String result = json.stringify(holder);
            assertTrue(result.contains("\"zonedDateTime\":\"2024-01-15T10:30:00Z[UTC]\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("UTC")), parsed.zonedDateTime);
        }

        @Test
        @DisplayName("Instant round-trip with ISO format")
        void instantRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.instant = Instant.parse("2024-01-15T10:30:00Z");
            String result = json.stringify(holder);
            assertTrue(result.contains("\"instant\":\"2024-01-15T10:30:00Z\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(Instant.parse("2024-01-15T10:30:00Z"), parsed.instant);
        }

        @Test
        @DisplayName("Duration round-trip")
        void durationRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.duration = Duration.ofHours(2).plusMinutes(30);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"duration\":\"PT2H30M\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(Duration.ofHours(2).plusMinutes(30), parsed.duration);
        }

        @Test
        @DisplayName("Period round-trip")
        void periodRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.period = Period.of(1, 2, 3);
            String result = json.stringify(holder);
            assertTrue(result.contains("\"period\":\"P1Y2M3D\""));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertEquals(Period.of(1, 2, 3), parsed.period);
        }

        @Test
        @DisplayName("java.util.Date round-trip")
        void legacyDateRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.legacyDate = Date.from(Instant.parse("2024-01-15T10:30:00Z"));
            String result = json.stringify(holder);
            assertTrue(result.contains("\"legacyDate\":"));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertNotNull(parsed.legacyDate);
            assertEquals(holder.legacyDate.getTime(), parsed.legacyDate.getTime());
        }

        @Test
        @DisplayName("Calendar round-trip")
        void calendarRoundTrip() {
            DateHolder holder = new DateHolder();
            GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli());
            holder.calendar = cal;
            String result = json.stringify(holder);
            assertTrue(result.contains("\"calendar\":"));

            DateHolder parsed = json.parse(result, DateHolder.class);
            assertNotNull(parsed.calendar);
            assertEquals(cal.getTimeInMillis(), parsed.calendar.getTimeInMillis());
        }
    }

    @Nested
    @DisplayName("@JsonDateFormat custom pattern")
    class JsonDateFormatTest {

        @Test
        @DisplayName("Custom date format pattern round-trip")
        void customDatePatternRoundTrip() {
            CustomDateHolder holder = new CustomDateHolder();
            holder.date = LocalDate.of(2024, 1, 15);
            String result = json.stringify(holder);
            assertEquals("{\"date\":\"15/01/2024\"}", result);

            CustomDateHolder parsed = json.parse(result, CustomDateHolder.class);
            assertEquals(LocalDate.of(2024, 1, 15), parsed.date);
        }
    }

    @Nested
    @DisplayName("DateFormat epoch modes")
    class EpochModes {

        @Test
        @DisplayName("DateFormat.EPOCH_MILLIS for Instant")
        void epochMillisInstant() {
            Json epochJson = Json.builder().dateFormat(DateFormat.EPOCH_MILLIS).build();
            Instant instant = Instant.parse("2024-01-15T10:30:00Z");
            long expected = instant.toEpochMilli();

            EpochHolder holder = new EpochHolder();
            holder.instant = instant;
            String result = epochJson.stringify(holder);
            assertTrue(result.contains("\"instant\":" + expected));

            EpochHolder parsed = epochJson.parse(result, EpochHolder.class);
            assertEquals(instant, parsed.instant);
        }

        @Test
        @DisplayName("DateFormat.EPOCH_SECONDS for Instant")
        void epochSecondsInstant() {
            Json epochJson = Json.builder().dateFormat(DateFormat.EPOCH_SECONDS).build();
            Instant instant = Instant.parse("2024-01-15T10:30:00Z");
            long expected = instant.getEpochSecond();

            EpochHolder holder = new EpochHolder();
            holder.instant = instant;
            String result = epochJson.stringify(holder);
            assertTrue(result.contains("\"instant\":" + expected));

            EpochHolder parsed = epochJson.parse(result, EpochHolder.class);
            assertEquals(instant, parsed.instant);
        }

        @Test
        @DisplayName("DateFormat.EPOCH_MILLIS keeps LocalDate as ISO")
        void epochMillisKeepsLocalDateAsIso() {
            Json epochJson = Json.builder().dateFormat(DateFormat.EPOCH_MILLIS).build();
            DateHolder holder = new DateHolder();
            holder.localDate = LocalDate.of(2024, 1, 15);

            String result = epochJson.stringify(holder);
            assertTrue(result.contains("\"localDate\":\"2024-01-15\""));

            DateHolder parsed = epochJson.parse(result, DateHolder.class);
            assertEquals(LocalDate.of(2024, 1, 15), parsed.localDate);
        }

        @Test
        @DisplayName("DateFormat.EPOCH_MILLIS rejects numeric LocalDate input")
        void epochMillisRejectsNumericLocalDateInput() {
            Json epochJson = Json.builder().dateFormat(DateFormat.EPOCH_MILLIS).build();
            JsonMappingException ex = assertThrows(JsonMappingException.class,
                    () -> epochJson.parse("{\"localDate\":1700000000000}", DateHolder.class));
            assertEquals("$.localDate", ex.jsonPath());
            assertEquals(LocalDate.class, ex.targetType());
        }
    }

    @Nested
    @DisplayName("Field naming strategies")
    class FieldNamingStrategies {

        @Test
        @DisplayName("IDENTITY uses Java field name as-is")
        void identityNaming() {
            Json idJson = Json.builder().fieldNaming(FieldNaming.IDENTITY).build();
            NamingLowerUser holder = new NamingLowerUser();
            holder.userName = "Alice";
            String result = idJson.stringify(holder);
            assertTrue(result.contains("\"userName\":\"Alice\""));
        }

        @Test
        @DisplayName("SNAKE_CASE converts camelCase to snake_case")
        void snakeCaseNaming() {
            Json snakeJson = Json.builder().fieldNaming(FieldNaming.SNAKE_CASE).build();
            NamingSnakeUser holder = new NamingSnakeUser();
            holder.userName = "Alice";
            String result = snakeJson.stringify(holder);
            assertTrue(result.contains("\"user_name\":\"Alice\""));
            assertFalse(result.contains("\"userName\""));
        }

        @Test
        @DisplayName("KEBAB_CASE converts camelCase to kebab-case")
        void kebabCaseNaming() {
            Json kebabJson = Json.builder().fieldNaming(FieldNaming.KEBAB_CASE).build();
            NamingKebabUser holder = new NamingKebabUser();
            holder.userName = "Alice";
            String result = kebabJson.stringify(holder);
            assertTrue(result.contains("\"user-name\":\"Alice\""));
            assertFalse(result.contains("\"userName\""));
        }

        @Test
        @DisplayName("LOWER_CASE lowercases the field name")
        void lowerCaseNaming() {
            Json lowerJson = Json.builder().fieldNaming(FieldNaming.LOWER_CASE).build();
            NamingLowerUser holder = new NamingLowerUser();
            holder.userName = "Alice";
            String result = lowerJson.stringify(holder);
            assertTrue(result.contains("\"username\":\"Alice\""));
            assertFalse(result.contains("\"userName\""));
        }

        @Test
        @DisplayName("UPPER_CASE uppercases the field name")
        void upperCaseNaming() {
            Json upperJson = Json.builder().fieldNaming(FieldNaming.UPPER_CASE).build();
            NamingUpperUser holder = new NamingUpperUser();
            holder.userName = "Alice";
            String result = upperJson.stringify(holder);
            assertTrue(result.contains("\"USERNAME\":\"Alice\""));
            assertFalse(result.contains("\"userName\""));
        }

        @Test
        @DisplayName("CAMEL_CASE converts snake_case to camelCase")
        void camelCaseNaming() {
            Json camelJson = Json.builder().fieldNaming(FieldNaming.CAMEL_CASE).build();
            NamingCamelUser holder = new NamingCamelUser();
            holder.user_name = "Alice";
            String result = camelJson.stringify(holder);
            assertTrue(result.contains("\"userName\":\"Alice\""));
            assertFalse(result.contains("\"user_name\""));
        }

        @Test
        @DisplayName("Naming strategy round-trip SNAKE_CASE")
        void snakeCaseRoundTrip() {
            Json snakeJson = Json.builder().fieldNaming(FieldNaming.SNAKE_CASE).build();
            String input = "{\"user_name\":\"Bob\"}";
            NamingSnakeUser parsed = snakeJson.parse(input, NamingSnakeUser.class);
            assertEquals("Bob", parsed.userName);
        }
    }

    @Nested
    @DisplayName("JsonException hierarchy")
    class ExceptionHierarchy {

        @Test
        @DisplayName("JsonParseException extends JsonException")
        void parseExceptionHierarchy() {
            JsonParseException ex = new JsonParseException("test", 1, 1, 0);
            assertInstanceOf(JsonException.class, ex);
        }

        @Test
        @DisplayName("JsonMappingException extends JsonException")
        void mappingExceptionHierarchy() {
            JsonMappingException ex = new JsonMappingException("test", "$", null, null, null, null);
            assertInstanceOf(JsonException.class, ex);
        }

        @Test
        @DisplayName("JsonMappingException provides context")
        void mappingExceptionContext() {
            Json strict = Json.builder().failOnMissingRequiredFields(true).build();
            JsonMappingException ex = assertThrows(JsonMappingException.class,
                    () -> strict.parse("{\"name\":\"Alice\"}", AnnotatedUser.class));
            assertNotNull(ex.jsonPath());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty object")
        void emptyObject() {
            User parsed = json.parse("{}", User.class);
            assertNotNull(parsed);
            assertNull(parsed.name);
            assertEquals(0, parsed.age);
            assertFalse(parsed.active);
        }

        @Test
        @DisplayName("Empty array")
        void emptyArray() {
            List<String> result = json.parseList("[]", String.class);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Empty string value")
        void emptyStringValue() {
            assertEquals("\"\"", json.stringify(""));
            String parsed = json.parse("\"\"", String.class);
            assertEquals("", parsed);
        }

        @Test
        @DisplayName("Zero values")
        void zeroValues() {
            assertEquals("0", json.stringify(0));
            assertEquals("0", json.stringify(0L));
            assertEquals("0.0", json.stringify(0.0));
        }

        @Test
        @DisplayName("Negative number")
        void negativeNumber() {
            assertEquals("-42", json.stringify(-42));
        }

        @Test
        @DisplayName("Large integer")
        void largeInteger() {
            String result = json.stringify(Long.MAX_VALUE);
            long parsed = json.parse(result, Long.class);
            assertEquals(Long.MAX_VALUE, parsed);
        }

        @Test
        @DisplayName("Very long string")
        void longString() {
            String longStr = "A".repeat(10000);
            String result = json.stringify(longStr);
            String parsed = json.parse(result, String.class);
            assertEquals(longStr, parsed);
        }

        @Test
        @DisplayName("Nested lists")
        void nestedList() {
            String jsonStr = "[[1,2],[3,4]]";
            List<List<Long>> result = (List<List<Long>>) (List<?>) json.parseList(jsonStr, List.class);
            assertEquals(2, result.size());
            assertEquals(List.of(1L, 2L), result.get(0));
            assertEquals(List.of(3L, 4L), result.get(1));
        }

        @Test
        @DisplayName("parseMap with nested objects returns Map values")
        void parseMapNestedObjects() {
            Map<String, Object> result = json.parseMap("{\"outer\":{\"inner\":\"value\"}}");
            assertInstanceOf(Map.class, result.get("outer"));
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) result.get("outer");
            assertEquals("value", inner.get("inner"));
        }

        @Test
        @DisplayName("NaN throws JsonException")
        void nanThrows() {
            assertThrows(JsonException.class, () -> json.stringify(Double.NaN));
        }

        @Test
        @DisplayName("Infinity throws JsonException")
        void infinityThrows() {
            assertThrows(JsonException.class, () -> json.stringify(Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("Multiple fields round-trip correctly")
        void multipleFieldsRoundTrip() {
            DateHolder holder = new DateHolder();
            holder.localDate = LocalDate.of(2024, 6, 15);
            holder.localDateTime = LocalDateTime.of(2024, 6, 15, 12, 0);
            holder.instant = Instant.parse("2024-06-15T12:00:00Z");
            holder.duration = Duration.ofMinutes(90);
            holder.period = Period.of(0, 6, 0);

            String result = json.stringify(holder);
            DateHolder parsed = json.parse(result, DateHolder.class);

            assertEquals(holder.localDate, parsed.localDate);
            assertEquals(holder.localDateTime, parsed.localDateTime);
            assertEquals(holder.instant, parsed.instant);
            assertEquals(holder.duration, parsed.duration);
            assertEquals(holder.period, parsed.period);
        }
    }

    @Nested
    @DisplayName("JsonBuilder chaining")
    class BuilderChaining {

        @Test
        @DisplayName("Builder methods can be chained")
        void builderChaining() {
            Json custom = Json.builder()
                    .fieldNaming(FieldNaming.SNAKE_CASE)
                    .includeNulls(false)
                    .failOnUnknownProperties(true)
                    .failOnMissingRequiredFields(true)
                    .failOnNullForPrimitives(true)
                    .failOnDuplicateKeys(true)
                    .failOnCycles(false)
                    .maxDepth(64)
                    .prettyPrint(true)
                    .dateFormat(DateFormat.ISO)
                    .zoneId(ZoneId.of("America/New_York"))
                    .enumMode(EnumMode.TO_STRING)
                    .build();

            JsonConfig cfg = custom.config();
            assertEquals(FieldNaming.SNAKE_CASE, cfg.fieldNaming());
            assertFalse(cfg.includeNulls());
            assertTrue(cfg.failOnUnknownProperties());
            assertTrue(cfg.failOnNullForPrimitives());
            assertTrue(cfg.failOnDuplicateKeys());
            assertFalse(cfg.failOnCycles());
            assertEquals(64, cfg.maxDepth());
            assertTrue(cfg.prettyPrint());
            assertEquals(DateFormat.ISO, cfg.dateFormat());
            assertEquals(ZoneId.of("America/New_York"), cfg.zoneId());
            assertEquals(EnumMode.TO_STRING, cfg.enumMode());
        }

        @Test
        @DisplayName("maxDepth must be positive")
        void maxDepthMustBePositive() {
            assertThrows(IllegalArgumentException.class, () -> Json.builder().maxDepth(0));
        }

        @Test
        @DisplayName("Builder rejects null enum and zone values")
        void builderRejectsNullValues() {
            assertThrows(NullPointerException.class, () -> Json.builder().fieldNaming(null));
            assertThrows(NullPointerException.class, () -> Json.builder().dateFormat(null));
            assertThrows(NullPointerException.class, () -> Json.builder().zoneId(null));
            assertThrows(NullPointerException.class, () -> Json.builder().enumMode(null));
        }
    }

    @Nested
    @DisplayName("Performance-related tests")
    class PerformanceTests {

        @Test
        @DisplayName("Repeated stringify/parse calls are consistent")
        void repeatedStringifyParse() {
            User user = new User();
            user.name = "Alice";
            user.age = 30;
            user.active = true;

            for (int i = 0; i < 100; i++) {
                String s = json.stringify(user);
                User parsed = json.parse(s, User.class);
                assertEquals("Alice", parsed.name);
                assertEquals(30, parsed.age);
                assertEquals(true, parsed.active);
            }
        }

        @Test
        @DisplayName("String escaping handles all control characters")
        void stringEscapingControlCharacters() {
            String input = "\"\\\n\r\t\b\f\u0001\u001f";
            String escaped = json.stringify(input);
            String parsed = json.parse(escaped, String.class);
            assertEquals(input, parsed);
        }

        @Test
        @DisplayName("String escaping handles Unicode correctly")
        void stringEscapingUnicode() {
            String input = "hello\u00e9\u00e8\u00ea world";
            String escaped = json.stringify(input);
            String parsed = json.parse(escaped, String.class);
            assertEquals(input, parsed);
        }

        @Test
        @DisplayName("Date format caching via repeated custom pattern")
        void dateFormatCachingCustomPattern() {
            Json localJson = Json.builder().build();
            for (int i = 0; i < 50; i++) {
                String result = localJson.stringify(java.time.LocalDate.of(2024, 1, 15));
                assertTrue(result.contains("2024"));
            }
        }

        @Test
        @DisplayName("Large list round-trip")
        void largeListRoundTrip() {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                list.add(i);
            }
            String jsonStr = json.stringify(list);
            List<Integer> parsed = json.parseList(jsonStr, Integer.class);
            assertEquals(1000, parsed.size());
            assertEquals(0, parsed.get(0));
            assertEquals(999, parsed.get(999));
        }

        @Test
        @DisplayName("Cycle detection with repeated calls")
        void cycleDetectionRepeatedCalls() {
            Node root = new Node();
            root.value = "root";
            Node child = new Node();
            child.value = "child";
            child.next = root;
            root.next = child;
            for (int i = 0; i < 10; i++) {
                assertThrows(JsonException.class, () -> json.stringify(root));
            }
        }

        @Test
        @DisplayName("Max depth with repeated calls")
        void maxDepthRepeatedCalls() {
            Json depthLimited = Json.builder().maxDepth(2).build();
            Node a = new Node();
            a.value = "a";
            Node b = new Node();
            b.value = "b";
            Node c = new Node();
            c.value = "c";
            a.next = b;
            b.next = c;
            for (int i = 0; i < 10; i++) {
                assertThrows(JsonException.class, () -> depthLimited.stringify(a));
            }
        }

    }
}
