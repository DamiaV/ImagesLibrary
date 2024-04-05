package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
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
    Path path,
    Hash hash,
    Set<Pair<@Nullable TagType, String>> tagsToAdd,
    Set<Tag> tagsToRemove
) {
  public PictureUpdate {
    Objects.requireNonNull(path);
    Objects.requireNonNull(hash);
    Objects.requireNonNull(tagsToAdd);
    Objects.requireNonNull(tagsToRemove);
  }
}
