package net.darmo_creations.imageslibrary.data.batch_operations;

import javafx.util.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

/**
 * An operation that updates the tags of a {@link MediaFile}.
 */
public final class UpdateTagsOperation extends Operation {
  public static final String KEY = "update_tags";

  private final Set<ParsedTag> tagsToAdd = new HashSet<>();
  private final Set<Tag> tagsToRemove = new HashSet<>();

  /**
   * Create a new operation that the updates tags of {@link MediaFile}s.
   *
   * @param tagsToAdd    The set of tags to add.
   * @param tagsToRemove The set of tags to remove.
   * @param condition    An optional condition.
   */
  public UpdateTagsOperation(@NotNull Set<ParsedTag> tagsToAdd, @NotNull Set<Tag> tagsToRemove, Condition condition) {
    super(condition);
    this.tagsToAdd.addAll(tagsToAdd);
    this.tagsToRemove.addAll(tagsToRemove);
  }

  @Override
  protected Pair<Boolean, MediaFile> execute(@NotNull MediaFile mediaFile, @NotNull DatabaseConnection db)
      throws DatabaseOperationException {
    final Set<Tag> oldTags = db.getMediaTags(mediaFile);
    db.updateMedia(new MediaFileUpdate(mediaFile.id(), mediaFile.path(), mediaFile.hash(), this.tagsToAdd, this.tagsToRemove));
    final Set<Tag> newTags = db.getMediaTags(mediaFile);
    return new Pair<>(!oldTags.equals(newTags), mediaFile);
  }

  @UnmodifiableView
  public Set<ParsedTag> tagsToAdd() {
    return Collections.unmodifiableSet(this.tagsToAdd);
  }

  @UnmodifiableView
  public Set<Tag> tagsToRemove() {
    return Collections.unmodifiableSet(this.tagsToRemove);
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return "%s\t%s".formatted(
        this.tagsToAdd.stream().map(tag -> {
          if (tag.tagType().isPresent())
            return tag.tagType().get().symbol() + tag.label();
          return tag.label();
        }).collect(Collectors.joining(" ")),
        this.tagsToRemove.stream().map(Tag::label).collect(Collectors.joining(" "))
    );
  }

  /**
   * Deserialize a string into a new {@link UpdateTagsOperation} object.
   *
   * @param serialized The string to deserialize.
   * @param condition  An optional condition.
   * @return A new {@link UpdateTagsOperation} object.
   * @throws IllegalArgumentException If the string is not valid.
   */
  @Contract(pure = true, value = "_, _, _ -> new")
  public static UpdateTagsOperation deserialize(
      @NotNull String serialized,
      Condition condition,
      @NotNull DatabaseConnection db
  ) {
    final String[] parts = serialized.split("\t", 2);
    if (parts.length != 2)
      throw new IllegalArgumentException("Invalid serialized data");

    final Set<ParsedTag> toAdd = Arrays.stream(parts[0].split(" "))
        .map(s -> {
          try {
            final var tagParts = TagLike.splitLabel(s);
            final var tagType = tagParts.typeSymbol().flatMap(c -> getTagType(db, c));
            return new ParsedTag(tagType, tagParts.label());
          } catch (final TagParseException e) {
            throw new IllegalArgumentException(e);
          }
        })
        .collect(Collectors.toSet());

    final Set<Tag> toRemove = Arrays.stream(parts[1].split(" "))
        .flatMap(s -> getTag(db, s).stream())
        .collect(Collectors.toSet());

    return new UpdateTagsOperation(toAdd, toRemove, condition);
  }

  private static Optional<TagType> getTagType(@NotNull DatabaseConnection db, @NotNull char c) {
    return db.getAllTagTypes().stream().filter(type -> type.symbol() == c).findFirst();
  }

  private static Optional<Tag> getTag(@NotNull DatabaseConnection db, @NotNull String s) {
    return db.getAllTags().stream()
        .filter(t -> t.label().equals(s))
        .findFirst();
  }
}
