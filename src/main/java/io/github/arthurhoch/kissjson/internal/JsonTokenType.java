package io.github.arthurhoch.kissjson.internal;

enum JsonTokenType {
    OBJECT_START,
    OBJECT_END,
    ARRAY_START,
    ARRAY_END,
    COLON,
    COMMA,
    STRING,
    NUMBER,
    BOOLEAN,
    NULL,
    END
}
