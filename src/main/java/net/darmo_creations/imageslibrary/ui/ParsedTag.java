package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents a tag that was parsed from a text field.
 *
 * @param tagType The tag’s optional type.
 * @param label   The tag’s label.
 */
public record ParsedTag(@NotNull Optional<TagType> tagType, @NotNull String label) {
  public ParsedTag {
    Objects.requireNonNull(tagType);
    Objects.requireNonNull(label);
  }
}
