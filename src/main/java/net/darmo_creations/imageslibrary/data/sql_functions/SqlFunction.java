package net.darmo_creations.imageslibrary.data.sql_functions;

import org.sqlite.*;

import java.lang.annotation.*;

/**
 * This annotation is used to declare SQL functions.
 * Annotated classes must extend the {@link Function} class.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlFunction {
  /**
   * The function’s SQL name.
   */
  String name();

  /**
   * The number of arguments the function takes.
   */
  int nArgs();

  /**
   * Optional flags to attach to the function.
   * Defaults to 0.
   *
   * @see Function#FLAG_DETERMINISTIC
   */
  int flags() default 0;
}
