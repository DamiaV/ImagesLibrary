package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents an tag. Tags can be associated to images, unless their {@link #definition()} is not null.
 */
public final class Tag extends DatabaseObject implements TagLike {
  private String label;
  private TagType type;
  private String definition;

  /**
   * Create a new tag.
   *
   * @param id         The tag’s database ID.
   * @param label      The tag’s label.
   * @param type       The tag’s type. May be null.
   * @param definition The tag’s definition. May be null.
   */
  Tag(int id, String label, @Nullable TagType type, @Nullable String definition) {
    super(id);
    this.setLabel(label);
    this.setType(type);
    this.setDefinition(definition);
  }

  /**
   * This tag’s label.
   */
  @Override
  public String label() {
    return this.label;
  }

  /**
   * Set this tag’s label.
   *
   * @param label The new label.
   * @throws IllegalArgumentException If the label is invalid.
   */
  void setLabel(String label) {
    TagLike.ensureValidLabel(label);
    this.label = label;
  }

  /**
   * This tag’s type.
   */
  @Override
  public Optional<TagType> type() {
    return Optional.ofNullable(this.type);
  }

  /**
   * Set this tag’s type.
   *
   * @param type The new type. May be null.
   */
  void setType(@Nullable TagType type) {
    this.type = type;
  }

  /**
   * This tag’s definition.
   */
  @Override
  public Optional<String> definition() {
    return Optional.ofNullable(this.definition);
  }

  /**
   * Set this tag’s definition. The new definition’s syntax will not be checked,
   * it is the responsability of the caller to do so.
   *
   * @param definition The new definition. May be null.
   */
  void setDefinition(@Nullable String definition) {
    this.definition = definition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || this.getClass() != o.getClass()) return false;
    Tag tag = (Tag) o;
    return this.id() == tag.id()
           && Objects.equals(this.label, tag.label)
           && Objects.equals(this.type, tag.type)
           && Objects.equals(this.definition, tag.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id(), this.label, this.type, this.definition);
  }

  @Override
  public String toString() {
    return "Tag{id=%d, label='%s', type=%s, definition=%s}"
        .formatted(this.id(), this.label, this.type, this.definition);
  }
}
