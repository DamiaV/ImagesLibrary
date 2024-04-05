package net.darmo_creations.imageslibrary.io;

import org.jetbrains.annotations.*;

/**
 * This class indicates that the file being moved has to be skipped.
 */
public final class Skip implements FileNameConflictResolution {
  private static final Skip INSTANCE = new Skip();

  /**
   * Get the singleton instance of this class.
   */
  @Contract(pure = true)
  static Skip instance() {
    return INSTANCE;
  }

  private Skip() {
  }
}
