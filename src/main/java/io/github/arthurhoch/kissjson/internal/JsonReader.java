package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.JsonConfig;
import io.github.arthurhoch.kissjson.JsonException;
import io.github.arthurhoch.kissjson.JsonParseException;

import java.math.BigDecimal;

final class JsonReader {

    private final char[] buf;
    private final int length;
    private final int maxDepth;
    private final boolean checkDuplicates;

    int offset;
    int line = 1;
    int column = 1;
    private int tokenOffset;
    private int tokenLine = 1;
    private int tokenColumn = 1;

    private JsonTokenType current;
    private String stringValue;
    private boolean boolValue;
    private boolean numberIsLong;
    private long numberLongValue;
    private BigDecimal numberDecimalValue;
    private int depth;

    JsonReader(String json, JsonConfig config) {
        this.buf = json.toCharArray();
        this.length = buf.length;
        this.maxDepth = config.maxDepth();
        this.checkDuplicates = config.failOnDuplicateKeys();
    }

    int line() { return line; }
    int column() { return column; }
    int offset() { return offset; }

    boolean failOnDuplicateKeys() { return checkDuplicates; }

    JsonTokenType currentToken() {
        return current;
    }

    String stringValue() {
        return stringValue;
    }

    boolean boolValue() {
        return boolValue;
    }

    long longValue() {
        return numberIsLong ? numberLongValue : numberDecimalValue.longValue();
    }

    double doubleValue() {
        return numberIsLong ? (double) numberLongValue : numberDecimalValue.doubleValue();
    }

    BigDecimal decimalValue() {
        return numberIsLong ? BigDecimal.valueOf(numberLongValue) : numberDecimalValue;
    }

    boolean isIntegral() {
        if (numberIsLong) return true;
        if (numberDecimalValue != null) {
            try {
                numberDecimalValue.longValueExact();
                return true;
            } catch (ArithmeticException e) {
                return false;
            }
        }
        return false;
    }

    JsonTokenType nextToken() {
        skipWhitespace();
        tokenOffset = offset;
        tokenLine = line;
        tokenColumn = column;
        if (offset >= length) {
            current = JsonTokenType.END;
            return current;
        }
        char c = buf[offset];
        switch (c) {
            case '{':
                offset++;
                column++;
                current = JsonTokenType.OBJECT_START;
                return current;
            case '}':
                offset++;
                column++;
                current = JsonTokenType.OBJECT_END;
                return current;
            case '[':
                offset++;
                column++;
                current = JsonTokenType.ARRAY_START;
                return current;
            case ']':
                offset++;
                column++;
                current = JsonTokenType.ARRAY_END;
                return current;
            case ':':
                offset++;
                column++;
                current = JsonTokenType.COLON;
                return current;
            case ',':
                offset++;
                column++;
                current = JsonTokenType.COMMA;
                return current;
            case '"':
                readString();
                current = JsonTokenType.STRING;
                return current;
            case 't':
                readLiteral("true");
                boolValue = true;
                current = JsonTokenType.BOOLEAN;
                return current;
            case 'f':
                readLiteral("false");
                boolValue = false;
                current = JsonTokenType.BOOLEAN;
                return current;
            case 'n':
                readLiteral("null");
                current = JsonTokenType.NULL;
                return current;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) {
                    readNumber();
                    current = JsonTokenType.NUMBER;
                    return current;
                }
                throw parseError("Unexpected character '" + escapeChar(c) + "'");
        }
    }

    void consume(JsonTokenType expected) {
        if (current != expected) {
            throw parseError("Expected " + expected + " but was " + current);
        }
        nextToken();
    }

    void readObjectStart() {
        if (current != JsonTokenType.OBJECT_START) {
            throw parseError("Expected '{' but was " + current);
        }
        incrementAndCheckDepth();
        nextToken();
    }

    void readObjectEnd() {
        if (current != JsonTokenType.OBJECT_END) {
            throw parseError("Expected '}' but was " + current);
        }
        decrementDepth();
        nextToken();
    }

    void readArrayStart() {
        if (current != JsonTokenType.ARRAY_START) {
            throw parseError("Expected '[' but was " + current);
        }
        incrementAndCheckDepth();
        nextToken();
    }

    void readArrayEnd() {
        if (current != JsonTokenType.ARRAY_END) {
            throw parseError("Expected ']' but was " + current);
        }
        decrementDepth();
        nextToken();
    }

    String readKey() {
        if (current != JsonTokenType.STRING) {
            throw parseError("Expected string for object key but was " + current);
        }
        String key = stringValue;
        nextToken();
        consume(JsonTokenType.COLON);
        return key;
    }

    boolean hasNextEntry() {
        if (current == JsonTokenType.OBJECT_END) {
            return false;
        }
        return true;
    }

    boolean hasNextElement() {
        if (current == JsonTokenType.ARRAY_END) {
            return false;
        }
        return true;
    }

    void nextEntryOrEnd() {
        if (current == JsonTokenType.COMMA) {
            nextToken();
            if (current == JsonTokenType.OBJECT_END) {
                throw parseError("Trailing comma in object");
            }
        } else if (current != JsonTokenType.OBJECT_END) {
            throw parseError("Expected ',' or '}' in object");
        }
    }

    void nextElementOrEnd() {
        if (current == JsonTokenType.COMMA) {
            nextToken();
            if (current == JsonTokenType.ARRAY_END) {
                throw parseError("Trailing comma in array");
            }
        } else if (current != JsonTokenType.ARRAY_END) {
            throw parseError("Expected ',' or ']' in array");
        }
    }

    void skipValue() {
        switch (current) {
            case STRING:
            case NUMBER:
            case BOOLEAN:
            case NULL:
                nextToken();
                break;
            case OBJECT_START:
                skipObjectValue();
                break;
            case ARRAY_START:
                skipArrayValue();
                break;
            default:
                throw parseError("Unexpected token when skipping value: " + current);
        }
    }

    private void skipObjectValue() {
        readObjectStart();
        java.util.HashSet<String> seenKeys = checkDuplicates ? new java.util.HashSet<>() : null;
        while (hasNextEntry()) {
            if (seenKeys != null) {
                String key = readKey();
                if (!seenKeys.add(key)) {
                    throw parseError("Duplicate key '" + key + "'");
                }
            } else {
                skipKey();
            }
            skipValue();
            nextEntryOrEnd();
        }
        readObjectEnd();
    }

    private void skipArrayValue() {
        readArrayStart();
        while (hasNextElement()) {
            skipValue();
            nextElementOrEnd();
        }
        readArrayEnd();
    }

    private void skipKey() {
        if (current != JsonTokenType.STRING) {
            throw parseError("Expected string for object key but was " + current);
        }
        nextToken();
        consume(JsonTokenType.COLON);
    }

    void ensureFullyConsumed() {
        if (current != JsonTokenType.END) {
            throw new JsonParseException(
                    "Unexpected character after JSON value at line " + tokenLine
                            + ", column " + tokenColumn + " (offset " + tokenOffset + ")",
                    tokenLine, tokenColumn, tokenOffset);
        }
    }

    private void readString() {
        offset++;
        column++;
        StringBuilder sb = null;
        int start = offset;
        while (offset < length) {
            char c = buf[offset];
            if (c == '"') {
                String result = (sb == null)
                        ? new String(buf, start, offset - start)
                        : sb.toString();
                offset++;
                column++;
                stringValue = result;
                return;
            }
            if (c == '\\') {
                if (sb == null) {
                    sb = new StringBuilder(offset - start + 16);
                    sb.append(buf, start, offset - start);
                }
                offset++;
                column++;
                if (offset >= length) {
                    throw parseError("Unexpected end of input in string escape");
                }
                readEscapeChar(sb);
                continue;
            }
            if (c < 0x20) {
                throw parseError("Unescaped control character U+" + String.format("%04X", (int) c) + " in string");
            }
            if (sb != null) {
                sb.append(c);
            }
            offset++;
            column++;
        }
        throw parseError("Unterminated string");
    }

    private void readEscapeChar(StringBuilder sb) {
        char c = buf[offset];
        offset++;
        column++;
        switch (c) {
            case '"': sb.append('"'); return;
            case '\\': sb.append('\\'); return;
            case '/': sb.append('/'); return;
            case 'b': sb.append('\b'); return;
            case 'f': sb.append('\f'); return;
            case 'n': sb.append('\n'); return;
            case 'r': sb.append('\r'); return;
            case 't': sb.append('\t'); return;
            case 'u':
                readUnicodeEscape(sb);
                return;
            default:
                throw parseError("Invalid escape sequence '\\" + c + "'");
        }
    }

    private void readUnicodeEscape(StringBuilder sb) {
        if (offset + 4 > length) {
            throw parseError("Incomplete Unicode escape sequence");
        }
        int codePoint = 0;
        for (int i = 0; i < 4; i++) {
            int val = hexValue(buf[offset + i]);
            if (val < 0) {
                throw parseError("Invalid hex digit in Unicode escape");
            }
            codePoint = (codePoint << 4) | val;
        }
        offset += 4;
        column += 4;

        if (Character.isHighSurrogate((char) codePoint)) {
            if (offset + 6 <= length && buf[offset] == '\\' && buf[offset + 1] == 'u') {
                offset += 2;
                column += 2;
                int lowCode = 0;
                for (int i = 0; i < 4; i++) {
                    int val = hexValue(buf[offset + i]);
                    if (val < 0) {
                        throw parseError("Invalid hex digit in surrogate escape");
                    }
                    lowCode = (lowCode << 4) | val;
                }
                offset += 4;
                column += 4;
                if (!Character.isLowSurrogate((char) lowCode)) {
                    throw parseError("Invalid low surrogate");
                }
                sb.append(Character.toChars(Character.toCodePoint((char) codePoint, (char) lowCode)));
                return;
            }
            throw parseError("High surrogate without matching low surrogate");
        }
        if (Character.isLowSurrogate((char) codePoint)) {
            throw parseError("Low surrogate without matching high surrogate");
        }
        sb.append((char) codePoint);
    }

    private void readNumber() {
        int start = offset;

        if (buf[offset] == '-') {
            offset++;
            column++;
        }

        if (offset >= length) {
            throw parseError("Unexpected end of input in number");
        }

        if (buf[offset] == '0') {
            offset++;
            column++;
            if (offset < length && buf[offset] >= '0' && buf[offset] <= '9') {
                throw parseError("Leading zeros not allowed");
            }
        } else if (buf[offset] >= '1' && buf[offset] <= '9') {
            while (offset < length && buf[offset] >= '0' && buf[offset] <= '9') {
                offset++;
                column++;
            }
        } else {
            throw parseError("Invalid number format");
        }

        boolean isDecimal = false;
        if (offset < length && buf[offset] == '.') {
            isDecimal = true;
            offset++;
            column++;
            if (offset >= length || buf[offset] < '0' || buf[offset] > '9') {
                throw parseError("Expected digit after decimal point");
            }
            while (offset < length && buf[offset] >= '0' && buf[offset] <= '9') {
                offset++;
                column++;
            }
        }

        if (offset < length && (buf[offset] == 'e' || buf[offset] == 'E')) {
            isDecimal = true;
            offset++;
            column++;
            if (offset < length && (buf[offset] == '+' || buf[offset] == '-')) {
                offset++;
                column++;
            }
            if (offset >= length || buf[offset] < '0' || buf[offset] > '9') {
                throw parseError("Expected digit after exponent");
            }
            while (offset < length && buf[offset] >= '0' && buf[offset] <= '9') {
                offset++;
                column++;
            }
        }

        if (!isDecimal) {
            if (!readLongValue(start, offset)) {
                numberIsLong = false;
                numberDecimalValue = new BigDecimal(new String(buf, start, offset - start));
            }
        } else {
            numberIsLong = false;
            numberDecimalValue = new BigDecimal(new String(buf, start, offset - start));
        }
    }

    private boolean readLongValue(int start, int end) {
        boolean negative = false;
        int pos = start;
        if (buf[pos] == '-') {
            negative = true;
            pos++;
        }

        long limit = negative ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long multmin = limit / 10;
        long result = 0;

        while (pos < end) {
            int digit = buf[pos++] - '0';
            if (result < multmin) {
                return false;
            }
            result *= 10;
            if (result < limit + digit) {
                return false;
            }
            result -= digit;
        }

        numberIsLong = true;
        numberLongValue = negative ? result : -result;
        numberDecimalValue = null;
        return true;
    }

    private void readLiteral(String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (offset >= length || buf[offset] != expected.charAt(i)) {
                throw parseError("Expected '" + expected + "'");
            }
            offset++;
            column++;
        }
    }

    private void skipWhitespace() {
        while (offset < length) {
            char c = buf[offset];
            if (c == ' ' || c == '\t') {
                offset++;
                column++;
            } else if (c == '\n') {
                offset++;
                line++;
                column = 1;
            } else if (c == '\r') {
                offset++;
                line++;
                column = 1;
                if (offset < length && buf[offset] == '\n') {
                    offset++;
                }
            } else {
                break;
            }
        }
    }

    void incrementAndCheckDepth() {
        depth++;
        if (depth > maxDepth) {
            throw new JsonParseException("Maximum depth (" + maxDepth + ") exceeded at line " + line + ", column " + column, line, column, offset);
        }
    }

    void decrementDepth() {
        depth--;
    }

    private int hexValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    private String escapeChar(char c) {
        switch (c) {
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\t': return "\\t";
            default:
                return String.valueOf(c);
        }
    }

    JsonParseException parseError(String message) {
        return new JsonParseException(
                message + " at line " + line + ", column " + column + " (offset " + offset + ")",
                line, column, offset);
    }
}
