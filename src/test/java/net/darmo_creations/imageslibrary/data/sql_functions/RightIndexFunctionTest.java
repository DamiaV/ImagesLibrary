package net.darmo_creations.imageslibrary.data.sql_functions;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class RightIndexFunctionTest extends FunctionTest<RightIndexFunction> {
  @Test
  void testFunctionSubstringFoundIndexFrom1() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "RINSTR"('test', 't')
          """)) {
        resultSet.next();
        assertEquals(4, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testFunctionSubstringNotFoundReturns0() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "RINSTR"('test', 'b')
          """)) {
        resultSet.next();
        assertEquals(0, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testFunctionFirstArgNullReturnsNull() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "RINSTR"(NULL, 'o') IS NULL
          """)) {
        resultSet.next();
        assertEquals(1, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testFunctionSecondArgNullReturnsNull() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "RINSTR"('test', NULL) IS NULL
          """)) {
        resultSet.next();
        assertEquals(1, resultSet.getInt(1));
      }
    }
  }

  @Test
  void tooFewArgsError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "RINSTR"('test20')
          """));
    }
  }

  @Test
  void tooManyArgsError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "RINSTR"('test20', 't', 'extra')
          """));
    }
  }

  @Override
  protected RightIndexFunction getFunction() {
    return new RightIndexFunction();
  }
}