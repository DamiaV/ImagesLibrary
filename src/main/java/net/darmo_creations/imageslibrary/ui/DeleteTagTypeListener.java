package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;

public interface DeleteTagTypeListener {
  /**
   * Called when a tag type is to be deleted.
   *
   * @param tagType The tag type to delete.
   */
  void onDeleteTagType(TagType tagType);
}
