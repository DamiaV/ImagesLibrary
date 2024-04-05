package net.darmo_creations.imageslibrary.query_parser.ex;

/**
 * This exception is thrown whenever an invalid pseudo-tag is used in a tag query.
 */
public class InvalidPseudoTagException extends Exception {
  public InvalidPseudoTagException(String msg) {
    super(msg);
  }
}
