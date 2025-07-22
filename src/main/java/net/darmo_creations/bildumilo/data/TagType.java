package net.darmo_creations.bildumilo.data;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents the type of a tag.
 */
public final class TagType extends DatabaseObject implements TagTypeLike {
  private String label;
  private char symbol;
  private int color;

  /**
   * Create a new tag type.
   *
   * @param id     The tag type’s database ID.
   * @param label  The tag type’s label.
   * @param symbol The tag type’s symbol.
   * @param color  The tag type’s color.
   * @throws IllegalArgumentException If the symbol is invalid.
   */
  TagType(int id, @NotNull String label, char symbol, int color) {
    super(id);
    this.setLabel(label);
    this.setSymbol(symbol);
    this.setColor(color);
  }

  /**
   * This tag type’s label.
   */
  @Override
  public String label() {
    return this.label;
  }

  /**
   * Set this tag type’s label.
   *
   * @param label The new label.
   * @throws IllegalArgumentException If the label is invalid.
   */
  void setLabel(@NotNull String label) {
    TagTypeLike.ensureValidLabel(label);
    this.label = label;
  }

  /**
   * This tag type’s symbol.
   */
  @Override
  public char symbol() {
    return this.symbol;
  }

  /**
   * Set this tag type’s symbol.
   *
   * @param symbol The new symbol.
   * @throws IllegalArgumentException If the symbol is invalid.
   */
  void setSymbol(char symbol) {
    TagTypeLike.ensureValidSymbol(symbol);
    this.symbol = symbol;
  }

  /**
   * This tag type’s color.
   */
  @Override
  public int color() {
    return this.color;
  }

  /**
   * Set this tag type’s color.
   *
   * @param color The new color.
   */
  void setColor(int color) {
    this.color = color;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || this.getClass() != o.getClass()) return false;
    final TagType tagType = (TagType) o;
    return this.id() == tagType.id()
           && this.symbol == tagType.symbol
           && this.color == tagType.color
           && Objects.equals(this.label, tagType.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id(), this.label, this.symbol, this.color);
  }

  @Override
  public String toString() {
    return "TagType{id=%d, label='%s', symbol=%s, color=%d}"
        .formatted(this.id(), this.label, this.symbol, this.color);
  }
}
