package io.github.arthurhoch.kissjson;

/**
 * Base exception for all KissJson errors.
 */
public class JsonException extends RuntimeException {

    /**
     * Constructs a new JsonException with the specified message.
     *
     * @param message the detail message
     */
    public JsonException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
