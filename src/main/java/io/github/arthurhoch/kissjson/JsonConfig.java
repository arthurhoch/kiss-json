package io.github.arthurhoch.kissjson;

import java.time.ZoneId;

/**
 * Immutable configuration for a Json instance.
 * Obtain via {@link Json#config()} or {@link JsonBuilder#build()}.
 */
public final class JsonConfig {

    private final FieldNaming fieldNaming;
    private final boolean includeNulls;
    private final boolean failOnUnknownProperties;
    private final boolean failOnMissingRequiredFields;
    private final boolean failOnNullForPrimitives;
    private final boolean failOnDuplicateKeys;
    private final boolean failOnCycles;
    private final int maxDepth;
    private final boolean prettyPrint;
    private final DateFormat dateFormat;
    private final ZoneId zoneId;
    private final EnumMode enumMode;

    JsonConfig(FieldNaming fieldNaming, boolean includeNulls, boolean failOnUnknownProperties,
               boolean failOnMissingRequiredFields, boolean failOnNullForPrimitives,
               boolean failOnDuplicateKeys, boolean failOnCycles, int maxDepth,
               boolean prettyPrint, DateFormat dateFormat, ZoneId zoneId, EnumMode enumMode) {
        this.fieldNaming = fieldNaming;
        this.includeNulls = includeNulls;
        this.failOnUnknownProperties = failOnUnknownProperties;
        this.failOnMissingRequiredFields = failOnMissingRequiredFields;
        this.failOnNullForPrimitives = failOnNullForPrimitives;
        this.failOnDuplicateKeys = failOnDuplicateKeys;
        this.failOnCycles = failOnCycles;
        this.maxDepth = maxDepth;
        this.prettyPrint = prettyPrint;
        this.dateFormat = dateFormat;
        this.zoneId = zoneId;
        this.enumMode = enumMode;
    }

    /**
     * Returns the field naming strategy. Default: {@link FieldNaming#IDENTITY}.
     *
     * @return the field naming strategy
     */
    public FieldNaming fieldNaming() { return fieldNaming; }

    /**
     * Returns whether null fields are included in output. Default: true.
     *
     * @return true when null fields are included
     */
    public boolean includeNulls() { return includeNulls; }

    /**
     * Returns whether unknown JSON properties fail. Default: false.
     *
     * @return true when unknown properties fail
     */
    public boolean failOnUnknownProperties() { return failOnUnknownProperties; }

    /**
     * Returns whether missing {@link JsonRequired} fields fail. Default: false.
     *
     * @return true when missing required fields fail
     */
    public boolean failOnMissingRequiredFields() { return failOnMissingRequiredFields; }

    /**
     * Returns whether null values for primitives fail. Default: false.
     *
     * @return true when null primitive values fail
     */
    public boolean failOnNullForPrimitives() { return failOnNullForPrimitives; }

    /**
     * Returns whether duplicate JSON object keys fail. Default: false.
     *
     * @return true when duplicate keys fail
     */
    public boolean failOnDuplicateKeys() { return failOnDuplicateKeys; }

    /**
     * Returns whether cycles are detected during serialization. Default: true.
     *
     * @return true when cycles fail during serialization
     */
    public boolean failOnCycles() { return failOnCycles; }

    /**
     * Returns the maximum nesting depth. Default: 128.
     *
     * @return the maximum nesting depth
     */
    public int maxDepth() { return maxDepth; }

    /**
     * Returns whether output is formatted with indentation. Default: false.
     *
     * @return true when output is pretty printed
     */
    public boolean prettyPrint() { return prettyPrint; }

    /**
     * Returns the date format strategy. Default: {@link DateFormat#ISO}.
     *
     * @return the date format strategy
     */
    public DateFormat dateFormat() { return dateFormat; }

    /**
     * Returns the timezone for Date/Calendar. Default: UTC.
     *
     * @return the zone id
     */
    public ZoneId zoneId() { return zoneId; }

    /**
     * Returns the enum mode. Default: {@link EnumMode#NAME}.
     *
     * @return the enum mode
     */
    public EnumMode enumMode() { return enumMode; }
}
