package net.darmo_creations.imageslibrary.data;

/**
 * This class represents a single database table row.
 */
public abstract sealed class DatabaseObject
    permits Picture, Tag, TagType {
  private final int id;

  /**
   * Create a new object for the given ID.
   *
   * @param id The ID of the object.
   */
  protected DatabaseObject(int id) {
    this.id = id;
  }

  /**
   * This object’s database ID.
   */
  public int id() {
    return this.id;
  }
}
