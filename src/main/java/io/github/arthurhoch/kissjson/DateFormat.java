package io.github.arthurhoch.kissjson;

/**
 * Date/time format strategies for serialization and deserialization.
 */
public enum DateFormat {
    /** ISO-8601 string format. */
    ISO,
    /** Unix epoch in milliseconds. */
    EPOCH_MILLIS,
    /** Unix epoch in seconds. */
    EPOCH_SECONDS
}
