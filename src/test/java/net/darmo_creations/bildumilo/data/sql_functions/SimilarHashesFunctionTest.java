package net.darmo_creations.bildumilo.data.sql_functions;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.sql.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class SimilarHashesFunctionTest extends FunctionTest<SimilarHashesFunction> {
  static Stream<Arguments> provideHashes() {
    return Stream.of(
        Arguments.of(0b0000000000_1),
        Arguments.of(0b0000000001_1),
        Arguments.of(0b0000000011_1),
        Arguments.of(0b0000000111_1),
        Arguments.of(0b0000001111_1),
        Arguments.of(0b0000011111_1),
        Arguments.of(0b0000111111_1),
        Arguments.of(0b0001111111_1),
        Arguments.of(0b0011111111_1),
        Arguments.of(0b0111111111_1),
        Arguments.of(0b1111111111_1)
    );
  }

  @ParameterizedTest
  @MethodSource("provideHashes")
  void testFunctionHashDistanceLessThan11Returns1(int hash) throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "SIMILAR_HASHES"(%d, 1)
          """.formatted(hash))) {
        resultSet.next();
        assertEquals(1, resultSet.getInt(1));
      }
    }
  }

  @Test
  void testFunctionHashDistanceGreaterThan10Returns0() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "SIMILAR_HASHES"(4095, 1)
          """)) {
        resultSet.next();
        assertEquals(0, resultSet.getInt(1));
      }
    }
  }

  @Override
  protected SimilarHashesFunction getFunction() {
    return new SimilarHashesFunction();
  }
}