package net.darmo_creations.bildumilo.ui;

import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

@FunctionalInterface
public interface EditTagTypeListener {
  /**
   * Called when a tag type is to be edited.
   *
   * @param tagType The tag type to edit.
   */
  void onEditTagType(@NotNull TagType tagType);
}
