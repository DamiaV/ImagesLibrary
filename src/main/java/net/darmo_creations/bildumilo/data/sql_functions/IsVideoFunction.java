package net.darmo_creations.bildumilo.data.sql_functions;

import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.utils.*;

import java.nio.file.*;
import java.sql.*;

/**
 * This function returns 1 if the provided path’s file extension is in {@link App#VALID_VIDEO_EXTENSIONS},
 * false otherwise.
 */
@SqlFunction(name = "IS_VIDEO", nArgs = 1, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class IsVideoFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final String path = this.value_text(0);
    if (path == null) {
      this.result(0);
      return;
    }
    final String ext = FileUtils.getExtension(Path.of(path)).toLowerCase();
    this.result(App.VALID_VIDEO_EXTENSIONS.contains(ext) ? 1 : 0);
  }
}
