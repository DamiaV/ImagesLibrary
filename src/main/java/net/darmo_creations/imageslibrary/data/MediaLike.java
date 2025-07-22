package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * Base interface for classes representing medias.
 */
public interface MediaLike extends DatabaseElement, Comparable<MediaLike> {
  /**
   * This media’s path.
   */
  Path path();

  /**
   * This media’s hash.
   */
  Optional<Hash> hash();

  /**
   * Indicate whether this media is a video file.
   *
   * @return True if the path’s extension is contained in {@link App#VALID_VIDEO_EXTENSIONS}, false otherwise.
   */
  default boolean isVideo() {
    return App.VALID_VIDEO_EXTENSIONS.contains(FileUtils.getExtension(this.path()).toLowerCase());
  }

  @Override
  default int compareTo(@NotNull MediaLike o) {
    return this.path().compareTo(o.path());
  }
}
