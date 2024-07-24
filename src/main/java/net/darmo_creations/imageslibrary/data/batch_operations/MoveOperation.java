package net.darmo_creations.imageslibrary.data.batch_operations;

import javafx.util.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * An operation that moves a {@link Picture} and its associated file.
 */
public final class MoveOperation extends Operation {
  public static final String KEY = "move";

  private final Path targetDirectory;
  private final boolean deleteEmptySourceDirectory;
  private final boolean overwriteTarget;

  /**
   * Create a new operation that moves {@link Picture}s and their associated files.
   *
   * @param targetDirectory            The directory where to move {@link Picture}s to.
   * @param deleteEmptySourceDirectory If true, delete source directories that end up empty after each move.
   * @param overwriteTarget            If true, overwrite target files that have conflicting names.
   * @param condition                  An optional condition.
   */
  public MoveOperation(
      @NotNull Path targetDirectory,
      boolean deleteEmptySourceDirectory,
      boolean overwriteTarget,
      Condition condition
  ) {
    super(condition);
    this.targetDirectory = Objects.requireNonNull(targetDirectory);
    this.deleteEmptySourceDirectory = deleteEmptySourceDirectory;
    this.overwriteTarget = overwriteTarget;
  }

  @Override
  protected Pair<Boolean, Picture> execute(@NotNull Picture picture, @NotNull DatabaseConnection db)
      throws DatabaseOperationException {
    final Path newPath = this.targetDirectory.resolve(picture.path().getFileName());
    final boolean updated = db.moveOrRenamePicture(
        picture,
        newPath,
        this.overwriteTarget
    );
    if (this.deleteEmptySourceDirectory)
      FileUtils.deleteDirectoryIfEmpty(picture);
    return new Pair<>(updated, new Picture(picture.id(), newPath, picture.hash().orElse(null)));
  }

  public Path targetPath() {
    return this.targetDirectory;
  }

  public boolean deleteEmptySourceDirectory() {
    return this.deleteEmptySourceDirectory;
  }

  public boolean overwriteTarget() {
    return this.overwriteTarget;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return "%s,%s,%s".formatted(
        this.deleteEmptySourceDirectory ? "1" : "0",
        this.overwriteTarget ? "1" : "0",
        this.targetDirectory
    );
  }

  /**
   * Deserialize a string into a new {@link MoveOperation} object.
   *
   * @param serialized The string to deserialize.
   * @param condition  An optional condition.
   * @return A new {@link MoveOperation} object.
   * @throws IllegalArgumentException If the string is not valid.
   */
  @Contract(pure = true, value = "_, _ -> new")
  public static MoveOperation deserialize(@NotNull String serialized, Condition condition) {
    final String[] parts = serialized.split(",", 3);
    if (parts.length != 3)
      throw new IllegalArgumentException("Invalid serialized data");
    return new MoveOperation(
        Path.of(parts[2]),
        !parts[0].equals("0"),
        !parts[1].equals("0"),
        condition
    );
  }
}
