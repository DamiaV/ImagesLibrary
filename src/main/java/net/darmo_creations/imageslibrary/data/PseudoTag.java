package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A pseudo-tag is an element of a tag query that is used to filter images using a specified pattern.
 *
 * @param sqlTemplate  The SQL query template for this pseudo-tag.
 * @param acceptsRegex If true, this pseudo-tag’s pattern may be a regex
 *                     and string literals interpret the "*" and "?" characters as wildcards;
 *                     otherwise, this pseudo-tag may only accept string literals.
 */
public record PseudoTag(
    @SQLite String sqlTemplate,
    boolean acceptsRegex
) {
  @Unmodifiable
  public static final List<String> FLAGS = List.of("i", "s");

  public PseudoTag {
    Objects.requireNonNull(sqlTemplate);
  }
}
