package net.darmo_creations.imageslibrary.data;

/**
 * Base interface for classes representing tag types.
 */
public interface TagTypeLike extends DatabaseElement {
  /**
   * This tag type’s label.
   */
  String label();

  /**
   * This tag type’s symbol.
   */
  char symbol();

  /**
   * This tag type’s RGB color.
   */
  int color();
}
