package net.darmo_creations.bildumilo.data;

import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

public class TagParseException extends Exception {
  private final FormatArg[] formatArgs;

  public TagParseException(@NotNull String translationKey, @NotNull FormatArg... formatArgs) {
    super(translationKey);
    this.formatArgs = formatArgs;
  }

  public TagParseException(@NotNull Throwable cause, @NotNull String translationKey, @NotNull FormatArg... formatArgs) {
    super(translationKey, cause);
    this.formatArgs = formatArgs;
  }

  public String translationKey() {
    return this.getMessage();
  }

  public FormatArg[] formatArgs() {
    return this.formatArgs;
  }
}
