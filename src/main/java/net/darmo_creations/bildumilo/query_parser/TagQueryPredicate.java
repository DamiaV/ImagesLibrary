package net.darmo_creations.bildumilo.query_parser;

import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Represents a predicate (boolean-valued function) of two arguments, a {@link MediaFile} and its tags.
 */
@FunctionalInterface
public interface TagQueryPredicate {
  /**
   * Evaluates this predicate on the given arguments.
   *
   * @param mediaFile A {@link MediaFile}.
   * @param tags      The media’s tags.
   * @param db        The database to get data from.
   * @return True if the input arguments match the predicate, otherwise false.
   */
  boolean test(@NotNull MediaFile mediaFile, final @NotNull Set<Tag> tags, final @NotNull DatabaseConnection db);

  /**
   * Returns a composed predicate that represents a short-circuiting logical
   * AND of this predicate and another.  When evaluating the composed
   * predicate, if this predicate is {@code false}, then the {@code other}
   * predicate is not evaluated.
   *
   * <p>Any exceptions thrown during evaluation of either predicate are relayed
   * to the caller; if evaluation of this predicate throws an exception, the
   * {@code other} predicate will not be evaluated.
   *
   * @param other A predicate that will be logically-ANDed with this predicate.
   * @return A composed predicate that represents the short-circuiting logical
   * AND of this predicate and the {@code other} predicate.
   * @throws NullPointerException If other is null.
   */
  default TagQueryPredicate and(@NotNull TagQueryPredicate other) {
    Objects.requireNonNull(other);
    return (mediaFile, tags, db) -> this.test(mediaFile, tags, db) && other.test(mediaFile, tags, db);
  }

  /**
   * Returns a predicate that represents the logical negation of this predicate.
   *
   * @return A predicate that represents the logical negation of this predicate
   */
  default TagQueryPredicate negate() {
    return (mediaFile, tags, db) -> !this.test(mediaFile, tags, db);
  }

  /**
   * Returns a composed predicate that represents a short-circuiting logical
   * OR of this predicate and another.  When evaluating the composed
   * predicate, if this predicate is {@code true}, then the {@code other}
   * predicate is not evaluated.
   *
   * <p>Any exceptions thrown during evaluation of either predicate are relayed
   * to the caller; if evaluation of this predicate throws an exception, the
   * {@code other} predicate will not be evaluated.
   *
   * @param other A predicate that will be logically-ORed with this predicate
   * @return A composed predicate that represents the short-circuiting logical
   * OR of this predicate and the {@code other} predicate.
   * @throws NullPointerException If other is null.
   */
  default TagQueryPredicate or(@NotNull TagQueryPredicate other) {
    Objects.requireNonNull(other);
    return (mediaFile, tags, db) -> this.test(mediaFile, tags, db) || other.test(mediaFile, tags, db);
  }
}
