package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.DateFormat;
import io.github.arthurhoch.kissjson.EnumMode;
import io.github.arthurhoch.kissjson.JsonConfig;
import io.github.arthurhoch.kissjson.JsonException;
import io.github.arthurhoch.kissjson.JsonMappingException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ObjectWriter {

    private static final String[] INDENT_CACHE;

    static {
        INDENT_CACHE = new String[33];
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < INDENT_CACHE.length; i++) {
            INDENT_CACHE[i] = sb.toString();
            sb.append("  ");
        }
    }

    private final StringBuilder out;
    private final JsonConfig config;
    private final IdentityHashMap<Object, Object> visited;
    private final boolean checkCycles;
    private final boolean prettyPrint;
    private final boolean includeNulls;
    private final int maxDepth;
    private final EnumMode enumMode;
    private int indent;

    private ObjectWriter(JsonConfig config, int initialCapacity) {
        this.out = new StringBuilder(initialCapacity);
        this.config = config;
        this.visited = new IdentityHashMap<>();
        this.checkCycles = config.failOnCycles();
        this.prettyPrint = config.prettyPrint();
        this.includeNulls = config.includeNulls();
        this.maxDepth = config.maxDepth();
        this.enumMode = config.enumMode();
        this.indent = 0;
    }

    static String write(Object value, JsonConfig config) {
        ObjectWriter writer = new ObjectWriter(config, estimateInitialCapacity(value));
        writer.writeValue(value, new JsonPath());
        return writer.out.toString();
    }

    private void writeValue(Object value, JsonPath path) {
        if (value == null) {
            out.append("null");
            return;
        }

        if (value instanceof String) {
            JsonWriter.escapeString((String) value, out);
            return;
        }
        if (value instanceof Character) {
            JsonWriter.escapeString(value.toString(), out);
            return;
        }
        if (value instanceof Boolean) {
            out.append((boolean) value ? "true" : "false");
            return;
        }
        if (value instanceof Number) {
            writeNumber((Number) value);
            return;
        }
        if (value instanceof Enum) {
            writeEnum((Enum<?>) value);
            return;
        }

        Class<?> c = value.getClass();
        if (c.isArray()) {
            writeArray(value, path);
            return;
        }
        if (DateCodec.isDateType(c)) {
            writeDate(value, null);
            return;
        }
        if (value instanceof Map) {
            writeMap((Map<?, ?>) value, path);
            return;
        }
        if (value instanceof List) {
            writeList((List<?>) value, path);
            return;
        }

        writeObject(value, path);
    }

    private void writeNumber(Number value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            out.append(value.longValue());
        } else if (value instanceof BigDecimal bd) {
            out.append(bd.toPlainString());
        } else if (value instanceof BigInteger bi) {
            out.append(bi.toString());
        } else if (value instanceof Double d) {
            double dv = d;
            if (Double.isNaN(dv) || Double.isInfinite(dv)) {
                throw new JsonException("NaN and Infinity are not valid JSON values");
            }
            out.append(dv);
        } else if (value instanceof Float f) {
            float fv = f;
            if (Float.isNaN(fv) || Float.isInfinite(fv)) {
                throw new JsonException("NaN and Infinity are not valid JSON values");
            }
            out.append(fv);
        } else {
            out.append(value.toString());
        }
    }

    private void writeEnum(Enum<?> value) {
        String str = enumMode == EnumMode.TO_STRING ? value.toString() : value.name();
        JsonWriter.escapeString(str, out);
    }

    private void writeDate(Object value, String pattern) {
        String result = DateCodec.serialize(value, config.dateFormat(), config.zoneId(), pattern);
        if (pattern == null && config.dateFormat() == DateFormat.ISO) {
            JsonWriter.escapeString(result, out);
            return;
        }
        if (isNumeric(result)) {
            out.append(result);
        } else {
            JsonWriter.escapeString(result, out);
        }
    }

    private void writeObject(Object obj, JsonPath path) {
        ClassModel model = ClassModelCache.get(obj.getClass(), config.fieldNaming());
        writeObject(obj, path, model);
    }

    private void writeObject(Object obj, JsonPath path, ClassModel model) {
        if (checkCycles) {
            if (visited.containsKey(obj)) {
                throw new JsonException("Cycle detected at " + path);
            }
            visited.put(obj, Boolean.TRUE);
        }

        FieldModel[] fields = model.fields();

        out.append('{');
        indent++;
        checkDepth(path);
        boolean first = true;

        for (FieldModel fm : fields) {
            Object fieldValue = fm.getValue(obj);

            if (fieldValue == null) {
                boolean shouldInclude = includeNulls;
                if (fm.includeNull()) shouldInclude = true;
                if (fm.excludeNull()) shouldInclude = false;
                if (!shouldInclude) continue;
            }

            if (!first) out.append(',');
            first = false;

            if (prettyPrint) {
                out.append('\n');
                appendIndent();
            }

            String name = fm.primaryName();
            out.append(fm.quotedNameColon());
            if (prettyPrint) out.append(' ');

            if (fieldValue == null) {
                out.append("null");
            } else if (fm.dateFormat() != null && fm.isDateType()) {
                writeDate(fieldValue, fm.dateFormat());
            } else {
                if (needsPath(fieldValue)) {
                    int mark = path.pushField(name);
                    try {
                        writeValue(fieldValue, path);
                    } finally {
                        path.restore(mark);
                    }
                } else {
                    writeValue(fieldValue, path);
                }
            }
        }

        indent--;
        if (prettyPrint && !first) {
            out.append('\n');
            appendIndent();
        }
        out.append('}');

        if (checkCycles) {
            visited.remove(obj);
        }
    }

    private void writeList(List<?> list, JsonPath path) {
        if (checkCycles) {
            if (visited.containsKey(list)) {
                throw new JsonException("Cycle detected at " + path);
            }
            visited.put(list, Boolean.TRUE);
        }

        out.append('[');
        indent++;
        checkDepth(path);
        boolean first = true;
        Class<?> cachedObjectClass = null;
        ClassModel cachedObjectModel = null;

        for (int i = 0, size = list.size(); i < size; i++) {
            if (!first) out.append(',');
            first = false;

            if (prettyPrint) {
                out.append('\n');
                appendIndent();
            }

            Object element = list.get(i);
            if (needsPath(element)) {
                int mark = path.pushIndex(i);
                try {
                    if (isPlainObject(element)) {
                        Class<?> elementClass = element.getClass();
                        if (elementClass != cachedObjectClass) {
                            cachedObjectClass = elementClass;
                            cachedObjectModel = ClassModelCache.get(elementClass, config.fieldNaming());
                        }
                        writeObject(element, path, cachedObjectModel);
                    } else {
                        writeValue(element, path);
                    }
                } finally {
                    path.restore(mark);
                }
            } else {
                writeValue(element, path);
            }
        }

        indent--;
        if (prettyPrint && !first) {
            out.append('\n');
            appendIndent();
        }
        out.append(']');

        if (checkCycles) {
            visited.remove(list);
        }
    }

    private void writeMap(Map<?, ?> map, JsonPath path) {
        if (checkCycles) {
            if (visited.containsKey(map)) {
                throw new JsonException("Cycle detected at " + path);
            }
            visited.put(map, Boolean.TRUE);
        }

        out.append('{');
        indent++;
        checkDepth(path);
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String stringKey)) {
                throw new JsonException("Map keys must be String values for JSON object serialization");
            }

            if (!first) out.append(',');
            first = false;

            if (prettyPrint) {
                out.append('\n');
                appendIndent();
            }

            JsonWriter.escapeString(stringKey, out);
            out.append(':');
            if (prettyPrint) out.append(' ');

            Object value = entry.getValue();
            if (needsPath(value)) {
                int mark = path.pushField(stringKey);
                try {
                    writeValue(value, path);
                } finally {
                    path.restore(mark);
                }
            } else {
                writeValue(value, path);
            }
        }

        indent--;
        if (prettyPrint && !first) {
            out.append('\n');
            appendIndent();
        }
        out.append('}');

        if (checkCycles) {
            visited.remove(map);
        }
    }

    private void writeArray(Object array, JsonPath path) {
        int len = java.lang.reflect.Array.getLength(array);

        out.append('[');
        indent++;
        checkDepth(path);
        boolean first = true;

        for (int i = 0; i < len; i++) {
            Object element = java.lang.reflect.Array.get(array, i);

            if (!first) out.append(',');
            first = false;

            if (prettyPrint) {
                out.append('\n');
                appendIndent();
            }

            if (needsPath(element)) {
                int mark = path.pushIndex(i);
                try {
                    writeValue(element, path);
                } finally {
                    path.restore(mark);
                }
            } else {
                writeValue(element, path);
            }
        }

        indent--;
        if (prettyPrint && !first) {
            out.append('\n');
            appendIndent();
        }
        out.append(']');
    }

    private void checkDepth(JsonPath path) {
        if (indent >= maxDepth) {
            throw new JsonException("Maximum depth (" + maxDepth + ") exceeded at " + path);
        }
    }

    private void appendIndent() {
        if (indent < INDENT_CACHE.length) {
            out.append(INDENT_CACHE[indent]);
        } else {
            for (int i = 0; i < indent; i++) {
                out.append("  ");
            }
        }
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        int start = 0;
        if (s.charAt(0) == '-') {
            if (s.length() == 1) return false;
            start = 1;
        }
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static boolean needsPath(Object value) {
        if (value == null) return false;
        return value instanceof List
                || value instanceof Map
                || value.getClass().isArray()
                || isPlainObject(value);
    }

    private static boolean isPlainObject(Object value) {
        if (value == null) return false;
        if (value instanceof String
                || value instanceof Character
                || value instanceof Boolean
                || value instanceof Number
                || value instanceof Enum) {
            return false;
        }
        Class<?> type = value.getClass();
        return !type.isArray()
                && !DateCodec.isDateType(type)
                && !(value instanceof Map)
                && !(value instanceof List);
    }

    private static int estimateInitialCapacity(Object value) {
        if (value == null) return 16;
        int estimate;
        if (value instanceof String s) {
            estimate = s.length() + 2;
        } else if (value instanceof List<?> list) {
            estimate = multiplySaturated(list.size(), 64);
        } else if (value instanceof Map<?, ?> map) {
            estimate = multiplySaturated(map.size(), 48);
        } else if (value.getClass().isArray()) {
            estimate = multiplySaturated(java.lang.reflect.Array.getLength(value), 48);
        } else {
            estimate = 256;
        }
        return Math.max(16, Math.min(estimate, 1 << 20));
    }

    private static int multiplySaturated(int value, int multiplier) {
        if (value <= 0) return 16;
        if (value > Integer.MAX_VALUE / multiplier) {
            return Integer.MAX_VALUE;
        }
        return value * multiplier;
    }

    private static final class JsonPath {
        private Object[] stack = new Object[16];
        private int size;

        int pushField(Object key) {
            int mark = size;
            if (size >= stack.length) {
                stack = java.util.Arrays.copyOf(stack, stack.length * 2);
            }
            stack[size++] = key;
            return mark;
        }

        int pushIndex(int index) {
            int mark = size;
            if (size >= stack.length) {
                stack = java.util.Arrays.copyOf(stack, stack.length * 2);
            }
            stack[size++] = index;
            return mark;
        }

        void restore(int mark) {
            size = mark;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append('$');
            for (int i = 0; i < size; i++) {
                Object seg = stack[i];
                if (seg instanceof Integer idx) {
                    sb.append('[').append(idx).append(']');
                } else {
                    sb.append('.').append(seg);
                }
            }
            return sb.toString();
        }
    }
}
