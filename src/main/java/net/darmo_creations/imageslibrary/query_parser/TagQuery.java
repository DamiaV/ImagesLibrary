package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import org.intellij.lang.annotations.*;
import org.jetbrains.annotations.*;
import org.logicng.formulas.*;
import org.logicng.transformations.simplification.*;

import java.util.*;

/**
 * A tag query is a logical expression that is used to filter images.
 * Tag queries can be converted into a SQL query.
 */
public final class TagQuery {
  private final Formula formula;
  private final @Nullable String sql;

  /**
   * Create a new tag query from the given formula.
   *
   * @param formula    The formula to wrap into a tag query.
   * @param pseudoTags A map containing pseudo-tags.
   * @throws InvalidPseudoTagException If the formula contains a pseudo-tag
   *                                   that is not present in the {@code pseudoTags} map.
   */
  public TagQuery(final Formula formula, final Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    this.formula = new FactorOutSimplifier().apply(formula, false);
    this.sql = toSql(this.formula, pseudoTags);
  }

  /**
   * This query’s underlying formula.
   * Package-only access for tests.
   */
  Formula formula() {
    return this.formula;
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
   * Convert the given {@link Formula} into a SQL query.
   *
   * @param formula    The formula to convert.
   * @param pseudoTags A map containing pseudo-tag definitions.
   * @return The SQL query for the given formula or null if the formula is false.
   */
  private static @Nullable String toSql(final Formula formula, final Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    // Checking for CTrue and CFalse only here as the simplified formula should contain neither in any branch
    if (formula instanceof CTrue)
      // language=sqlite
      return "SELECT id, path, hash FROM images";
    if (formula instanceof CFalse)
      return null;
    return toSql_(formula, pseudoTags);
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
  private static String toSql_(final Formula formula, final Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    Objects.requireNonNull(formula);

    if (formula instanceof Variable variable) {
      final String text = variable.name();
      if (text.contains(":")) // Pseudo-tag
        return pseudoTagToSql(text, pseudoTags);
      else // Normal tag
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
                 """ + toSql_(not.operand(), pseudoTags);

    if (formula instanceof And and)
      return joinOperands(and, pseudoTags, "INTERSECT");

    if (formula instanceof Or or)
      return joinOperands(or, pseudoTags, "UNION");

    if (formula instanceof Literal literal) {
      final var variable = literal.variable();
      if (literal.phase()) // Positive variable
        return toSql_(variable, pseudoTags);
      else // Negated variable
        // language=sqlite
        return """
                   SELECT id, path, hash
                   FROM images
                   EXCEPT
                   """ + toSql_(variable, pseudoTags);
    }

    throw new RuntimeException("Unsupported Formula type: " + formula.getClass());
  }

  /**
   * Convert the given serialized pseudo-tag to an SQL query.
   *
   * @param text       The serialized pseudo-tag.
   * @param pseudoTags A map containing pseudo-tag definitions.
   * @return The SQL query for the pseudo-tag.
   */
  private static String pseudoTagToSql(String text, final Map<String, PseudoTag> pseudoTags)
      throws InvalidPseudoTagException {
    final var parts = text.split(":", 4);
    final String tagName = parts[0];
    final String tagType = parts[1];
    String tagFlags = parts[2];
    final String tagPattern = parts[3];

    final PseudoTag tag = pseudoTags.get(tagName);
    if (tag == null)
      throw new InvalidPseudoTagException(tagName, tagName);
    if (!"".equals(tagFlags) && !tag.acceptsRegex())
      throw new InvalidPseudoTagException("Pseudo-tag %s does not accept flags".formatted(tagName), tagName);
    checkPseudoTagFlags(tagName, tagFlags);

    String escaped = tagPattern;
    switch (tagType) {
      case "string" -> {
        if (tag.acceptsRegex()) {
          escaped = escaped
              // Escape regex meta-characters except "*" and "?"
              .replaceAll("([\\[\\]()+{.^$|])", "\\\\$1")
              // Replace with regexs "*" and "?" that are preceeded by an even number of \ (including 0)
              .replaceAll("((?<!\\\\)(?:\\\\\\\\)*)([*?])", "$1.$2");
          // language=regexp
          escaped = "^%s$".formatted(escaped);
        }
      }
      case "regex" -> {
        if (!tag.acceptsRegex())
          throw new InvalidPseudoTagException("Pseudo-tag '%s' does not accept RegExs".formatted(tagName), tagName);
      }
      default -> throw new RuntimeException("Invalid metatag type: " + tagType);
    }
    escaped = escaped.replace("'", "''"); // Espace single quotes

    if (tag.acceptsRegex()
        && !tagFlags.contains(String.valueOf(PseudoTag.CASE_SENSITIVE_FLAG))
        && !tagFlags.contains(String.valueOf(PseudoTag.CASE_INSENSITIVE_FLAG)))
      tagFlags += App.config() != null && App.config().caseSensitiveQueriesByDefault() ? "s" : "i";

    return tag.acceptsRegex()
        ? tag.sqlTemplate().formatted(escaped, tagFlags)
        : tag.sqlTemplate().formatted(escaped);
  }

  private static void checkPseudoTagFlags(String pseudoTag, String flags) throws InvalidPseudoTagException {
    for (int i = 0; i < flags.length(); i++) {
      final char flag = flags.charAt(i);
      if (!PseudoTag.FLAGS.contains(flag))
        throw new InvalidPseudoTagException("Invalid pseudo-tag flag: " + flag, pseudoTag);
    }
  }

  /**
   * Convert the operands of the given operator to SQL queries then join them with the given keyword.
   *
   * @param operator      The operator to convert to extract operands from.
   * @param pseudoTags    A map containing pseudo-tag definitions.
   * @param joinerKeyword The SQL keyword to use as a joiner.
   * @return The constructed SQL query.
   */
  private static String joinOperands(
      final NAryOperator operator,
      final Map<String, PseudoTag> pseudoTags,
      @Language(value = "sqlite", prefix = "(SELECT * FROM table) ", suffix = " (SELECT * FROM table)")
      String joinerKeyword
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
      query.add(toSql_(subQuery, pseudoTags));
    return query.toString();
  }

  @Override
  public String toString() {
    return this.formula.toString();
  }
}
