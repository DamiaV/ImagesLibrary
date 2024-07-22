package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.query_parser.*;
import org.jetbrains.annotations.*;

import java.sql.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

/**
 * A pseudo-tag that accepts a string or regex as its only argument.
 * Flags may be prepended to the argument.
 *
 * @param sqlTemplate      The SQL query template for this pseudo-tag.
 * @param predicateFactory A factory that builds a {@link TagQueryPredicate} for the given Regex pattern and flags.
 * @param acceptsRegex     If true, this pseudo-tag’s pattern may be a regex
 *                         and string literals should interpret the "*" and "?" characters as wildcards;
 *                         otherwise, this pseudo-tag may only accept plain string literals.
 */
public record PatternPseudoTag(
    @SQLite @NotNull String sqlTemplate,
    @NotNull BiFunction<String, String, TagQueryPredicate> predicateFactory,
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
    Objects.requireNonNull(predicateFactory);
  }

  /**
   * Build a {@link Pattern} object from a Regex string and pseudo-tag flags.
   *
   * @param pattern A Java-compatible Regex string.
   * @param flags   {@link PatternPseudoTag} flags.
   * @return A new {@link Pattern} object.
   * @throws SQLException If flags are invalid.
   */
  @Contract(pure = true, value = "_, _ -> new")
  public static Pattern getPattern(@NotNull String pattern, String flags) throws SQLException {
    if (flags == null || !flags.contains(String.valueOf(CASE_SENSITIVE_FLAG))
                         && !flags.contains(String.valueOf(CASE_INSENSITIVE_FLAG)))
      throw new SQLException("Missing case sensitivity flag");
    if (flags.contains(String.valueOf(CASE_SENSITIVE_FLAG))
        && flags.contains(String.valueOf(CASE_INSENSITIVE_FLAG)))
      throw new SQLException("Both case sensitivity flags present");
    boolean caseSensitive = true;
    for (int i = 0; i < flags.length(); i++) {
      final char c = flags.charAt(i);
      switch (c) {
        case CASE_SENSITIVE_FLAG -> caseSensitive = true;
        case CASE_INSENSITIVE_FLAG -> caseSensitive = false;
        default -> throw new SQLException("Invalid regex flag: " + c);
      }
    }
    return Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
  }
}
