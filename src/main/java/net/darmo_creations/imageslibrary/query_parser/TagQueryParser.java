package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.query_parser.generated.*;
import org.antlr.v4.runtime.*;
import org.jetbrains.annotations.*;
import org.logicng.formulas.*;

import java.util.*;

/**
 * This class parses tag query strings into {@link TagQuery} objects.
 */
public final class TagQueryParser {
  /**
   * Parse a tag query string.
   *
   * @param query          The tag query string to parse.
   * @param tagDefinitions A map containing definitions of compound tags.
   * @param pseudoTags     A map containing pseudo-tags.
   * @return The parsed {@link TagQuery} object.
   * @throws InvalidPseudoTagException If the formula contains a pseudo-tag
   *                                   that is not present in the {@code pseudoTags} map.
   */
  @Contract("_, _, _ -> new")
  public static TagQuery parse(
      String query,
      final Map<String, String> tagDefinitions,
      final Map<String, PseudoTag> pseudoTags
  ) throws InvalidPseudoTagException {
    return new TagQuery(parse(query, tagDefinitions, 0, new FormulaFactory()), pseudoTags);
  }

  /**
   * Parse a tag query string into a {@link Formula}.
   *
   * @param query          The tag query string to parse.
   * @param tagDefinitions A map containing definitions of compound tags.
   * @param depth          The current recursion depth.
   * @param formulaFactory The {@link FormulaFactory} to use to create new {@link Formula}s.
   * @return A {@link Formula} corresponding to the given tag query string.
   */
  @Contract("_, _, _, _ -> new")
  static Formula parse(
      String query,
      final Map<String, String> tagDefinitions,
      int depth,
      FormulaFactory formulaFactory
  ) {
    final var errorListener = new ErrorListener();
    final var lexer = new TagQueryLanguageLexer(CharStreams.fromString(query));
    lexer.removeErrorListeners(); // Remove default listener that prints to STDOUT
    lexer.addErrorListener(errorListener);
    final var parser = new TagQueryLanguageParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners(); // Remove default listener that prints to STDOUT
    parser.addErrorListener(errorListener);
    return new TagQueryVisitor(tagDefinitions, depth, formulaFactory).visit(parser.expr());
  }

  private TagQueryParser() {
  }

  /**
   * Custom error listener that throws {@link TagQuerySyntaxErrorException}
   * whenever the associated parser encounters a syntax error.
   */
  private static class ErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(
        Recognizer<?, ?> recognizer,
        Object offendingSymbol,
        int line,
        int charPositionInLine,
        String msg,
        RecognitionException e
    ) {
      throw new TagQuerySyntaxErrorException(line, charPositionInLine + 1, msg);
    }
  }
}
