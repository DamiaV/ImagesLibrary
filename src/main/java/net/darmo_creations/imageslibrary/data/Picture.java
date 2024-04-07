package net.darmo_creations.imageslibrary.data;

import java.nio.file.*;
import java.util.*;

/**
 * This class represents a picture file. Pictures hold the path to a file on the disk and a dHash of this file.
 * The hash is used to check which pictures are similar.
 */
public final class Picture extends DatabaseObject implements PictureLike {
  private final Path path;
  private final Hash hash;

  /**
   * Create a new picture.
   *
   * @param id   The picture’s database ID.
   * @param path The path to the picture’s file.
   * @param hash The hash of the file.
   */
  Picture(int id, final Path path, Hash hash) {
    super(id);
    this.path = Objects.requireNonNull(path.toAbsolutePath());
    this.hash = Objects.requireNonNull(hash);
  }

  /**
   * The absolute path to this picture’s file.
   */
  @Override
  public Path path() {
    return this.path;
  }

  /**
   * The hash of this picture’s file.
   */
  @Override
  public Hash hash() {
    return this.hash;
  }
}
