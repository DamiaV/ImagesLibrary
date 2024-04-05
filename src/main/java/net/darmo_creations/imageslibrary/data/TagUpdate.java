package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class indicates how to update a tag in the database.
 *
 * @param id         The ID of the tag to update.
 * @param label      The tag’s new label.
 * @param type       The tag’s new type. May be null.
 * @param definition The tag’s new definition. May be null.
 */
public record TagUpdate(
    int id,
    String label,
    @Nullable TagType type,
    @Nullable String definition
) {
  public TagUpdate {
    Objects.requireNonNull(label);
  }

  /**
   * Return a new {@link TagUpdate} with the same field values as this one, but with the given ID.
   *
   * @param id The ID to replace this one’s with.
   * @return A new {@link TagUpdate} object if the IDs are different, this object if they are identical.
   */
  @Contract(pure = true)
  public TagUpdate withId(int id) {
    if (id == this.id())
      return this;
    return new TagUpdate(id, this.label, this.type, this.definition);
  }
}
