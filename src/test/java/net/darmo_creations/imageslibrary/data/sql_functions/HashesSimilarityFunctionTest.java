package net.darmo_creations.imageslibrary.data.sql_functions;

import net.darmo_creations.imageslibrary.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.sql.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class HashesSimilarityFunctionTest extends FunctionTest<HashesSimilarityFunction> {
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
  void testFunctionHashDistanceLessThan11ReturnsSameResultAsHashComputeSimilarity(int hash) throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "SIMILARITY_CONFIDENCE"(%d, 1)
          """.formatted(hash))) {
        resultSet.next();
        assertEquals(
            new Hash(1).computeSimilarity(new Hash(hash)).confidence(),
            resultSet.getFloat(1),
            1e-6f
        );
      }
    }
  }

  @Test
  void testFunctionHashDistanceGreaterThan10Returns0() throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      try (final var resultSet = statement.executeQuery("""
          SELECT "SIMILARITY_CONFIDENCE"(4095, 1)
          """)) {
        resultSet.next();
        assertEquals(0, resultSet.getInt(1));
      }
    }
  }

  @Override
  protected HashesSimilarityFunction getFunction() {
    return new HashesSimilarityFunction();
  }
}