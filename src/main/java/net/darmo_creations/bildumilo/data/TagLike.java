package net.darmo_creations.bildumilo.data;

import net.darmo_creations.bildumilo.utils.*;
import org.intellij.lang.annotations.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base interface for classes representing media tags.
 */
public interface TagLike extends DatabaseElement, Comparable<TagLike> {
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

  @Override
  default int compareTo(@NotNull TagLike o) {
    return this.label().compareToIgnoreCase(o.label());
  }

  /**
   * Raise an exception if the given tag label is invalid.
   * A label is considered invalid if it contains a character
   * that is not in any of the L or N Unicode General Categories
   * or is not an underscore '_'.
   *
   * @param label The label to check.
   * @throws TagParseException If the label is invalid.
   */
  static void ensureValidLabel(@NotNull String label) throws TagParseException {
    if (label.isEmpty())
      throw new TagParseException("empty_tag_label");
    if (!isLabelValid(label))
      throw new TagParseException("invalid_tag_label", new FormatArg("label", label));
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
   * @throws TagParseException If the label is invalid.
   */
  static TagParts splitLabel(@NotNull String tag) throws TagParseException {
    final char firstChar = tag.charAt(0);
    if (TagTypeLike.isSymbolValid(firstChar)) {
      final String tagLabel = tag.substring(1);
      ensureValidLabel(tagLabel);
      return new TagParts(Optional.of(firstChar), tagLabel);
    }
    ensureValidLabel(tag);
    return new TagParts(Optional.empty(), tag);
  }

  record TagParts(@NotNull Optional<Character> typeSymbol, @NotNull String label) {
    public TagParts {
      Objects.requireNonNull(typeSymbol);
      Objects.requireNonNull(label);
    }
  }
}
