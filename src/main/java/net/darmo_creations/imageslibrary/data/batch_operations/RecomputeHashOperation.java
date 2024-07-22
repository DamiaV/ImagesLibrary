package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * An operation that computes the hash of a {@link Picture}.
 */
public final class RecomputeHashOperation extends Operation {
  public static final String KEY = "recompute_hash";

  /**
   * Create a new operation that computes the hash of {@link Picture}s.
   *
   * @param condition An optional condition.
   */
  public RecomputeHashOperation(Condition condition) {
    super(condition);
  }

  @Override
  protected void execute(@NotNull Picture picture, @NotNull DatabaseConnection db) throws DatabaseOperationException {
    final Hash hash;
    try {
      hash = Hash.computeForFile(picture.path());
    } catch (final IOException | SecurityException e) {
      throw new DatabaseOperationException(DatabaseConnection.getErrorCode(e), e);
    }
    db.updatePicture(new PictureUpdate(picture.id(), picture.path(), hash, Set.of(), Set.of()));
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return "";
  }
}
