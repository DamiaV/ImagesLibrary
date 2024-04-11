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
   * Raise an exception if the given tag or tag type label is invalid.
   * A label is considered invalid if it contains a character that is not:
   * <li>a Unicode letter;</li>
   * <li>a Unicode number;</li>
   * <li>an underscore '_'.</li>
   *
   * @param label The label to check.
   * @throws IllegalArgumentException If the label is invalid.
   */
  static void ensureValidLabel(String label) {
    if (!label.matches("[\\p{IsL}\\p{IsN}_]+"))
      throw new IllegalArgumentException("Invalid label: " + label);
  }
}
