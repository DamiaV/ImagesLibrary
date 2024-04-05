package net.darmo_creations.imageslibrary.io;

import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * This class describes the changes that occured after moving a file
 * using the {@link FilesManager#moveFile(Path, Path, FilesManager.FileNameConflictResolutionProvider)} method.
 */
public final class FileMovingOutcome {
  private final Path oldPath;
  private final Path newPath;
  private final @Nullable FileNameConflictResolution resolution;

  FileMovingOutcome(
      Path oldPath,
      Path newPath,
      @Nullable FileNameConflictResolution resolution
  ) {
    this.oldPath = oldPath.toAbsolutePath();
    this.newPath = newPath.toAbsolutePath();
    this.resolution = resolution;
  }

  /**
   * The path of the source file before being moved.
   */
  public Path oldPath() {
    return this.oldPath;
  }

  /**
   * The path of the source file after being moved/renamed.
   * May be the same as {@link #oldPath()} if the file was not moved/renamed.
   */
  public Path newPath() {
    return this.newPath;
  }

  /**
   * Get whether the file has been moved.
   *
   * @return True if the old and new paths are different, false otherwise.
   */
  public boolean hasFileMoved() {
    return !this.oldPath.equals(this.newPath);
  }

  /**
   * If there was a name conflict, the way the name conflict was resolved.
   */
  public Optional<FileNameConflictResolution> resolution() {
    return Optional.ofNullable(this.resolution);
  }
}
