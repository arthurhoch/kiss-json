package io.github.arthurhoch.kissjson.internal;

import io.github.arthurhoch.kissjson.FieldNaming;

final class NamingStrategy {

    private NamingStrategy() {
    }

    public static String apply(FieldNaming strategy, String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        switch (strategy) {
            case IDENTITY:
                return fieldName;
            case LOWER_CASE:
                return fieldName.toLowerCase();
            case UPPER_CASE:
                return fieldName.toUpperCase();
            case CAMEL_CASE:
                return toCamelCase(fieldName);
            case SNAKE_CASE:
                return toSnakeCase(fieldName, '_');
            case KEBAB_CASE:
                return toSnakeCase(fieldName, '-');
            default:
                return fieldName;
        }
    }

    private static String toCamelCase(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        int i = 0;
        while (i < name.length()) {
            char c = name.charAt(i);
            if (c == '_' || c == '-') {
                i++;
                if (i < name.length()) {
                    char next = name.charAt(i);
                    if (next >= 'a' && next <= 'z') {
                        sb.append((char) (next - 32));
                    } else {
                        sb.append(next);
                    }
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    private static String toSnakeCase(String name, char separator) {
        StringBuilder sb = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (i > 0) {
                    char prev = name.charAt(i - 1);
                    if (prev >= 'a' && prev <= 'z') {
                        sb.append(separator);
                    } else if (prev >= 'A' && prev <= 'Z' && i + 1 < name.length()) {
                        char next = name.charAt(i + 1);
                        if (next >= 'a' && next <= 'z') {
                            sb.append(separator);
                        }
                    }
                }
                sb.append((char) (c + 32));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
