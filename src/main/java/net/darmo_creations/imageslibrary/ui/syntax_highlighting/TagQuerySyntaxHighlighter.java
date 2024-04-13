package net.darmo_creations.imageslibrary.ui.syntax_highlighting;

import net.darmo_creations.imageslibrary.query_parser.*;

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
  public Collection<Span> highlight(String text) {
    return TagQueryParser.parse(text);
  }
}
