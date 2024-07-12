package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
import org.intellij.lang.annotations.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base interface for classes representing picture tags.
 */
public interface TagLike extends DatabaseElement {
  @Language("RegExp")
  String LABEL_PATTERN = "[\\p{IsL}\\p{IsN}_]+";
  @Language("RegExp")
  String NOT_LABEL_PATTERN = "[^\\p{IsL}\\p{IsN}_]";

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
    return label.matches(LABEL_PATTERN);
  }

  /**
   * Extract the tag type and label from the given string.
   *
   * @param tag The string to split.
   * @return A pair containing the tag type symbol and the tag label.
   * @throws IllegalArgumentException If the label is invalid.
   */
  static Pair<Optional<Character>, String> splitLabel(@NotNull String tag) {
    if (TagTypeLike.isSymbolValid(tag.charAt(0))) {
      ensureValidLabel(tag.substring(1));
      return new Pair<>(Optional.of(tag.charAt(0)), tag.substring(1));
    }
    ensureValidLabel(tag);
    return new Pair<>(Optional.empty(), tag);
  }
}
