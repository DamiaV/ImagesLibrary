package net.darmo_creations.imageslibrary.data.sql_functions;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class FileExistsFunctionTest extends FunctionTest<FileExistsFunction> {
  @Test
  void testFileExists() throws SQLException {
    try (final var statement = this.connection.createStatement();
         final var resultSet = statement.executeQuery("SELECT \"FILE_EXISTS\"('test_file.png')")) {
      resultSet.next();
      assertEquals(1, resultSet.getInt(1));
    }
  }

  @Test
  void testFileDoesNotExists() throws SQLException {
    try (final var statement = this.connection.createStatement();
         final var resultSet = statement.executeQuery("SELECT \"FILE_EXISTS\"('test_file_0.png')")) {
      resultSet.next();
      assertEquals(0, resultSet.getInt(1));
    }
  }

  @Override
  protected FileExistsFunction getFunction() {
    return new FileExistsFunction();
  }
}