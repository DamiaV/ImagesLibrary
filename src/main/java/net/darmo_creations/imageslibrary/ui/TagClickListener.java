package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

@FunctionalInterface
public interface TagClickListener {
  /**
   * Called when a tag is double-clicked.
   *
   * @param tag The clicked tag.
   */
  void onTagClick(@NotNull Tag tag);
}
