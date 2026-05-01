package io.github.arthurhoch.kissjson;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for KissJson serialization and deserialization.
 * <p>
 * Usage:
 * <pre>{@code
 * Json json = Json.create();
 * String text = json.stringify(object);
 * User user = json.parse(text, User.class);
 * }</pre>
 */
public final class Json {

    private final JsonConfig config;

    Json(JsonConfig config) {
        this.config = config;
    }

    /**
     * Creates a Json instance with default configuration.
     *
     * @return a new Json instance
     */
    public static Json create() {
        return new Json(new JsonConfig(
            FieldNaming.IDENTITY, true, false, false, false, false, true,
            128, false, DateFormat.ISO, java.time.ZoneId.of("UTC"), EnumMode.NAME
        ));
    }

    /**
     * Returns a new JsonBuilder for configuring a Json instance.
     *
     * @return a new builder
     */
    public static JsonBuilder builder() {
        return new JsonBuilder();
    }

    /**
     * Returns the immutable configuration for this instance.
     *
     * @return the configuration
     */
    public JsonConfig config() {
        return config;
    }

    /**
     * Serializes the given Java object to a JSON string.
     *
     * @param value the object to serialize (may be null)
     * @return the JSON string
     * @throws JsonException if cycle detection triggers or max depth is exceeded
     */
    public String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        return RuntimeBridge.write(value, config);
    }

    /**
     * Deserializes a JSON string to an object of the given type.
     *
     * @param json       the JSON string
     * @param targetType the target class
     * @param <T>        the target type
     * @return the deserialized object
     * @throws NullPointerException    if json or targetType is null
     * @throws JsonParseException      if the JSON is syntactically invalid
     * @throws JsonMappingException    if the JSON cannot be mapped to the target type
     */
    @SuppressWarnings("unchecked")
    public <T> T parse(String json, Class<T> targetType) {
        if (json == null) throw new NullPointerException("JSON string must not be null");
        if (targetType == null) throw new NullPointerException("Target type must not be null");
        return (T) RuntimeBridge.parseAndRead(json, targetType, config);
    }

    /**
     * Deserializes a JSON array to a List.
     *
     * @param json        the JSON array string
     * @param elementType the element type
     * @param <T>         the element type
     * @return a List of deserialized elements
     * @throws NullPointerException    if json or elementType is null
     * @throws JsonParseException      if the JSON is syntactically invalid or not an array
     * @throws JsonMappingException    if an element cannot be mapped
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> parseList(String json, Class<T> elementType) {
        if (json == null) throw new NullPointerException("JSON string must not be null");
        if (elementType == null) throw new NullPointerException("Element type must not be null");
        return (List<T>) RuntimeBridge.parseAndReadList(json, elementType, config);
    }

    /**
     * Deserializes a JSON object to a Map with Object values.
     *
     * @param json the JSON object string
     * @return a Map of String to Object
     * @throws NullPointerException    if json is null
     * @throws JsonParseException      if the JSON is syntactically invalid
     */
    public Map<String, Object> parseMap(String json) {
        if (json == null) throw new NullPointerException("JSON string must not be null");
        return RuntimeBridge.parseAndReadMapUntyped(json, config);
    }

    /**
     * Deserializes a JSON object to a Map with typed values.
     *
     * @param json      the JSON object string
     * @param valueType the value type class
     * @param <T>       the value type
     * @return a Map of String to T
     * @throws NullPointerException    if json or valueType is null
     * @throws JsonParseException      if the JSON is syntactically invalid
     * @throws JsonMappingException    if a value cannot be mapped
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> parseMap(String json, Class<T> valueType) {
        if (json == null) throw new NullPointerException("JSON string must not be null");
        if (valueType == null) throw new NullPointerException("Value type must not be null");
        return (Map<String, T>) RuntimeBridge.parseAndReadMap(json, valueType, config);
    }

    private static final class RuntimeBridge {
        private static final MethodHandle WRITE;
        private static final MethodHandle PARSE_AND_READ;
        private static final MethodHandle PARSE_AND_READ_LIST;
        private static final MethodHandle PARSE_AND_READ_MAP;
        private static final MethodHandle PARSE_AND_READ_MAP_UNTYPED;

        static {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                Class<?> objectWriter = Class.forName("io.github.arthurhoch.kissjson.internal.ObjectWriter");
                Class<?> objectReader = Class.forName("io.github.arthurhoch.kissjson.internal.ObjectReader");
                MethodHandles.Lookup writerLookup = MethodHandles.privateLookupIn(objectWriter, lookup);
                MethodHandles.Lookup readerLookup = MethodHandles.privateLookupIn(objectReader, lookup);

                WRITE = writerLookup.findStatic(
                        objectWriter,
                        "write",
                        MethodType.methodType(String.class, Object.class, JsonConfig.class));
                PARSE_AND_READ = readerLookup.findStatic(
                        objectReader,
                        "parseAndRead",
                        MethodType.methodType(Object.class, String.class, Class.class, JsonConfig.class));
                PARSE_AND_READ_LIST = readerLookup.findStatic(
                        objectReader,
                        "parseAndReadList",
                        MethodType.methodType(List.class, String.class, Class.class, JsonConfig.class));
                PARSE_AND_READ_MAP = readerLookup.findStatic(
                        objectReader,
                        "parseAndReadMap",
                        MethodType.methodType(Map.class, String.class, Class.class, JsonConfig.class));
                PARSE_AND_READ_MAP_UNTYPED = readerLookup.findStatic(
                        objectReader,
                        "parseAndReadMapUntyped",
                        MethodType.methodType(Map.class, String.class, JsonConfig.class));
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private RuntimeBridge() {
        }

        static String write(Object value, JsonConfig config) {
            try {
                return (String) WRITE.invokeExact(value, config);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        static Object parseAndRead(String json, Class<?> targetType, JsonConfig config) {
            try {
                return PARSE_AND_READ.invokeExact(json, targetType, config);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        static List<?> parseAndReadList(String json, Class<?> elementType, JsonConfig config) {
            try {
                return (List<?>) PARSE_AND_READ_LIST.invokeExact(json, elementType, config);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @SuppressWarnings("unchecked")
        static Map<String, ?> parseAndReadMap(String json, Class<?> valueType, JsonConfig config) {
            try {
                return (Map<String, ?>) PARSE_AND_READ_MAP.invokeExact(json, valueType, config);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> parseAndReadMapUntyped(String json, JsonConfig config) {
            try {
                return (Map<String, Object>) PARSE_AND_READ_MAP_UNTYPED.invokeExact(json, config);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            if (t instanceof Error error) {
                throw error;
            }
            return new JsonException("Internal KissJson invocation failed: " + t.getMessage(), t);
        }
    }
}
