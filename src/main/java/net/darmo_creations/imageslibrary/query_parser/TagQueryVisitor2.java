package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.query_parser.generated.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class visits a parser tree and transforms it into
 * a list of {@link Span} objects for syntax highlighting purposes.
 */
class TagQueryVisitor2 extends TagQueryLanguageBaseVisitor<List<Span>> {
  @Override
  public List<Span> visitQuery(TagQueryLanguageParser.QueryContext ctx) {
    final var list = this.visit(ctx.expr());
    addTerminal(ctx.WS(0), list, "ws");
    addTerminal(ctx.WS(1), list, "ws");
    return list;
  }

  @Override
  public List<Span> visitOr(TagQueryLanguageParser.OrContext ctx) {
    final var left = this.visit(ctx.expr(0));
    addTerminal(ctx.WS(0), left, "ws");
    addTerminal(ctx.WS(1), left, "ws");
    addTerminal(ctx.OR(), left, "or");
    left.addAll(this.visit(ctx.expr(1)));
    return left;
  }

  @Override
  public List<Span> visitAnd(TagQueryLanguageParser.AndContext ctx) {
    final var left = this.visit(ctx.expr(0));
    addTerminal(ctx.WS(), left, "ws");
    left.addAll(this.visit(ctx.expr(1)));
    return left;
  }

  @Override
  public List<Span> visitNegation(TagQueryLanguageParser.NegationContext ctx) {
    final var list = this.visit(ctx.lit());
    addTerminal(ctx.WS(), list, "ws");
    addTerminal(ctx.NOT(), list, "not");
    return list;
  }

  @Override
  public List<Span> visitLiteral(TagQueryLanguageParser.LiteralContext ctx) {
    return this.visit(ctx.lit());
  }

  @Override
  public List<Span> visitGroup(TagQueryLanguageParser.GroupContext ctx) {
    final var list = this.visit(ctx.expr());
    addTerminal(ctx.LPAREN(), list, "paren");
    addTerminal(ctx.RPAREN(), list, "paren");
    addTerminal(ctx.WS(0), list, "ws");
    addTerminal(ctx.WS(1), list, "ws");
    return list;
  }

  @Override
  public List<Span> visitPseudoTagString(TagQueryLanguageParser.PseudoTagStringContext ctx) {
    final List<Span> list = new LinkedList<>();
    addTerminal(ctx.IDENT(0), list, "pseudo-tag");
    addTerminal(ctx.EQUAL(), list, "equal");
    addTerminal(ctx.IDENT(1), list, "flags");
    addTerminal(ctx.STRING(), list, "string");
    return list;
  }

  @Override
  public List<Span> visitPseudoTagRegex(TagQueryLanguageParser.PseudoTagRegexContext ctx) {
    final List<Span> list = new LinkedList<>();
    addTerminal(ctx.IDENT(0), list, "pseudo-tag");
    addTerminal(ctx.EQUAL(), list, "equal");
    addTerminal(ctx.IDENT(1), list, "flags");
    addTerminal(ctx.REGEX(), list, "regex");
    return list;
  }

  @Override
  public List<Span> visitBooleanPseudoTag(TagQueryLanguageParser.BooleanPseudoTagContext ctx) {
    final List<Span> list = new LinkedList<>();
    addTerminal(ctx.HASH(), list, "hash");
    addTerminal(ctx.IDENT(), list, "pseudo-tag");
    return list;
  }

  @Override
  public List<Span> visitTag(TagQueryLanguageParser.TagContext ctx) {
    final List<Span> list = new LinkedList<>();
    addTerminal(ctx.IDENT(), list, "tag");
    return list;
  }

  @Override
  public List<Span> visit(@Nullable ParseTree tree) {
    if (tree == null)
      return new LinkedList<>();
    final var list = super.visit(tree);
    return list == null ? new LinkedList<>() : list;
  }

  private static void addTerminal(@Nullable TerminalNode node, List<Span> list, String cssClass) {
    if (node != null) {
      final Token symbol = node.getSymbol();
      if (symbol.getStartIndex() != -1 && symbol.getStopIndex() != -1)
        list.add(new Span(cssClass, symbol.getStartIndex(), symbol.getStopIndex()));
    }
  }
}
