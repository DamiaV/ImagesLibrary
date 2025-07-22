package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import org.intellij.lang.annotations.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.*;
import org.logicng.formulas.*;
import org.logicng.transformations.simplification.*;

import java.util.*;
import java.util.function.*;

/**
 * A tag query is a logical expression that is used to filter medias.
 * Tag queries can be converted into a SQL query.
 */
public final class TagQuery {
  private final Formula formula;
  @Nullable
  private final String sql;
  private final TagQueryPredicate predicate;
  @Nullable
  private final Config config;

  /**
   * Create a new tag query from the given formula.
   *
   * @param formula    The formula to wrap into a tag query.
   * @param pseudoTags A map containing pseudo-tags.
   * @param config     The app’s config.
   * @throws InvalidPseudoTagException If the formula contains a pseudo-tag
   *                                   that is not present in the {@code pseudoTags} map.
   */
  public TagQuery(
      final @NotNull Formula formula,
      final @NotNull Map<String, PseudoTag> pseudoTags,
      final Config config
  ) throws InvalidPseudoTagException {
    this.config = config;
    this.formula = new FactorOutSimplifier().apply(formula, false);
    this.sql = this.toSql(this.formula, pseudoTags);
    this.predicate = this.toPredicate(formula, pseudoTags);
  }

  /**
   * Return the SQL query equivalent of this tag query.
   *
   * @return An {@link Optional} containing the SQL query,
   * or an empty {@link Optional} if the query is false.
   */
  @Contract(pure = true)
  public Optional<String> asSQL() {
    return Optional.ofNullable(this.sql);
  }

  /**
   * A {@link BiPredicate} that corresponds to this query.
   * It takes a {@link MediaFile} and the set of its tags.
   */
  @Contract(pure = true)
  public TagQueryPredicate predicate() {
    return this.predicate;
  }

  /**
   * Convert the given {@link Formula} into a SQL query.
   *
   * @param formula    The formula to convert.
   * @param pseudoTags A map containing pseudo-tag definitions.
   * @return The SQL query for the given formula or null if the formula is false.
   */
  private @Nullable String toSql(final @NotNull Formula formula, final @NotNull Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    // Checking for CTrue and CFalse only here as the simplified formula should contain neither in any branch
    if (formula instanceof CTrue)
      // language=sqlite
      return "SELECT id, path, hash FROM images";
    if (formula instanceof CFalse)
      return null;
    return this.toSql_(formula, pseudoTags);
  }

  /**
   * Convert the given {@link Formula} into a SQL query.
   * Contrary to the {@link #toSql(Formula, Map)} method, {@link CTrue} and {@link CFalse} symbols
   * will throw an exception.
   *
   * @param formula    The formula to convert.
   * @param pseudoTags A map containing pseudo-tag definitions.
   * @return The SQL query for the given formula or null if the formula is false.
   */
  private String toSql_(final @NotNull Formula formula, final @NotNull Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    Objects.requireNonNull(formula);

    if (formula instanceof Variable variable) {
      final String text = variable.name();
      if (text.contains(":")) { // Pseudo-tag
        final var parts = text.split(":", 4);
        if (parts.length == 2)
          return getBooleanFlag(pseudoTags, parts[0]).sqlTemplate();
        final var parseResult = this.parsePseudoTag(pseudoTags, parts[0], parts[2], parts[3], parts[1], true);
        return parseResult.tag().acceptsRegex()
            ? parseResult.tag().sqlTemplate().formatted(parseResult.pattern(), parseResult.flags())
            : parseResult.tag().sqlTemplate().formatted(parseResult.pattern());
      } else // Normal tag
        // language=sqlite
        return """
            SELECT i.id, i.path, i.hash
            FROM images AS i, tags AS t, image_tag AS it
            WHERE t.label = '%s'
              AND t.id = it.tag_id
              AND it.image_id = i.id
            """.formatted(text);
    }

    if (formula instanceof Not not)
      // language=sqlite
      return """
          SELECT id, path, hash
          FROM images
          EXCEPT
          """ + this.toSql_(not.operand(), pseudoTags);

    if (formula instanceof And and)
      return this.joinOperands(and, pseudoTags, "INTERSECT");

    if (formula instanceof Or or)
      return this.joinOperands(or, pseudoTags, "UNION");

    if (formula instanceof Literal literal) {
      final var variable = literal.variable();
      final String subQuery = this.toSql_(variable, pseudoTags);
      if (literal.phase()) // Positive variable
        return subQuery;
      else // Negated variable
        // language=sqlite
        return """
            SELECT id, path, hash
            FROM images
            EXCEPT
            """ + subQuery;
    }

    throw new RuntimeException("Unsupported Formula type: " + formula.getClass());
  }

  /**
   * Convert the operands of the given operator to SQL queries then join them with the given keyword.
   *
   * @param operator      The operator to convert to extract operands from.
   * @param pseudoTags    A map containing pseudo-tag definitions.
   * @param joinerKeyword The SQL keyword to use as a joiner.
   * @return The constructed SQL query.
   */
  private String joinOperands(
      final @NotNull NAryOperator operator,
      final @NotNull Map<String, PseudoTag> pseudoTags,
      @Language(value = "sqlite", prefix = "(SELECT * FROM table) ", suffix = " (SELECT * FROM table)")
      @NotNull String joinerKeyword
  ) throws InvalidPseudoTagException {
    final var query = new StringJoiner(
        "\n%s\n".formatted(joinerKeyword),
        // language=sqlite suffix=images)
        """
            SELECT id, path, hash
            FROM (
            """,
        ")"
    );
    for (final var subQuery : operator)
      query.add(this.toSql_(subQuery, pseudoTags));
    return query.toString();
  }

  private TagQueryPredicate toPredicate(
      final @NotNull Formula formula,
      final @NotNull Map<String, PseudoTag> pseudoTags
  ) throws InvalidPseudoTagException {
    // Checking for CTrue and CFalse only here as the simplified formula should contain neither in any branch
    if (formula instanceof CTrue)
      return (p, t, db) -> true;
    if (formula instanceof CFalse)
      return (p, t, db) -> false;
    return this.toPredicate_(formula, pseudoTags);
  }

  private TagQueryPredicate toPredicate_(
      final @NotNull Formula formula,
      final @NotNull Map<String, PseudoTag> pseudoTags
  ) throws InvalidPseudoTagException {
    if (formula instanceof Variable variable) {
      final String text = variable.name();
      if (text.contains(":")) {
        final var parts = text.split(":", 4);
        if (parts.length == 2)
          return getBooleanFlag(pseudoTags, parts[0]).predicate();
        final var parseResult = this.parsePseudoTag(pseudoTags, parts[0], parts[2], parts[3], parts[1], false);
        return parseResult.tag().acceptsRegex()
            ? parseResult.tag().predicateFactory().apply(parseResult.pattern(), parseResult.flags())
            : parseResult.tag().predicateFactory().apply(parseResult.pattern(), null);
      }
      return (p, t, db) -> t.stream().anyMatch(tag -> tag.label().equals(text));
    }

    if (formula instanceof Not not)
      return this.toPredicate_(not.operand(), pseudoTags).negate();

    if (formula instanceof And and)
      return this.joinOperands(and, pseudoTags, TagQueryPredicate::and);

    if (formula instanceof Or or)
      return this.joinOperands(or, pseudoTags, TagQueryPredicate::or);

    if (formula instanceof Literal literal) {
      final var pred = this.toPredicate_(literal.variable(), pseudoTags);
      return literal.phase() ? pred : pred.negate();
    }

    throw new RuntimeException("Unsupported Formula type: " + formula.getClass());
  }

  private @NotNull ParseResult parsePseudoTag(
      final @NotNull Map<String, PseudoTag> pseudoTags,
      @NotNull String tagName,
      @NotNull String tagFlags,
      @NotNull String tagPattern,
      @NotNull String tagType,
      boolean escapeSingleQuotes
  ) throws InvalidPseudoTagException {
    final PatternPseudoTag tag = getPatternPseudoTag(pseudoTags, tagName, tagFlags);

    String pattern = tagPattern;
    switch (tagType) {
      case "string" -> {
        if (tag.acceptsRegex()) {
          pattern = pattern
              // Escape regex meta-characters except "*" and "?"
              .replaceAll("([\\[\\]()+{.^$|])", "\\\\$1")
              // Replace with regexs "*" and "?" that are preceeded by an even number of \ (including 0)
              .replaceAll("((?<!\\\\)(?:\\\\\\\\)*)([*?])", "$1.$2");
          // language=regexp
          pattern = "^%s$".formatted(pattern);
        }
      }
      case "regex" -> {
        if (!tag.acceptsRegex())
          throw new InvalidPseudoTagException("Pseudo-tag '%s' does not accept RegExs".formatted(tagName), tagName);
      }
      default -> throw new RuntimeException("Invalid metatag type: " + tagType);
    }
    if (escapeSingleQuotes)
      pattern = pattern.replace("'", "''");

    if (tag.acceptsRegex()) {
      if (!tagFlags.contains(String.valueOf(PatternPseudoTag.CASE_SENSITIVE_FLAG))
          && !tagFlags.contains(String.valueOf(PatternPseudoTag.CASE_INSENSITIVE_FLAG)))
        tagFlags += this.config != null && this.config.caseSensitiveQueriesByDefault() ? "s" : "i";
      else if (tagFlags.contains(String.valueOf(PatternPseudoTag.CASE_SENSITIVE_FLAG))
          && tagFlags.contains(String.valueOf(PatternPseudoTag.CASE_INSENSITIVE_FLAG)))
        throw new InvalidPseudoTagException(tagName, tagName);
    }

    return new ParseResult(tag, tagFlags, pattern);
  }

  private record ParseResult(@NotNull PatternPseudoTag tag, @NotNull String flags, @NotNull String pattern) {
    private ParseResult {
      Objects.requireNonNull(tag);
      Objects.requireNonNull(flags);
      Objects.requireNonNull(pattern);
    }
  }

  private @Nullable TagQueryPredicate joinOperands(
      final @NotNull NAryOperator operator,
      final @NotNull Map<String, PseudoTag> pseudoTags,
      @NotNull BiFunction<TagQueryPredicate, TagQueryPredicate, TagQueryPredicate> aggregator
  ) throws InvalidPseudoTagException {
    TagQueryPredicate acc = null;
    for (final Formula operand : operator) {
      final var pred = this.toPredicate_(operand, pseudoTags);
      if (acc == null)
        acc = pred;
      else
        acc = aggregator.apply(acc, pred);
    }
    return acc;
  }

  private static BooleanFlag getBooleanFlag(
      @NotNull Map<String, PseudoTag> pseudoTags,
      @NotNull String tagName
  ) throws InvalidPseudoTagException {
    final PseudoTag t = pseudoTags.get(tagName);
    if (t == null)
      throw new InvalidPseudoTagException(tagName, tagName);
    if (!(t instanceof BooleanFlag flag))
      throw new InvalidPseudoTagException(tagName, tagName);
    return flag;
  }

  private static PatternPseudoTag getPatternPseudoTag(
      @NotNull Map<String, PseudoTag> pseudoTags,
      @NotNull String tagName,
      @NotNull String tagFlags
  ) throws InvalidPseudoTagException {
    final PseudoTag t = pseudoTags.get(tagName);
    if (t == null)
      throw new InvalidPseudoTagException(tagName, tagName);
    if (!(t instanceof PatternPseudoTag tag))
      throw new InvalidPseudoTagException(tagName, tagName);
    if (!tagFlags.isEmpty() && !tag.acceptsRegex())
      throw new InvalidPseudoTagException("Pseudo-tag %s does not accept flags".formatted(tagName), tagName);
    for (int i = 0; i < tagFlags.length(); i++) {
      final char flag = tagFlags.charAt(i);
      if (!PatternPseudoTag.FLAGS.contains(flag))
        throw new InvalidPseudoTagException("Invalid pseudo-tag flag: " + flag, tagName);
    }
    return tag;
  }

  @Override
  public String toString() {
    return this.formula.toString();
  }

  /*
   * For tests
   */

  /**
   * This query’s underlying formula.
   * Package-only access for tests.
   */
  Formula formula() {
    return this.formula;
  }
}
