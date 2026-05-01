package io.github.arthurhoch.kissjson;

/**
 * Naming strategies for mapping Java field names to JSON keys.
 */
public enum FieldNaming {
    /** Use the Java field name as-is. */
    IDENTITY,
    /** Lowercase the entire field name. */
    LOWER_CASE,
    /** Uppercase the entire field name. */
    UPPER_CASE,
    /** Convert to lower camelCase. */
    CAMEL_CASE,
    /** Convert to snake_case. */
    SNAKE_CASE,
    /** Convert to kebab-case. */
    KEBAB_CASE
}
