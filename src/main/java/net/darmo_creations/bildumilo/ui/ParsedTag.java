package net.darmo_creations.bildumilo.ui;

import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents a tag that was parsed from a text field.
 *
 * @param tagType The tag’s optional type.
 * @param label   The tag’s label.
 */
public record ParsedTag(@NotNull Optional<TagType> tagType, @NotNull String label) implements Comparable<ParsedTag> {
  public ParsedTag {
    Objects.requireNonNull(tagType);
    Objects.requireNonNull(label);
  }

  @Override
  public int compareTo(@NotNull ParsedTag o) {
    return this.label().compareToIgnoreCase(o.label());
  }
}
