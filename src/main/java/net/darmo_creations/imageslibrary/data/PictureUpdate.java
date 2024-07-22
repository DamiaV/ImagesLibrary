package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.ui.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * This class indicates how to update a picture in the database.
 *
 * @param id           The ID of the picture to update.
 * @param path         The picture’s new file location.
 * @param hash         The picture’s new hash.
 * @param tagsToAdd    The tags to add to this picture. Each entry associates a tag type to a tag name.
 * @param tagsToRemove The tags to remove from this picture. Each entry associates a tag type to a tag name.
 */
public record PictureUpdate(
    int id,
    @NotNull Path path,
    Optional<Hash> hash,
    @NotNull Set<ParsedTag> tagsToAdd,
    @NotNull Set<Tag> tagsToRemove
) implements PictureLike {
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public PictureUpdate(
      int id,
      @NotNull Path path,
      Optional<Hash> hash,
      @NotNull Set<ParsedTag> tagsToAdd,
      @NotNull Set<Tag> tagsToRemove
  ) {
    this.id = id;
    this.path = path.toAbsolutePath();
    this.hash = Objects.requireNonNull(hash);
    this.tagsToAdd = Objects.requireNonNull(tagsToAdd);
    this.tagsToRemove = Objects.requireNonNull(tagsToRemove);
  }

  /**
   * Return a new {@link PictureUpdate} with the same field values as this one, but with the given ID.
   *
   * @param id The ID to replace this one’s with.
   * @return A new {@link PictureUpdate} object if the IDs are different, this object if they are identical.
   */
  @Contract(pure = true)
  public PictureUpdate withId(int id) {
    if (id == this.id())
      return this;
    return new PictureUpdate(
        id,
        this.path,
        this.hash,
        this.tagsToAdd,
        this.tagsToRemove
    );
  }
}
