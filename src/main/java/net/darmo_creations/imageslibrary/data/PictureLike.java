package net.darmo_creations.imageslibrary.data;

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

  @Override
  default int compareTo(@NotNull PictureLike o) {
    return this.path().compareTo(o.path());
  }
}
