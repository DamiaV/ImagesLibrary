package net.darmo_creations.imageslibrary.data.sql_functions;

import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class RegexFunctionTest extends FunctionTest<RegexFunction> {
  @Test
  void testCaseInsensitive() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "REGEX"('TeSt20', '^test\\d+', 'i')
          """)) {
        resultSet.next();
        assertEquals(1, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testCaseSensitive() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "REGEX"('TeSt20', '^test\\d+', 's')
          """)) {
        resultSet.next();
        assertEquals(0, resultSet.getInt(1));
      }
      try (final var resultSet = statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+', 's')
          """)) {
        resultSet.next();
        assertEquals(1, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testInvalidFlagError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+', 'ia')
          """));
    }
  }

  @Test
  void testMissingCaseFlagError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+', '')
          """));
    }
  }

  @Test
  void testMissingBothCaseFlagsError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+', 'is')
          """));
    }
  }

  @Test
  void tooFewArgsError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+')
          """));
    }
  }

  @Test
  void tooManyArgsError() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      assertThrows(SQLException.class, () -> statement.executeQuery("""
          SELECT "REGEX"('test20', '^test\\d+', 's', 'extra')
          """));
    }
  }

  @Override
  protected RegexFunction getFunction() {
    return new RegexFunction();
  }
}