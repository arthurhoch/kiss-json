package io.github.arthurhoch.kissjson.internal;

final class JsonWriter {

    private static final char[] HEX = "0123456789abcdef".toCharArray();
    private static final String[] UNICODE_ESCAPES;

    static {
        UNICODE_ESCAPES = new String[32];
        for (int c = 0; c < 32; c++) {
            if (c != '\n' && c != '\r' && c != '\t' && c != '\b' && c != '\f') {
                UNICODE_ESCAPES[c] = "\\u" + HEX[(c >> 12) & 0xF] + HEX[(c >> 8) & 0xF]
                        + HEX[(c >> 4) & 0xF] + HEX[c & 0xF];
            }
        }
    }

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
                    default: out.append(UNICODE_ESCAPES[c]); break;
                }
            } else if (c == '"') {
                out.append("\\\"");
            } else if (c == '\\') {
                out.append("\\\\");
            } else {
                int runStart = i;
                int j = i + 1;
                while (j < len) {
                    char rc = s.charAt(j);
                    if (rc == '"' || rc == '\\' || rc < 0x20) break;
                    j++;
                }
                out.append(s, runStart, j);
                i = j - 1;
            }
        }
        out.append('"');
    }

}
