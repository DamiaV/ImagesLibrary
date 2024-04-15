package net.darmo_creations.imageslibrary.data;

import java.util.*;

/**
 * Base interface for classes representing picture tags.
 */
public interface TagLike extends DatabaseElement {
  /**
   * This tag’s label.
   */
  String label();

  /**
   * This tag’s type.
   */
  Optional<TagType> type();

  /**
   * This tag’s definition.
   */
  Optional<String> definition();

  /**
   * Raise an exception if the given tag label is invalid.
   * A label is considered invalid if it contains a character
   * that is not in any of the L or N Unicode General Categories
   * or is not an underscore '_'.
   *
   * @param label The label to check.
   * @throws IllegalArgumentException If the label is invalid.
   */
  static void ensureValidLabel(String label) {
    if (!label.matches("[\\p{IsL}\\p{IsN}_]+"))
      throw new IllegalArgumentException("Invalid label: " + label);
  }
}
