package net.darmo_creations.bildumilo.query_parser.ex;

/**
 * This exception is thrown when a tag query contains a syntax error.
 */
public class TagQuerySyntaxErrorException extends RuntimeException {
  /**
   * Create a new exception.
   *
   * @param line   The line of the invalid character.
   * @param column The column of the invalid character.
   * @param msg    The error message.
   */
  public TagQuerySyntaxErrorException(int line, int column, String msg) {
    super("Syntax error (%d:%d): %s".formatted(line, column, msg));
  }
}
