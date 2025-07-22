package net.darmo_creations.bildumilo.utils;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Class providing methods to handle strings.
 */
public final class StringUtils {
  /**
   * Strip a string of all leading and trailing whitespace. If the string is null, is empty,
   * or only contains whitespace, an empty {@link Optional} is returned.
   *
   * @param s A string.
   * @return The same string stripped of all leading and trailing whitespace or an empty optional
   * if it was null, empty, or all whitespace.
   */
  public static Optional<String> stripNullable(String s) {
    return s == null || s.isBlank() ? Optional.empty() : Optional.of(s.strip());
  }

  /**
   * Format a string using named brace placeholders.
   * <p>
   * Each placeholder must be of the form {@code {name}}.
   *
   * @param pattern String pattern.
   * @param args    Arguments to substitute to placeholders.
   *                Placeholders will be substituted by the value of the format argument with the exact same name.
   * @return The formatted string.
   * @throws IllegalArgumentException If two or more format arguments share the same name.
   */
  public static String format(@NotNull String pattern, final @NotNull FormatArg... args) {
    final Set<String> argNames = new HashSet<>();
    for (final var arg : args) {
      final String name = arg.name();
      if (argNames.contains(name))
        throw new IllegalArgumentException("Duplicate format argument: " + name);
      argNames.add(name);
      pattern = pattern.replace("{" + name + "}", Objects.toString(arg.value()));
    }
    return pattern;
  }

  /**
   * Convert an RGB color to an hex CSS color value.
   *
   * @param color An RBG color.
   * @return A CSS color value in the {@code #RRGGBB} format.
   */
  public static String colorToCss(int color) {
    return "#%02X%02X%02X".formatted(
        color >> 16 & 0xff,
        color >> 8 & 0xff,
        color & 0xff
    );
  }

  private StringUtils() {
  }
}
