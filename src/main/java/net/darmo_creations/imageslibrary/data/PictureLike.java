package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * Base interface for classes representing pictures.
 */
public interface PictureLike extends DatabaseElement, Comparable<PictureLike> {
  /**
   * This picture’s path.
   */
  Path path();

  /**
   * This picture’s hash.
   */
  Optional<Hash> hash();

  /**
   * Indicate whether this object represents a video file.
   *
   * @return True if the path’s extension is contained in {@link App#VALID_VIDEO_EXTENSIONS}, false otherwise.
   */
  default boolean isVideo() {
    return App.VALID_VIDEO_EXTENSIONS.contains(FileUtils.getExtension(this.path()).toLowerCase());
  }

  @Override
  default int compareTo(@NotNull PictureLike o) {
    return this.path().compareTo(o.path());
  }
}
