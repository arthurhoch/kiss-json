package io.github.arthurhoch.kissjson.benchmark;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.arthurhoch.kissjson.Json;
import io.github.arthurhoch.kissjson.benchmark.KissJsonBenchmark.DatePojo;
import io.github.arthurhoch.kissjson.benchmark.KissJsonBenchmark.NestedPojo;
import io.github.arthurhoch.kissjson.benchmark.KissJsonBenchmark.SimplePojo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class JsonLibraryComparisonBenchmark {

    private static final TypeReference<List<SimplePojo>> SIMPLE_POJO_LIST_TYPE =
            new TypeReference<List<SimplePojo>>() {
            };

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    private Json json;
    private ObjectMapper objectMapper;

    private SimplePojo simplePojo;
    private String simplePojoJson;

    private NestedPojo nestedPojo;
    private String nestedPojoJson;

    private DatePojo datePojo;
    private String datePojoJson;

    private List<SimplePojo> pojoList;
    private String pojoListJson;

    private Map<String, Object> map;
    private String mapJson;

    private String escapeHeavyString;

    @Setup
    public void setup() throws Exception {
        json = Json.create();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        BenchmarkFixtures.Data data = BenchmarkFixtures.create(json);
        simplePojo = data.simplePojo;
        simplePojoJson = data.simplePojoJson;
        nestedPojo = data.nestedPojo;
        nestedPojoJson = data.nestedPojoJson;
        datePojo = data.datePojo;
        datePojoJson = data.datePojoJson;
        pojoList = data.pojoList;
        pojoListJson = data.pojoListJson;
        map = data.map;
        mapJson = data.mapJson;
        escapeHeavyString = data.escapeHeavyString;

        verifyCorrectness();
    }

    @Benchmark
    public String kissJsonSerializeSimplePojo() {
        return json.stringify(simplePojo);
    }

    @Benchmark
    public String jacksonSerializeSimplePojo() throws Exception {
        return objectMapper.writeValueAsString(simplePojo);
    }

    @Benchmark
    public SimplePojo kissJsonDeserializeSimplePojo() {
        return json.parse(simplePojoJson, SimplePojo.class);
    }

    @Benchmark
    public SimplePojo jacksonDeserializeSimplePojo() throws Exception {
        return objectMapper.readValue(simplePojoJson, SimplePojo.class);
    }

    @Benchmark
    public String kissJsonSerializeNestedPojo() {
        return json.stringify(nestedPojo);
    }

    @Benchmark
    public String jacksonSerializeNestedPojo() throws Exception {
        return objectMapper.writeValueAsString(nestedPojo);
    }

    @Benchmark
    public NestedPojo kissJsonDeserializeNestedPojo() {
        return json.parse(nestedPojoJson, NestedPojo.class);
    }

    @Benchmark
    public NestedPojo jacksonDeserializeNestedPojo() throws Exception {
        return objectMapper.readValue(nestedPojoJson, NestedPojo.class);
    }

    @Benchmark
    public String kissJsonSerializeDatePojo() {
        return json.stringify(datePojo);
    }

    @Benchmark
    public String jacksonSerializeDatePojo() throws Exception {
        return objectMapper.writeValueAsString(datePojo);
    }

    @Benchmark
    public DatePojo kissJsonDeserializeDatePojo() {
        return json.parse(datePojoJson, DatePojo.class);
    }

    @Benchmark
    public DatePojo jacksonDeserializeDatePojo() throws Exception {
        return objectMapper.readValue(datePojoJson, DatePojo.class);
    }

    @Benchmark
    public String kissJsonSerializePojoList100() {
        return json.stringify(pojoList);
    }

    @Benchmark
    public String jacksonSerializePojoList100() throws Exception {
        return objectMapper.writeValueAsString(pojoList);
    }

    @Benchmark
    public List<SimplePojo> kissJsonDeserializePojoList100() {
        return json.parseList(pojoListJson, SimplePojo.class);
    }

    @Benchmark
    public List<SimplePojo> jacksonDeserializePojoList100() throws Exception {
        return objectMapper.readValue(pojoListJson, SIMPLE_POJO_LIST_TYPE);
    }

    @Benchmark
    public String kissJsonStringifyMap() {
        return json.stringify(map);
    }

    @Benchmark
    public String jacksonStringifyMap() throws Exception {
        return objectMapper.writeValueAsString(map);
    }

    @Benchmark
    public Map<String, Object> kissJsonParseMap() {
        return json.parseMap(mapJson);
    }

    @Benchmark
    public Map<String, Object> jacksonParseMap() throws Exception {
        return objectMapper.readValue(mapJson, MAP_TYPE);
    }

    @Benchmark
    public String kissJsonEscapeString() {
        return json.stringify(escapeHeavyString);
    }

    @Benchmark
    public String jacksonEscapeString() throws Exception {
        return objectMapper.writeValueAsString(escapeHeavyString);
    }

    private void verifyCorrectness() throws Exception {
        assertSimpleEquals("KissJson simple round-trip", simplePojo, json.parse(json.stringify(simplePojo), SimplePojo.class));
        assertSimpleEquals("Jackson simple round-trip", simplePojo,
                objectMapper.readValue(objectMapper.writeValueAsString(simplePojo), SimplePojo.class));
        assertSimpleEquals("Jackson reads KissJson simple JSON", simplePojo,
                objectMapper.readValue(simplePojoJson, SimplePojo.class));
        assertSimpleEquals("KissJson reads Jackson simple JSON", simplePojo,
                json.parse(objectMapper.writeValueAsString(simplePojo), SimplePojo.class));

        assertNestedEquals("KissJson nested round-trip", nestedPojo, json.parse(json.stringify(nestedPojo), NestedPojo.class));
        assertNestedEquals("Jackson nested round-trip", nestedPojo,
                objectMapper.readValue(objectMapper.writeValueAsString(nestedPojo), NestedPojo.class));
        assertNestedEquals("Jackson reads KissJson nested JSON", nestedPojo,
                objectMapper.readValue(nestedPojoJson, NestedPojo.class));
        assertNestedEquals("KissJson reads Jackson nested JSON", nestedPojo,
                json.parse(objectMapper.writeValueAsString(nestedPojo), NestedPojo.class));

        assertDateEquals("KissJson date round-trip", datePojo, json.parse(json.stringify(datePojo), DatePojo.class));
        assertDateEquals("Jackson date round-trip", datePojo,
                objectMapper.readValue(objectMapper.writeValueAsString(datePojo), DatePojo.class));
        assertDateEquals("Jackson reads KissJson date JSON", datePojo,
                objectMapper.readValue(datePojoJson, DatePojo.class));
        assertDateEquals("KissJson reads Jackson date JSON", datePojo,
                json.parse(objectMapper.writeValueAsString(datePojo), DatePojo.class));

        assertSimpleListEquals("KissJson list round-trip", pojoList,
                json.parseList(json.stringify(pojoList), SimplePojo.class));
        assertSimpleListEquals("Jackson list round-trip", pojoList,
                objectMapper.readValue(objectMapper.writeValueAsString(pojoList), SIMPLE_POJO_LIST_TYPE));
        assertSimpleListEquals("Jackson reads KissJson list JSON", pojoList,
                objectMapper.readValue(pojoListJson, SIMPLE_POJO_LIST_TYPE));
        assertSimpleListEquals("KissJson reads Jackson list JSON", pojoList,
                json.parseList(objectMapper.writeValueAsString(pojoList), SimplePojo.class));

        assertBenchmarkMap("KissJson map round-trip", json.parseMap(json.stringify(map)));
        assertBenchmarkMap("Jackson map round-trip", objectMapper.readValue(objectMapper.writeValueAsString(map), MAP_TYPE));
        assertBenchmarkMap("Jackson reads KissJson map JSON", objectMapper.readValue(mapJson, MAP_TYPE));
        assertBenchmarkMap("KissJson reads Jackson map JSON", json.parseMap(objectMapper.writeValueAsString(map)));

        String kissJsonEscaped = json.stringify(escapeHeavyString);
        String jacksonEscaped = objectMapper.writeValueAsString(escapeHeavyString);
        require(escapeHeavyString.equals(objectMapper.readValue(kissJsonEscaped, String.class)),
                "Jackson reads KissJson escaped string JSON");
        require(escapeHeavyString.equals(json.parse(jacksonEscaped, String.class)),
                "KissJson reads Jackson escaped string JSON");
    }

    private static void assertSimpleListEquals(String context, List<SimplePojo> expected, List<SimplePojo> actual) {
        require(expected.size() == actual.size(), context + ": list size");
        for (int i = 0; i < expected.size(); i++) {
            assertSimpleEquals(context + "[" + i + "]", expected.get(i), actual.get(i));
        }
    }

    private static void assertSimpleEquals(String context, SimplePojo expected, SimplePojo actual) {
        require(Objects.equals(expected.name, actual.name), context + ": name");
        require(expected.age == actual.age, context + ": age");
        require(expected.active == actual.active, context + ": active");
        require(Double.compare(expected.score, actual.score) == 0, context + ": score");
    }

    private static void assertNestedEquals(String context, NestedPojo expected, NestedPojo actual) {
        if (expected == null || actual == null) {
            require(expected == actual, context + ": null nested value");
            return;
        }
        require(Objects.equals(expected.name, actual.name), context + ": name");
        require(expected.value == actual.value, context + ": value");
        assertNestedEquals(context + ".nested", expected.nested, actual.nested);
    }

    private static void assertDateEquals(String context, DatePojo expected, DatePojo actual) {
        require(Objects.equals(expected.localDate, actual.localDate), context + ": localDate");
        require(Objects.equals(expected.instant, actual.instant), context + ": instant");
    }

    private static void assertBenchmarkMap(String context, Map<String, Object> actual) {
        require("Alice".equals(actual.get("name")), context + ": name");
        require(actual.get("age") instanceof Number age && age.longValue() == 30L, context + ": age");
        require(Boolean.TRUE.equals(actual.get("active")), context + ": active");
        require(actual.get("tags") instanceof List<?> tags
                        && tags.size() == 2
                        && "dev".equals(tags.get(0))
                        && "java".equals(tags.get(1)),
                context + ": tags");
    }

    private static void require(boolean condition, String context) {
        if (!condition) {
            throw new IllegalStateException("Benchmark correctness check failed: " + context);
        }
    }
}
