package io.github.arthurhoch.kissjson.internal;

final class JsonWriter {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private JsonWriter() {
    }

    static void escapeString(String s, StringBuilder out) {
        out.append('"');
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c < 0x20) {
                break;
            }
            i++;
        }
        if (i == len) {
            out.append(s);
            out.append('"');
            return;
        }

        out.append(s, 0, i);
        for (; i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x20) {
                switch (c) {
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    case '\b': out.append("\\b"); break;
                    case '\f': out.append("\\f"); break;
                    default: appendUnicodeEscape(out, c); break;
                }
            } else {
                switch (c) {
                    case '"':  out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    default:   out.append(c); break;
                }
            }
        }
        out.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder out, char c) {
        out.append("\\u");
        out.append(HEX[(c >> 12) & 0xF]);
        out.append(HEX[(c >> 8) & 0xF]);
        out.append(HEX[(c >> 4) & 0xF]);
        out.append(HEX[c & 0xF]);
    }
}
