package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
import org.jetbrains.annotations.*;

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
  static void ensureValidLabel(@NotNull String label) {
    if (!isLabelValid(label))
      throw new IllegalArgumentException("Invalid label: " + label);
  }

  /**
   * Check whether the given tag label is valid.
   *
   * @param label The label to check.
   * @return True if the label is valid, false otherwise.
   */
  static boolean isLabelValid(@NotNull String label) {
    return label.matches("[\\p{IsL}\\p{IsN}_]+");
  }
}
