package net.darmo_creations.bildumilo.data.sql_functions;

import java.nio.file.*;
import java.sql.*;

/**
 * This function returns 1 if the file represented by the given path exists, 0 otherwise.
 */
@SqlFunction(name = "FILE_EXISTS", nArgs = 1)
public class FileExistsFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final String s = this.value_text(0);
    if (s == null)
      this.result(0);
    else
      try {
        this.result(Files.exists(Path.of(s)) ? 1 : 0);
      } catch (final SecurityException e) {
        this.result(0);
      }
  }
}
