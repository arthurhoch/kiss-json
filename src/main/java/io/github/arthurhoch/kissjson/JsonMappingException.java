package io.github.arthurhoch.kissjson;

/**
 * Thrown when valid JSON cannot be mapped to the target Java type.
 * Provides JSON path, target type, field name, and type information.
 */
public final class JsonMappingException extends JsonException {

    /** JSON path to the error location. */
    private final String jsonPath;
    /** Target Java type being mapped. */
    private final Class<?> targetType;
    /** Java field name being mapped, when applicable. */
    private final String fieldName;
    /** Expected Java type, when applicable. */
    private final Class<?> expectedType;
    /** Actual JSON value or token that caused the error. */
    private final Object actualValue;

    /**
     * Constructs a new JsonMappingException.
     *
     * @param message      the detail message
     * @param jsonPath     the JSON path to the error location
     * @param targetType   the target Java type
     * @param fieldName    the Java field name, or null
     * @param expectedType the expected Java type, or null
     * @param actualValue  the actual value, or null
     */
    public JsonMappingException(String message, String jsonPath, Class<?> targetType,
                                String fieldName, Class<?> expectedType, Object actualValue) {
        super(message);
        this.jsonPath = jsonPath;
        this.targetType = targetType;
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualValue = actualValue;
    }

    /**
     * Constructs a new JsonMappingException with a cause.
     *
     * @param message      the detail message
     * @param jsonPath     the JSON path to the error location
     * @param targetType   the target Java type
     * @param fieldName    the Java field name, or null
     * @param expectedType the expected Java type, or null
     * @param actualValue  the actual value, or null
     * @param cause        the cause
     */
    public JsonMappingException(String message, String jsonPath, Class<?> targetType,
                                String fieldName, Class<?> expectedType, Object actualValue,
                                Throwable cause) {
        super(message, cause);
        this.jsonPath = jsonPath;
        this.targetType = targetType;
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualValue = actualValue;
    }

    /**
     * Returns the JSON path to the error location (e.g., "$.user.address.city").
     *
     * @return the JSON path, or null
     */
    public String jsonPath() {
        return jsonPath;
    }

    /**
     * Returns the target Java class being deserialized, or null.
     *
     * @return the target Java class, or null
     */
    public Class<?> targetType() {
        return targetType;
    }

    /**
     * Returns the Java field name where the error occurred, or null.
     *
     * @return the Java field name, or null
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Returns the expected Java type, or null.
     *
     * @return the expected Java type, or null
     */
    public Class<?> expectedType() {
        return expectedType;
    }

    /**
     * Returns the actual JSON value that caused the error, or null.
     *
     * @return the actual JSON value, or null
     */
    public Object actualValue() {
        return actualValue;
    }
}
