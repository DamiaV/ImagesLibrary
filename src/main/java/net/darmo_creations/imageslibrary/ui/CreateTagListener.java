package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;

@FunctionalInterface
public interface CreateTagListener {
  /**
   * Called when a tag is to be created.
   *
   * @param tagType The type of the new tag. May be null.
   */
  void onCreateTag(TagType tagType);
}
