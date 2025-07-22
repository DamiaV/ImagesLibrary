package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A condition that evaluates a {@link TagQuery} against {@link MediaFile}s.
 */
public final class TagQueryCondition implements Condition {
  public static final String KEY = "tag_query";

  private final String rawQuery;
  private TagQuery tagQueryCache;

  /**
   * Create a condition for the given tag query.
   * <p>
   * The query will only be parsed the first time {@link #test(MediaFile, DatabaseConnection, Config)} is called,
   * or after {@link #purgeCaches()} has been called.
   *
   * @param tagQuery The tag query.
   */
  public TagQueryCondition(@NotNull String tagQuery) {
    this.rawQuery = Objects.requireNonNull(tagQuery);
  }

  @Override
  public boolean test(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db, @NotNull Config config)
      throws DatabaseOperationException {
    if (this.tagQueryCache == null)
      try {
        this.tagQueryCache = TagQueryParser.parse(
            this.rawQuery,
            db.getTagDefinitions(),
            DatabaseConnection.PSEUDO_TAGS,
            config
        );
      } catch (final InvalidPseudoTagException | TagQuerySyntaxErrorException | TagQueryTooLargeException e) {
        throw new DatabaseOperationException(DatabaseErrorCode.UNKNOWN_ERROR, e);
      }
    return db.mediaMatchesQuery(mediaFile, this.tagQueryCache);
  }

  public String tagQuery() {
    return this.rawQuery;
  }

  @Override
  public void purgeCaches() {
    this.tagQueryCache = null;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return this.rawQuery;
  }
}
