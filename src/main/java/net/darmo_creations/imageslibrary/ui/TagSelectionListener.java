package net.darmo_creations.imageslibrary.ui;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

@FunctionalInterface
public interface TagSelectionListener {
  /**
   * Called when the tag list selection changes or regains focus.
   *
   * @param tags The selected tags.
   */
  void onSelectionChanged(@NotNull List<Tag> tags);
}
