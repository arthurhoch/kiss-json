package io.github.arthurhoch.kissjson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a custom JSON key name for a field.
 * Takes precedence over naming strategies.
 * Applies to both serialization and deserialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonName {
    /**
     * Returns the custom JSON key name.
     *
     * @return the custom JSON key name
     */
    String value();
}
