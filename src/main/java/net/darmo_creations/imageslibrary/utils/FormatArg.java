package net.darmo_creations.imageslibrary.utils;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Format arguments are used by the {@link StringUtils#format(String, FormatArg...)} method.
 * A format argument represents a single named value.
 * The argument’s value will substitute the placeholder with the argument’s name.
 *
 * @param name  Argument’s name.
 * @param value Argument’s value.
 */
public record FormatArg(@NotNull String name, Object value) {
  public FormatArg {
    Objects.requireNonNull(name);
  }
}
