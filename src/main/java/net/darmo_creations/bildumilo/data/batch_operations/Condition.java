package net.darmo_creations.bildumilo.data.batch_operations;

import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

/**
 * A {@link Condition} checks whether a given {@link MediaFile} matches some predicate.
 */
public sealed interface Condition extends StringSerializable
    permits TagQueryCondition {
  /**
   * Check whether the given media fulfills this condition.
   *
   * @param mediaFile The media to check.
   * @param db        The database to pull additional data from.
   * @param config    The app’s config.
   * @return True if the media fulfills this condition, false otherwise.
   * @throws DatabaseOperationException If any database or file error occurs.
   */
  boolean test(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db, @NotNull Config config)
      throws DatabaseOperationException;

  /**
   * Purge all internal caches.
   */
  default void purgeCaches() {
  }
}
