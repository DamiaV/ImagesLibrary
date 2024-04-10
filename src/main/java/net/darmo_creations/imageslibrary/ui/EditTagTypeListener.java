package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;

@FunctionalInterface
public interface EditTagTypeListener {
  /**
   * Called when a tag type is to be edited.
   *
   * @param tagType The tag type to edit.
   */
  void onEditTagType(TagType tagType);
}
