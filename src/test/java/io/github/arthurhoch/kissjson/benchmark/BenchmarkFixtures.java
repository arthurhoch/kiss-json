package io.github.arthurhoch.kissjson.benchmark;

import io.github.arthurhoch.kissjson.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BenchmarkFixtures {

    private BenchmarkFixtures() {
    }

    static Data create(Json json) {
        KissJsonBenchmark.SimplePojo simplePojo = new KissJsonBenchmark.SimplePojo();
        simplePojo.name = "Alice";
        simplePojo.age = 30;
        simplePojo.active = true;
        simplePojo.score = 95.5;

        KissJsonBenchmark.NestedPojo inner = new KissJsonBenchmark.NestedPojo();
        inner.name = "Inner";
        inner.value = 42;

        KissJsonBenchmark.NestedPojo nestedPojo = new KissJsonBenchmark.NestedPojo();
        nestedPojo.name = "Outer";
        nestedPojo.value = 99;
        nestedPojo.nested = inner;

        KissJsonBenchmark.DatePojo datePojo = new KissJsonBenchmark.DatePojo();
        datePojo.localDate = java.time.LocalDate.of(2024, 6, 15);
        datePojo.instant = java.time.Instant.parse("2024-06-15T10:30:00Z");

        List<KissJsonBenchmark.SimplePojo> pojoList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            KissJsonBenchmark.SimplePojo p = new KissJsonBenchmark.SimplePojo();
            p.name = "User" + i;
            p.age = 20 + (i % 50);
            p.active = i % 2 == 0;
            p.score = 50.0 + i;
            pojoList.add(p);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Alice");
        map.put("age", 30);
        map.put("active", true);
        map.put("tags", List.of("dev", "java"));

        StringBuilder sb = new StringBuilder(200);
        for (char c = 0; c < 128; c++) {
            sb.append(c);
        }
        sb.append("unicode: \u00e9\u00e8\u00ea \u4e2d\u6587 \ud83d\ude00");

        return new Data(
                simplePojo,
                json.stringify(simplePojo),
                nestedPojo,
                json.stringify(nestedPojo),
                datePojo,
                json.stringify(datePojo),
                pojoList,
                json.stringify(pojoList),
                map,
                json.stringify(map),
                sb.toString()
        );
    }

    static final class Data {
        final KissJsonBenchmark.SimplePojo simplePojo;
        final String simplePojoJson;
        final KissJsonBenchmark.NestedPojo nestedPojo;
        final String nestedPojoJson;
        final KissJsonBenchmark.DatePojo datePojo;
        final String datePojoJson;
        final List<KissJsonBenchmark.SimplePojo> pojoList;
        final String pojoListJson;
        final Map<String, Object> map;
        final String mapJson;
        final String escapeHeavyString;

        private Data(
                KissJsonBenchmark.SimplePojo simplePojo,
                String simplePojoJson,
                KissJsonBenchmark.NestedPojo nestedPojo,
                String nestedPojoJson,
                KissJsonBenchmark.DatePojo datePojo,
                String datePojoJson,
                List<KissJsonBenchmark.SimplePojo> pojoList,
                String pojoListJson,
                Map<String, Object> map,
                String mapJson,
                String escapeHeavyString
        ) {
            this.simplePojo = simplePojo;
            this.simplePojoJson = simplePojoJson;
            this.nestedPojo = nestedPojo;
            this.nestedPojoJson = nestedPojoJson;
            this.datePojo = datePojo;
            this.datePojoJson = datePojoJson;
            this.pojoList = pojoList;
            this.pojoListJson = pojoListJson;
            this.map = map;
            this.mapJson = mapJson;
            this.escapeHeavyString = escapeHeavyString;
        }
    }
}
