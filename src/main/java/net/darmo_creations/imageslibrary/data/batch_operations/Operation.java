package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents an operation that can be applied to a {@link Picture}.
 * <p>
 * {@link Operation}s may feature a {@link Condition} that restricts which {@link Picture}s it should be applied to.
 */
public abstract sealed class Operation implements StringSerializable
    permits UpdateTagsOperation, MoveOperation, DeleteOperation, RecomputeHashOperation, TransformPathOperation {
  private final Condition condition;

  /**
   * Create a new operation.
   *
   * @param condition An optional condition to restrict which pictures this operation may be applied to.
   *                  If null, all pictures will be eligible.
   */
  protected Operation(Condition condition) {
    this.condition = condition;
  }

  /**
   * Apply this operation to the given picture.
   * <p>
   * If the passed condition evaluates to false on the given picture, no operation will be applied.
   *
   * @param picture The picture to apply this operation to.
   * @param db      A database to apply changes to.
   * @param config  The app’s config.
   * @return True if this operation was applied to the given picture, false otherwise.
   * @throws DatabaseOperationException If any database error occurs.
   */
  public final boolean apply(@NotNull Picture picture, @NotNull DatabaseConnection db, @NotNull Config config)
      throws DatabaseOperationException {
    boolean apply = this.condition == null || this.condition.test(picture, db, config);
    if (apply)
      apply = this.execute(picture, db);
    return apply;
  }

  /**
   * Apply this operation to the given picture.
   *
   * @param picture The picture to apply this operation to.
   * @param db      A database to apply changes to.
   * @return True if this operation updated the given picture, false otherwise.
   * @throws DatabaseOperationException If any database error occurs.
   */
  protected abstract boolean execute(@NotNull Picture picture, @NotNull DatabaseConnection db)
      throws DatabaseOperationException;

  /**
   * This operation’s condition.
   */
  public final Optional<Condition> condition() {
    return Optional.ofNullable(this.condition);
  }
}
