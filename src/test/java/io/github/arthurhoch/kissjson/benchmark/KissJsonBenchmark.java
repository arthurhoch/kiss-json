package io.github.arthurhoch.kissjson.benchmark;

import io.github.arthurhoch.kissjson.Json;
import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class KissJsonBenchmark {

    private Json json;

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
    public void setup() {
        json = Json.create();
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
    }

    @Benchmark
    public String serializeSimplePojo() {
        return json.stringify(simplePojo);
    }

    @Benchmark
    public SimplePojo deserializeSimplePojo() {
        return json.parse(simplePojoJson, SimplePojo.class);
    }

    @Benchmark
    public String serializeNestedPojo() {
        return json.stringify(nestedPojo);
    }

    @Benchmark
    public NestedPojo deserializeNestedPojo() {
        return json.parse(nestedPojoJson, NestedPojo.class);
    }

    @Benchmark
    public String serializeDatePojo() {
        return json.stringify(datePojo);
    }

    @Benchmark
    public DatePojo deserializeDatePojo() {
        return json.parse(datePojoJson, DatePojo.class);
    }

    @Benchmark
    public String serializePojoList100() {
        return json.stringify(pojoList);
    }

    @Benchmark
    public List<SimplePojo> deserializePojoList100() {
        return json.parseList(pojoListJson, SimplePojo.class);
    }

    @Benchmark
    public String stringifyMap() {
        return json.stringify(map);
    }

    @Benchmark
    public Map<String, Object> parseMap() {
        return json.parseMap(mapJson);
    }

    @Benchmark
    public String escapeString() {
        return json.stringify(escapeHeavyString);
    }

    public static class SimplePojo {
        String name;
        int age;
        boolean active;
        double score;
    }

    public static class NestedPojo {
        String name;
        int value;
        NestedPojo nested;
    }

    public static class DatePojo {
        java.time.LocalDate localDate;
        java.time.Instant instant;
    }

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}
