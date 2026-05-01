package io.github.arthurhoch.kissjson;

import java.time.ZoneId;
import java.util.Objects;

/**
 * Builder for creating configured {@link Json} instances.
 * All setter methods return this builder for chaining.
 */
public final class JsonBuilder {

    private FieldNaming fieldNaming = FieldNaming.IDENTITY;
    private boolean includeNulls = true;
    private boolean failOnUnknownProperties = false;
    private boolean failOnMissingRequiredFields = false;
    private boolean failOnNullForPrimitives = false;
    private boolean failOnDuplicateKeys = false;
    private boolean failOnCycles = true;
    private int maxDepth = 128;
    private boolean prettyPrint = false;
    private DateFormat dateFormat = DateFormat.ISO;
    private ZoneId zoneId = ZoneId.of("UTC");
    private EnumMode enumMode = EnumMode.NAME;

    JsonBuilder() {}

    /**
     * Sets the field naming strategy. Default: {@link FieldNaming#IDENTITY}.
     *
     * @param fieldNaming the field naming strategy
     * @return this builder
     */
    public JsonBuilder fieldNaming(FieldNaming fieldNaming) {
        this.fieldNaming = Objects.requireNonNull(fieldNaming, "fieldNaming must not be null");
        return this;
    }

    /**
     * Sets whether to include null fields in output. Default: true.
     *
     * @param includeNulls whether null fields are included
     * @return this builder
     */
    public JsonBuilder includeNulls(boolean includeNulls) {
        this.includeNulls = includeNulls;
        return this;
    }

    /**
     * Sets whether to fail on unknown JSON properties. Default: false.
     *
     * @param failOnUnknownProperties whether unknown properties fail
     * @return this builder
     */
    public JsonBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
        this.failOnUnknownProperties = failOnUnknownProperties;
        return this;
    }

    /**
     * Sets whether to fail on missing {@link JsonRequired} fields. Default: false.
     *
     * @param failOnMissingRequiredFields whether missing required fields fail
     * @return this builder
     */
    public JsonBuilder failOnMissingRequiredFields(boolean failOnMissingRequiredFields) {
        this.failOnMissingRequiredFields = failOnMissingRequiredFields;
        return this;
    }

    /**
     * Sets whether to fail when null maps to a primitive. Default: false.
     *
     * @param failOnNullForPrimitives whether null primitive values fail
     * @return this builder
     */
    public JsonBuilder failOnNullForPrimitives(boolean failOnNullForPrimitives) {
        this.failOnNullForPrimitives = failOnNullForPrimitives;
        return this;
    }

    /**
     * Sets whether to fail on duplicate JSON object keys. Default: false.
     *
     * @param failOnDuplicateKeys whether duplicate keys fail
     * @return this builder
     */
    public JsonBuilder failOnDuplicateKeys(boolean failOnDuplicateKeys) {
        this.failOnDuplicateKeys = failOnDuplicateKeys;
        return this;
    }

    /**
     * Sets whether to detect cycles during serialization. Default: true.
     *
     * @param failOnCycles whether cycles fail during serialization
     * @return this builder
     */
    public JsonBuilder failOnCycles(boolean failOnCycles) {
        this.failOnCycles = failOnCycles;
        return this;
    }

    /**
     * Sets the maximum nesting depth. Default: 128.
     *
     * @param maxDepth maximum nesting depth, at least 1
     * @return this builder
     * @throws IllegalArgumentException if maxDepth is less than 1
     */
    public JsonBuilder maxDepth(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be at least 1");
        }
        this.maxDepth = maxDepth;
        return this;
    }

    /**
     * Sets whether to format output with indentation. Default: false.
     *
     * @param prettyPrint whether output is pretty printed
     * @return this builder
     */
    public JsonBuilder prettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        return this;
    }

    /**
     * Sets the date format strategy. Default: {@link DateFormat#ISO}.
     *
     * @param dateFormat the date format strategy
     * @return this builder
     */
    public JsonBuilder dateFormat(DateFormat dateFormat) {
        this.dateFormat = Objects.requireNonNull(dateFormat, "dateFormat must not be null");
        return this;
    }

    /**
     * Sets the timezone for Date/Calendar. Default: UTC.
     *
     * @param zoneId the zone id
     * @return this builder
     */
    public JsonBuilder zoneId(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
        return this;
    }

    /**
     * Sets the enum mode. Default: {@link EnumMode#NAME}.
     *
     * @param enumMode the enum mode
     * @return this builder
     */
    public JsonBuilder enumMode(EnumMode enumMode) {
        this.enumMode = Objects.requireNonNull(enumMode, "enumMode must not be null");
        return this;
    }

    /**
     * Creates a new Json instance with the configured options.
     *
     * @return a configured Json instance
     */
    public Json build() {
        JsonConfig config = new JsonConfig(
            fieldNaming, includeNulls, failOnUnknownProperties,
            failOnMissingRequiredFields, failOnNullForPrimitives,
            failOnDuplicateKeys, failOnCycles, maxDepth,
            prettyPrint, dateFormat, zoneId, enumMode
        );
        return new Json(config);
    }
}
