package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A flag is an element of a tag query that is used to filter images based on a boolean property.
 *
 * @param sqlTemplate The SQL query template for this pseudo-tag.
 */
public record BooleanFlag(@SQLite @NotNull String sqlTemplate) implements PseudoTag {
  public BooleanFlag {
    Objects.requireNonNull(sqlTemplate);
  }
}
