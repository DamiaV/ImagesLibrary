package net.darmo_creations.bildumilo.data.sql_functions;

import java.sql.*;

/**
 * This function returns the index of the second string in the first string, starting from the right.
 * If either argument is null, this function returns null.
 */
@SqlFunction(name = "RINSTR", nArgs = 2, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class RightIndexFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final String string = this.value_text(0);
    final String substring = this.value_text(1);
    if (string == null || substring == null)
      this.result((byte[]) null); // Same behavior as INSTR function
    else
      this.result(string.lastIndexOf(substring) + 1); // SQLite string indices start from 1
  }
}
