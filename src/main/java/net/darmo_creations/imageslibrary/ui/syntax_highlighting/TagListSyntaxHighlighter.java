package net.darmo_creations.imageslibrary.ui.syntax_highlighting;

import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Syntax highlighter for tag lists.
 */
public class TagListSyntaxHighlighter implements SyntaxHighlighter {
  private static final String CSS_CLASS = "tag-list";
  private static final String ERROR_STYLE = "-fx-fill: red";

  private final Set<Tag> allTags;
  private final Set<TagType> allTagTypes;

  /**
   * Create a new syntax highlighter.
   *
   * @param allTags The set of tags to use for color lookup.
   */
  public TagListSyntaxHighlighter(final @NotNull Set<Tag> allTags, final @NotNull Set<TagType> allTagTypes) {
    this.allTags = allTags;
    this.allTagTypes = allTagTypes;
  }

  @Override
  public String cssClass() {
    return CSS_CLASS;
  }

  @Override
  public Collection<Span> highlight(@NotNull String text) {
    final List<Span> spans = new LinkedList<>();
    final var buffer = new StringBuilder();
    char tagTypeSymbol = 0;
    boolean isError = false;
    int i = 0, start = 0;
    final var iterator = text.chars().iterator();

    while (iterator.hasNext()) {
      final int codepoint = iterator.nextInt();
      final String ch = Character.toString(codepoint);

      if (Character.isWhitespace(codepoint)) {
        if (!buffer.isEmpty()) {
          spans.add(new Span(this.getTagStyle(buffer.toString()), start, i - 1));
          buffer.setLength(0);
        } else if (tagTypeSymbol != 0) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
          tagTypeSymbol = 0;
        } else if (isError) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
          isError = false;
        }

      } else if (TagLike.isLabelValid(ch)) {
        if (tagTypeSymbol != 0) {
          spans.add(new Span(this.getTagTypeStyle(tagTypeSymbol), start, i - 1));
          tagTypeSymbol = 0;
        } else if (isError) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
          isError = false;
        }
        if (buffer.isEmpty())
          start = i;
        buffer.append(ch);

      } else if (TagTypeLike.isSymbolValid(ch.charAt(0))) {
        if (!buffer.isEmpty()) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
          buffer.setLength(0);
        } else if (tagTypeSymbol != 0) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
        } else if (isError) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
          isError = false;
        }
        start = i;
        tagTypeSymbol = ch.charAt(0);

      } else {
        if (!buffer.isEmpty()) {
          spans.add(new Span(this.getTagStyle(buffer.toString()), start, i - 1));
          buffer.setLength(0);
        } else if (tagTypeSymbol != 0) {
          spans.add(new Span(ERROR_STYLE, start, i - 1));
        }
        if (!isError) {
          start = i;
          isError = true;
        }
      }

      i++;
    }

    if (!buffer.isEmpty())
      spans.add(new Span(this.getTagStyle(buffer.toString()), start, i - 1));
    else if (tagTypeSymbol != 0)
      spans.add(new Span(ERROR_STYLE, start, i - 1));
    else if (isError)
      spans.add(new Span(ERROR_STYLE, start, i - 1));

    return spans;
  }

  private String getTagStyle(@NotNull String tagName) {
    return this.allTags.stream()
        .filter(tag -> tag.label().equals(tagName))
        .findFirst()
        .map(tag -> {
          final String prefix = tag.definition().isPresent() ? "-fx-font-style: italic; " : ""; // Compound tag?
          return prefix + tag.type().map(type -> "-fx-fill: " + StringUtils.colorToCss(type.color())).orElse("");
        })
        .orElse("-fx-font-weight: bold"); // New tag
  }

  private String getTagTypeStyle(char symbol) {
    return this.allTagTypes.stream()
        .filter(type -> type.symbol() == symbol)
        .findFirst()
        .map(type -> "-fx-fill: " + StringUtils.colorToCss(type.color()))
        .orElse(ERROR_STYLE);
  }
}
