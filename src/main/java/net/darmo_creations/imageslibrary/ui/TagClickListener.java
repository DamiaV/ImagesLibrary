package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;

@FunctionalInterface
public interface TagClickListener {
  /**
   * Called when a tag is double-clicked.
   *
   * @param tag The clicked tag.
   */
  void onTagClick(Tag tag);
}
