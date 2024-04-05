package net.darmo_creations.imageslibrary.io;

import org.jetbrains.annotations.*;

/**
 * This class offers static methods to create {@link FileNameConflictResolution} objects.
 */
public final class FileNameConflictResolutionFactory {
  /**
   * Create a resolution that indicates the source file has to be skipped.
   *
   * @return A new {@link FileNameConflictResolution} object.
   */
  @Contract(pure = true)
  public static FileNameConflictResolution skip() {
    return Skip.instance();
  }

  /**
   * Create a resolution that indicates the source file has to be renamed.
   *
   * @param name The new name of the source file.
   * @return A new {@link FileNameConflictResolution} object.
   */
  @Contract(pure = true, value = "_ -> new")
  public static FileNameConflictResolution renameSourceTo(String name) {
    return new Rename(name);
  }

  /**
   * Create a resolution that indicates the target file has to be renamed.
   *
   * @param overwrittenTagsHandling The way the tags of the target file should be handled.
   * @return A new {@link FileNameConflictResolution} object.
   */
  @Contract(pure = true, value = "_ -> new")
  public static FileNameConflictResolution overwriteTarget(OverwrittenTagsHandling overwrittenTagsHandling) {
    return new Overwrite(overwrittenTagsHandling);
  }

  private FileNameConflictResolutionFactory() {
  }
}
