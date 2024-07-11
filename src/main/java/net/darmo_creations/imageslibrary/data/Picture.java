package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

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
  public Picture(int id, final @NotNull Path path, @NotNull Hash hash) {
    super(id);
    this.path = path.toAbsolutePath();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || this.getClass() != o.getClass()) return false;
    final Picture picture = (Picture) o;
    return this.id() == picture.id()
           && Objects.equals(this.path, picture.path)
           && Objects.equals(this.hash, picture.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id(), this.path, this.hash);
  }

  @Override
  public String toString() {
    return "Picture{id=%d, path=%s, hash=%s}"
        .formatted(this.id(), this.path, this.hash);
  }
}
