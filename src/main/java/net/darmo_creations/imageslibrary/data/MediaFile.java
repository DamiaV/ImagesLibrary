package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * This class represents a media file. {@link MediaFile} objects hold the path to a file on the disk and a dHash of this file.
 * The hash is used to check which images are similar.
 */
public final class MediaFile extends DatabaseObject implements MediaLike {
  private final Path path;
  @Nullable
  private final Hash hash;

  /**
   * Create a new media file.
   *
   * @param id   The file’s database ID.
   * @param path The path to the file.
   * @param hash The hash of the file.
   */
  public MediaFile(int id, final @NotNull Path path, Hash hash) {
    super(id);
    this.path = path.toAbsolutePath();
    this.hash = hash;
  }

  /**
   * The absolute path to this media file.
   */
  @Override
  public Path path() {
    return this.path;
  }

  /**
   * The hash of this media file. Only available for images.
   */
  @Override
  public Optional<Hash> hash() {
    return Optional.ofNullable(this.hash);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || this.getClass() != o.getClass()) return false;
    final MediaFile that = (MediaFile) o;
    return this.id() == that.id()
        && Objects.equals(this.path, that.path)
        && Objects.equals(this.hash, that.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id(), this.path, this.hash);
  }

  @Override
  public String toString() {
    return "MediaFile{id=%d, path=%s, hash=%s}"
        .formatted(this.id(), this.path, this.hash);
  }
}
