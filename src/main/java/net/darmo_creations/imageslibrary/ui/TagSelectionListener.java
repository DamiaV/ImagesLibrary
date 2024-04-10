package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;

import java.util.*;

@FunctionalInterface
public interface TagSelectionListener {
  /**
   * Called when the tag list selection changes or regains focus.
   *
   * @param tags The selected tags.
   */
  void onSelectionChanged(List<Tag> tags);
}
