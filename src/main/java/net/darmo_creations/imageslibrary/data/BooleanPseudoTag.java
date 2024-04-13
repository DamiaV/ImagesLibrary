package net.darmo_creations.imageslibrary.data;

import java.util.*;

/**
 * A pseudo-tag is an element of a tag query that is used to filter images using a specified pattern.
 *
 * @param sqlTemplate The SQL query template for this pseudo-tag.
 */
public record BooleanPseudoTag(@SQLite String sqlTemplate) implements PseudoTag {
  public BooleanPseudoTag {
    Objects.requireNonNull(sqlTemplate);
  }
}
