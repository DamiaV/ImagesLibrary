package net.darmo_creations.bildumilo.data.batch_operations;

import javafx.util.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

/**
 * An operation that deletes a {@link MediaFile} and optionally its associated file.
 */
public final class DeleteOperation extends Operation {
  public static final String KEY = "delete";

  private final boolean fromDisk;
  private final boolean deleteEmptySourceDirectory;

  /**
   * Create a new operation that deletes {@link MediaFile}s.
   *
   * @param fromDisk                   If true, the files of each {@link MediaFile} will be deleted.
   * @param deleteEmptySourceDirectory If true, delete source directories that end up empty after each move.
   * @param condition                  An optional condition.
   */
  public DeleteOperation(boolean fromDisk, boolean deleteEmptySourceDirectory, Condition condition) {
    super(condition);
    this.fromDisk = fromDisk;
    this.deleteEmptySourceDirectory = deleteEmptySourceDirectory;
  }

  @Override
  protected Pair<Boolean, MediaFile> execute(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db)
      throws DatabaseOperationException {
    db.deleteMedia(mediaFile, this.fromDisk);
    if (this.fromDisk && this.deleteEmptySourceDirectory)
      FileUtils.deleteDirectoryIfEmpty(mediaFile);
    return new Pair<>(true, mediaFile);
  }

  public boolean deleteFromDisk() {
    return this.fromDisk;
  }

  public boolean deleteEmptySourceDirectory() {
    return this.deleteEmptySourceDirectory;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return "%s,%s".formatted(
        this.fromDisk ? "1" : "0",
        this.deleteEmptySourceDirectory ? "1" : "0"
    );
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
    final String[] parts = serialized.split(",", 2);
    if (parts.length != 2)
      throw new IllegalArgumentException("Invalid serialized data");
    return new DeleteOperation(
        !parts[0].equals("0"),
        !parts[1].equals("0"),
        condition
    );
  }
}
