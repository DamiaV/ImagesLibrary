package net.darmo_creations.imageslibrary.ui.syntax_highlighting;

import net.darmo_creations.imageslibrary.ui.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base interface for syntax highlighters accepted by the {@link AutoCompleteTextField} class.
 */
public interface SyntaxHighlighter {
  /**
   * The CSS class name to apply to the highlighted field.
   */
  String cssClass();

  /**
   * Parse the given text and return a list of {@link Span} objects for each valid token.
   *
   * @param text The text to parse.
   * @return A list of {@link Span} objects.
   */
  Collection<Span> highlight(@NotNull String text);
}
