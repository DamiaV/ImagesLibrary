package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A pseudo-tag that accepts a string or regex as its only argument.
 * Flags may be prepended to the argument.
 *
 * @param sqlTemplate  The SQL query template for this pseudo-tag.
 * @param acceptsRegex If true, this pseudo-tag’s pattern may be a regex
 *                     and string literals interpret the "*" and "?" characters as wildcards;
 *                     otherwise, this pseudo-tag may only accept string literals.
 */
public record PatternPseudoTag(
    @SQLite @NotNull String sqlTemplate,
    boolean acceptsRegex
) implements PseudoTag {
  public static final char CASE_SENSITIVE_FLAG = 's';
  public static final char CASE_INSENSITIVE_FLAG = 'i';
  public static final List<Character> FLAGS = List.of(
      CASE_SENSITIVE_FLAG,
      CASE_INSENSITIVE_FLAG
  );

  public PatternPseudoTag {
    Objects.requireNonNull(sqlTemplate);
  }
}
