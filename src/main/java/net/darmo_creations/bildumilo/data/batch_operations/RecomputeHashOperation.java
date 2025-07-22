package net.darmo_creations.bildumilo.data.batch_operations;

import javafx.util.*;
import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * An operation that computes the hash of a {@link MediaFile}.
 */
public final class RecomputeHashOperation extends Operation {
  public static final String KEY = "recompute_hash";

  /**
   * Create a new operation that computes the hash of {@link MediaFile}s.
   *
   * @param condition An optional condition.
   */
  public RecomputeHashOperation(Condition condition) {
    super(condition);
  }

  @Override
  protected Pair<Boolean, MediaFile> execute(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db)
      throws DatabaseOperationException {
    final Optional<Hash> currentHash = mediaFile.hash();
    final Optional<Hash> hash = Hash.computeForFile(mediaFile.path());
    db.updateMedia(new MediaFileUpdate(mediaFile.id(), mediaFile.path(), hash, Set.of(), Set.of()));
    return new Pair<>(!currentHash.equals(hash), new MediaFile(mediaFile.id(), mediaFile.path(), hash.orElse(null)));
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
