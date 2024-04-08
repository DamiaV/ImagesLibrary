// Generated from TagQueryLanguage.g4 by ANTLR 4.9.3

package net.darmo_creations.imageslibrary.query_parser.generated;

import org.antlr.v4.runtime.tree.*;

/**
 * This class provides an empty implementation of {@link TagQueryLanguageVisitor},
 * which can be extended to create a visitor which only needs to handle a subset
 * of the available methods.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public class TagQueryLanguageBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements TagQueryLanguageVisitor<T> {
  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitOr(TagQueryLanguageParser.OrContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitNegation(TagQueryLanguageParser.NegationContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitAnd(TagQueryLanguageParser.AndContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitLiteral(TagQueryLanguageParser.LiteralContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitGroup(TagQueryLanguageParser.GroupContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitPseudoTagString(TagQueryLanguageParser.PseudoTagStringContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitPseudoTagRegex(TagQueryLanguageParser.PseudoTagRegexContext ctx) {
    return this.visitChildren(ctx);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation returns the result of calling
   * {@link #visitChildren} on {@code ctx}.</p>
   */
  @Override
  public T visitTag(TagQueryLanguageParser.TagContext ctx) {
    return this.visitChildren(ctx);
  }
}