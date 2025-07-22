package net.darmo_creations.imageslibrary.data.batch_operations;

import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class represents an operation that can be applied to a {@link MediaFile}.
 * <p>
 * {@link Operation}s may feature a {@link Condition} that restricts which {@link MediaFile}s it should be applied to.
 */
public abstract sealed class Operation implements StringSerializable
    permits UpdateTagsOperation, MoveOperation, DeleteOperation, RecomputeHashOperation, TransformPathOperation {
  private final Condition condition;

  /**
   * Create a new operation.
   *
   * @param condition An optional condition to restrict which medias this operation may be applied to.
   *                  If null, all medias will be eligible.
   */
  protected Operation(Condition condition) {
    this.condition = condition;
  }

  /**
   * Apply this operation to the given media.
   * <p>
   * If the passed condition evaluates to false on the given media, no operation will be applied.
   *
   * @param mediaFile The media to apply this operation to.
   * @param db        A database to apply changes to.
   * @param config    The app’s config.
   * @return A pair containing a boolean indicating whether this operation was applied to the given media or not,
   * and a {@link MediaFile} object containing the updated data of the passed media.
   * @throws DatabaseOperationException If any database error occurs.
   */
  public final Pair<Boolean, MediaFile> apply(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db, @NotNull Config config)
      throws DatabaseOperationException {
    boolean apply = this.condition == null || this.condition.test(mediaFile, db, config);
    if (apply) {
      final var result = this.execute(mediaFile, db);
      apply = result.getKey();
      mediaFile = result.getValue();
    }
    return new Pair<>(apply, mediaFile);
  }

  /**
   * Apply this operation to the given media.
   *
   * @param mediaFile The media to apply this operation to.
   * @param db        A database to apply changes to.
   * @return A pair containing a boolean indicating whether this operation was applied to the given media or not,
   * and a {@link MediaFile} object containing the updated data of the passed media.
   * @throws DatabaseOperationException If any database error occurs.
   */
  protected abstract Pair<Boolean, MediaFile> execute(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db)
      throws DatabaseOperationException;

  /**
   * This operation’s condition.
   */
  public final Optional<Condition> condition() {
    return Optional.ofNullable(this.condition);
  }
}
