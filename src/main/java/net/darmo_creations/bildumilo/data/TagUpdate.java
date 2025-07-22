package net.darmo_creations.bildumilo.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class indicates how to update a tag in the database.
 */
public final class TagUpdate implements TagLike {
  private final int id;
  private final String label;
  @Nullable
  private final TagType type;
  @Nullable
  private final String definition;

  public TagUpdate(int id, @NotNull String label, TagType type, String definition) {
    this.id = id;
    try {
      TagLike.ensureValidLabel(label);
    } catch (final TagParseException e) {
      throw new RuntimeException(e);
    }
    this.label = label;
    this.type = type;
    this.definition = definition;
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

  @Override
  public int id() {
    return this.id;
  }

  @Override
  public String label() {
    return this.label;
  }

  @Override
  public Optional<TagType> type() {
    return Optional.ofNullable(this.type);
  }

  @Override
  public Optional<String> definition() {
    return Optional.ofNullable(this.definition);
  }
}
