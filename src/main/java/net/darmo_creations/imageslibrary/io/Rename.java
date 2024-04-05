package net.darmo_creations.imageslibrary.io;

import java.util.*;

/**
 * This class indicates the the file being moved has to be renamed.
 *
 * @param newName The moved file’s new name.
 */
public record Rename(String newName) implements FileNameConflictResolution {
  public Rename {
    Objects.requireNonNull(newName);
  }
}
