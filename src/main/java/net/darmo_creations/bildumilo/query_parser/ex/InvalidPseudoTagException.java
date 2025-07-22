package net.darmo_creations.bildumilo.query_parser.ex;

/**
 * This exception is thrown whenever an invalid pseudo-tag is used in a tag query.
 */
public class InvalidPseudoTagException extends Exception {
  private final String pseudoTag;

  public InvalidPseudoTagException(String msg, String pseudoTag) {
    super(msg);
    this.pseudoTag = pseudoTag;
  }

  public String pseudoTag() {
    return this.pseudoTag;
  }
}
