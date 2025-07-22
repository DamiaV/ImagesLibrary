package net.darmo_creations.bildumilo.data;

import org.intellij.lang.annotations.*;

import java.lang.annotation.*;

/**
 * {@link String} elements annotated with this annotation are marked as containing SQLite code.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.SOURCE)
@Language("sqlite")
public @interface SQLite {
}
