package net.darmo_creations.imageslibrary.data.batch_operations;

import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * An operation that transforms the path of a {@link Picture}.
 */
public final class TransformPathOperation extends Operation {
  public static final String KEY = "transform_path";

  private final String rawPattern;
  private final Pattern pattern;
  private final String substitute;
  private final boolean asRegex;

  /**
   * Create a new operation that transforms the path of {@link Picture}s.
   *
   * @param pattern    The search pattern.
   * @param substitute The substitution string.
   * @param asRegex    If true, the pattern will be interpreted as a Regex, otherwise as plain text.
   * @param condition  An optional condition.
   */
  public TransformPathOperation(
      @NotNull String pattern,
      @NotNull String substitute,
      boolean asRegex,
      Condition condition
  ) {
    super(condition);
    this.rawPattern = Objects.requireNonNull(pattern);
    this.pattern = Pattern.compile(this.rawPattern);
    this.substitute = Objects.requireNonNull(substitute);
    this.asRegex = asRegex;
  }

  @Override
  protected void execute(@NotNull Picture picture, @NotNull DatabaseConnection db) throws DatabaseOperationException {
    final Path newPath = Path.of(this.pattern.matcher(picture.path().toString()).replaceAll(this.substitute));
    db.updatePicture(new PictureUpdate(picture.id(), newPath, picture.hash(), Set.of(), Set.of()));
  }

  public String pattern() {
    return this.rawPattern;
  }

  public String substitute() {
    return this.substitute;
  }

  public boolean isPatternRegex() {
    return this.asRegex;
  }

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public String serialize() {
    return "%s\n%s\n%s".formatted(
        this.asRegex ? "1" : "0",
        this.rawPattern,
        this.substitute
    );
  }

  /**
   * Deserialize a string into a new {@link TransformPathOperation} object.
   *
   * @param serialized The string to deserialize.
   * @param condition  An optional condition.
   * @return A new {@link DeleteOperation} object.
   */
  @Contract(pure = true, value = "_, _ -> new")
  public static TransformPathOperation deserialize(@NotNull String serialized, Condition condition) {
    final String[] parts = serialized.split("\n", 3);
    if (parts.length != 3)
      throw new IllegalArgumentException("Invalid serialized data");
    return new TransformPathOperation(
        parts[1],
        parts[2],
        !parts[0].equals("0"),
        condition
    );
  }
}
