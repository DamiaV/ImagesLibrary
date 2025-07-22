package net.darmo_creations.imageslibrary.data;

/**
 * A pseudo-tag is an element of a tag query that is used to filter medias based on their associated SQL query.
 */
public interface PseudoTag {
  /**
   * The SQL query template for this pseudo-tag.
   */
  String sqlTemplate();
}
