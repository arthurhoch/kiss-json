package io.github.arthurhoch.kissjson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a custom date/time format pattern for a field.
 * Overrides the global dateFormat setting.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonDateFormat {
    /**
     * Returns the DateTimeFormatter pattern string.
     *
     * @return the DateTimeFormatter pattern string
     */
    String value();
}
