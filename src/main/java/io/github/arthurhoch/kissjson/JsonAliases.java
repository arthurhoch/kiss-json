package io.github.arthurhoch.kissjson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies alternative JSON key names accepted during deserialization.
 * First matching alias wins (order matters).
 * Serialization always uses the primary name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonAliases {
    /**
     * Returns alternative JSON key names.
     *
     * @return alternative JSON key names
     */
    String[] value();
}
