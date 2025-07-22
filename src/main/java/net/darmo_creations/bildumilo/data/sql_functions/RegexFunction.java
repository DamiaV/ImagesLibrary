package net.darmo_creations.bildumilo.data.sql_functions;

import net.darmo_creations.bildumilo.data.*;

import java.sql.*;

/**
 * This function returns 1 if the given string matches the given Java-compatible regex, 0 otherwise.
 */
@SqlFunction(name = "REGEX", nArgs = 3, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class RegexFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final String string = this.value_text(0);
    final String pattern = this.value_text(1);
    final String flags = this.value_text(2);
    this.result(PatternPseudoTag.getPattern(pattern, flags).matcher(string).find() ? 1 : 0);
  }
}
