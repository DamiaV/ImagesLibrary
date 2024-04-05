package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.query_parser.generated.*;
import org.logicng.formulas.*;

import java.util.*;

/**
 * This class visits a parser tree and transforms it into a {@link Formula} tree.
 */
class TagQueryVisitor extends TagQueryLanguageBaseVisitor<Formula> {
  /**
   * Maximum allowed recursion depth.
   */
  private static final int MAX_DEPTH = 20;

  private final Map<String, String> tagDefinitions;
  private final int depth;
  private final FormulaFactory formulaFactory;

  /**
   * Create a new tag query visitor.
   *
   * @param tagDefinitions A map containing compound tag definitions.
   * @param depth          The current recursion depth.
   * @param formulaFactory The {@link FormulaFactory} to use to create new {@link Formula}s.
   */
  public TagQueryVisitor(final Map<String, String> tagDefinitions, int depth, FormulaFactory formulaFactory) {
    this.tagDefinitions = Objects.requireNonNull(tagDefinitions);
    this.depth = depth;
    this.formulaFactory = formulaFactory;
  }

  @Override
  public Formula visitOr(TagQueryLanguageParser.OrContext ctx) {
    return this.formulaFactory.or(this.visit(ctx.expr(0)), this.visit(ctx.expr(1)));
  }

  @Override
  public Formula visitAnd(TagQueryLanguageParser.AndContext ctx) {
    return this.formulaFactory.and(this.visit(ctx.expr(0)), this.visit(ctx.expr(1)));
  }

  @Override
  public Formula visitNegation(TagQueryLanguageParser.NegationContext ctx) {
    return this.formulaFactory.not(this.visit(ctx.lit()));
  }

  @Override
  public Formula visitLiteral(TagQueryLanguageParser.LiteralContext ctx) {
    return this.visit(ctx.lit());
  }

  @Override
  public Formula visitGroup(TagQueryLanguageParser.GroupContext ctx) {
    return this.visit(ctx.expr());
  }

  @Override
  public Formula visitPseudoTagString(TagQueryLanguageParser.PseudoTagStringContext ctx) {
    final String label = ctx.IDENT().getText();
    final var flagNode = ctx.FLAG();
    final String flags = flagNode == null ? "" : flagNode.getText();
    String pattern = ctx.STRING().getText();
    pattern = pattern.substring(1, pattern.length() - 1)
        .replaceAll("\\\\([\"\\\\])", "$1"); // Unescape \ and "
    return this.formulaFactory.variable("%s:string:%s:%s".formatted(label, flags, pattern));
  }

  @Override
  public Formula visitPseudoTagRegex(TagQueryLanguageParser.PseudoTagRegexContext ctx) {
    final String label = ctx.IDENT().getText();
    final var flagNode = ctx.FLAG();
    final String flags = flagNode == null ? "" : flagNode.getText();
    String pattern = ctx.REGEX().getText();
    pattern = pattern.substring(1, pattern.length() - 1)
        .replace("\\\\/", "/"); // Unescape /
    return this.formulaFactory.variable("%s:regex:%s:%s".formatted(label, flags, pattern));
  }

  @Override
  public Formula visitTag(TagQueryLanguageParser.TagContext ctx) {
    final String label = ctx.IDENT().getText();
    final String definition = this.tagDefinitions.get(label);
    if (definition != null) {
      if (this.depth == MAX_DEPTH)
        throw new TagQueryTooLargeException("Reached max recursive depth");
      else
        return TagQueryParser.parse(definition, this.tagDefinitions, this.depth + 1, this.formulaFactory);
    }
    return this.formulaFactory.variable(label);
  }
}
