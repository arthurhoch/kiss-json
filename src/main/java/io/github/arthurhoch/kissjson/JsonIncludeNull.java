package io.github.arthurhoch.kissjson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Always include this field in serialized output, even when null.
 * Overrides a global includeNulls=false setting.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonIncludeNull {
}
