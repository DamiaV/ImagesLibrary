package net.darmo_creations.bildumilo.ui;

import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

@FunctionalInterface
public interface DeleteTagTypeListener {
  /**
   * Called when a tag type is to be deleted.
   *
   * @param tagType The tag type to delete.
   */
  void onDeleteTagType(@NotNull TagType tagType);
}
