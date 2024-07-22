package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

/**
 * A {@link Condition} checks whether a given {@link Picture} matches some predicate.
 */
public sealed interface Condition extends StringSerializable
    permits TagQueryCondition {
  /**
   * Check whether the given picture fulfills this condition.
   *
   * @param picture The picture to check.
   * @param db      The database to pull additional data from.
   * @param config  The app’s config.
   * @return True if the picture fulfills this condition, false otherwise.
   * @throws DatabaseOperationException If any database or file error occurs.
   */
  boolean test(@NotNull Picture picture, @NotNull DatabaseConnection db, @NotNull Config config)
      throws DatabaseOperationException;

  /**
   * Purge all internal caches.
   */
  default void purgeCaches() {
  }
}
