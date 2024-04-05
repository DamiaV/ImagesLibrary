package net.darmo_creations.imageslibrary.data.sql_functions;

import org.jetbrains.annotations.*;

import java.sql.*;
import java.util.regex.*;

/**
 * This function returns 1 if the given string matches the given Java-compatible regex, 0 otherwise.
 */
@SqlFunction(name = "REGEX", nArgs = 3, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class RegexFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final String string = this.value_text(0);
    final String pattern = this.value_text(1);
    @Nullable
    final String flag = this.value_text(2);
    final boolean caseSensitive = "s".equals(flag);
    final Pattern regex = Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
    this.result(regex.matcher(string).matches() ? 1 : 0);
  }
}
