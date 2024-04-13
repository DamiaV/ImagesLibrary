// Generated from TagQueryLanguage.g4 by ANTLR 4.9.3

package net.darmo_creations.imageslibrary.query_parser.generated;

import org.antlr.v4.runtime.tree.*;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TagQueryLanguageParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface TagQueryLanguageVisitor<T> extends ParseTreeVisitor<T> {
  /**
   * Visit a parse tree produced by {@link TagQueryLanguageParser#query}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitQuery(TagQueryLanguageParser.QueryContext ctx);

  /**
   * Visit a parse tree produced by the {@code Or}
   * labeled alternative in {@link TagQueryLanguageParser#expr()}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitOr(TagQueryLanguageParser.OrContext ctx);

  /**
   * Visit a parse tree produced by the {@code Negation}
   * labeled alternative in {@link TagQueryLanguageParser#expr()}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitNegation(TagQueryLanguageParser.NegationContext ctx);

  /**
   * Visit a parse tree produced by the {@code And}
   * labeled alternative in {@link TagQueryLanguageParser#expr()}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitAnd(TagQueryLanguageParser.AndContext ctx);

  /**
   * Visit a parse tree produced by the {@code Literal}
   * labeled alternative in {@link TagQueryLanguageParser#expr()}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitLiteral(TagQueryLanguageParser.LiteralContext ctx);

  /**
   * Visit a parse tree produced by the {@code Group}
   * labeled alternative in {@link TagQueryLanguageParser#lit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitGroup(TagQueryLanguageParser.GroupContext ctx);

  /**
   * Visit a parse tree produced by the {@code PseudoTagString}
   * labeled alternative in {@link TagQueryLanguageParser#lit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPseudoTagString(TagQueryLanguageParser.PseudoTagStringContext ctx);

  /**
   * Visit a parse tree produced by the {@code PseudoTagRegex}
   * labeled alternative in {@link TagQueryLanguageParser#lit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitPseudoTagRegex(TagQueryLanguageParser.PseudoTagRegexContext ctx);

  /**
   * Visit a parse tree produced by the {@code BooleanPseudoTag}
   * labeled alternative in {@link TagQueryLanguageParser#lit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitBooleanPseudoTag(TagQueryLanguageParser.BooleanPseudoTagContext ctx);

  /**
   * Visit a parse tree produced by the {@code Tag}
   * labeled alternative in {@link TagQueryLanguageParser#lit}.
   *
   * @param ctx the parse tree
   * @return the visitor result
   */
  T visitTag(TagQueryLanguageParser.TagContext ctx);
}