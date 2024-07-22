package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

/**
 * An operation that deletes a {@link Picture} and optionally its associated file.
 */
public final class DeleteOperation extends Operation {
  public static final String KEY = "delete";

  private final boolean fromDisk;

  /**
   * Create a new operation that deletes {@link Picture}s.
   *
   * @param fromDisk  If true, the files of each {@link Picture} will be deleted.
   * @param condition An optional condition.
   */
  public DeleteOperation(boolean fromDisk, Condition condition) {
    super(condition);
    this.fromDisk = fromDisk;
  }

  @Override
  protected void execute(@NotNull Picture picture, @NotNull DatabaseConnection db) throws DatabaseOperationException {
    db.deletePicture(picture, this.fromDisk);
  }

  public boolean deleteFromDisk() {
    return this.fromDisk;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return this.fromDisk ? "1" : "0";
  }

  /**
   * Deserialize a string into a new {@link DeleteOperation} object.
   *
   * @param serialized The string to deserialize.
   * @param condition  An optional condition.
   * @return A new {@link DeleteOperation} object.
   */
  @Contract(pure = true, value = "_, _ -> new")
  public static DeleteOperation deserialize(@NotNull String serialized, Condition condition) {
    return new DeleteOperation(!serialized.equals("0"), condition);
  }
}
