package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

@FunctionalInterface
public interface EditTagsTypeListener {
  /**
   * Called when a set of tags should have their type set to the given one.
   *
   * @param tags          The tags to update.
   * @param targetTagType The type they should have.
   */
  void onEditTagsType(@NotNull List<Tag> tags, TagType targetTagType);
}
