package net.darmo_creations.imageslibrary.data;

import java.nio.file.*;
import java.util.*;

/**
 * Base interface for classes representing pictures.
 */
public interface PictureLike extends DatabaseElement {
  /**
   * This picture’s path.
   */
  Path path();

  /**
   * This picture’s hash.
   */
  Optional<Hash> hash();
}
