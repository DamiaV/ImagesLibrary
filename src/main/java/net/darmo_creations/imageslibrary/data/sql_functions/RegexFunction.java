package net.darmo_creations.imageslibrary.data.sql_functions;

import net.darmo_creations.imageslibrary.data.*;

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
    final String flag = this.value_text(2);
    System.out.println(string + " " + pattern + " " + flag);
    if (flag == null || !flag.contains(String.valueOf(PseudoTag.CASE_SENSITIVE_FLAG))
                        && !flag.contains(String.valueOf(PseudoTag.CASE_INSENSITIVE_FLAG)))
      throw new SQLException("Missing case sensitivity flag");
    if (flag.contains(String.valueOf(PseudoTag.CASE_SENSITIVE_FLAG))
        && flag.contains(String.valueOf(PseudoTag.CASE_INSENSITIVE_FLAG)))
      throw new SQLException("Both case sensitivity flags present");
    boolean caseSensitive = true;
    for (int i = 0; i < flag.length(); i++) {
      final char c = flag.charAt(i);
      switch (c) {
        case PseudoTag.CASE_SENSITIVE_FLAG -> caseSensitive = true;
        case PseudoTag.CASE_INSENSITIVE_FLAG -> caseSensitive = false;
        default -> throw new SQLException("Invalid regex flag: " + c);
      }
    }
    final Pattern regex = Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
    this.result(regex.matcher(string).find() ? 1 : 0);
  }
}
