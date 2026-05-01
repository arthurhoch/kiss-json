package io.github.arthurhoch.kissjson;

/**
 * Enum serialization and deserialization modes.
 */
public enum EnumMode {
    /** Use Enum.name() for serialization and matching. */
    NAME,
    /** Use Enum.toString() for serialization and matching. */
    TO_STRING
}
