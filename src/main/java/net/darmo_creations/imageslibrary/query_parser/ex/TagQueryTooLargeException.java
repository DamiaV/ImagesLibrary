package net.darmo_creations.imageslibrary.query_parser.ex;

/**
 * This exception is thrown when a tag query tree is too deep.
 */
public class TagQueryTooLargeException extends RuntimeException {
  public TagQueryTooLargeException(String message) {
    super(message);
  }
}
