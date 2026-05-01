package io.github.arthurhoch.kissjson;

/**
 * Thrown when JSON text is syntactically invalid.
 * Provides line, column, and offset information.
 */
public final class JsonParseException extends JsonException {

    /** 1-based line number where the error occurred. */
    private final int line;
    /** 1-based column number where the error occurred. */
    private final int column;
    /** 0-based character offset where the error occurred. */
    private final int offset;

    /**
     * Constructs a new JsonParseException.
     *
     * @param message the detail message
     * @param line    1-based line number
     * @param column  1-based column number
     * @param offset  0-based character offset
     */
    public JsonParseException(String message, int line, int column, int offset) {
        super(message);
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    /**
     * Constructs a new JsonParseException with a cause.
     *
     * @param message the detail message
     * @param line    1-based line number
     * @param column  1-based column number
     * @param offset  0-based character offset
     * @param cause   the cause
     */
    public JsonParseException(String message, int line, int column, int offset, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    /**
     * Returns the 1-based line number where the error occurred.
     *
     * @return the 1-based line number
     */
    public int line() {
        return line;
    }

    /**
     * Returns the 1-based column number where the error occurred.
     *
     * @return the 1-based column number
     */
    public int column() {
        return column;
    }

    /**
     * Returns the 0-based character offset where the error occurred.
     *
     * @return the 0-based character offset
     */
    public int offset() {
        return offset;
    }
}
