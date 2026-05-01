package io.github.arthurhoch.kissjson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exclude this field from serialized output when null.
 * Overrides a global includeNulls=true setting.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonExcludeNull {
}
