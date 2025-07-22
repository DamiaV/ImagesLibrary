package net.darmo_creations.bildumilo.ui.syntax_highlighting;

import net.darmo_creations.bildumilo.query_parser.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Syntax highlighter for tag queries.
 */
public class TagQuerySyntaxHighlighter implements SyntaxHighlighter {
  private static final String CSS_CLASS = "tag-query";

  @Override
  public String cssClass() {
    return CSS_CLASS;
  }

  @Override
  public Collection<Span> highlight(@NotNull String text) {
    return TagQueryParser.parse(text);
  }
}
