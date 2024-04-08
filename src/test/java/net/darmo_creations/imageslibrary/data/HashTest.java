package net.darmo_creations.imageslibrary.data;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class HashTest {
  static Stream<Arguments> provideHashes() {
    return Stream.of(
        Arguments.of(0b0000000000_1, 0),
        Arguments.of(0b0000000001_1, 1),
        Arguments.of(0b0000000011_1, 2),
        Arguments.of(0b0000000111_1, 3),
        Arguments.of(0b0000001111_1, 4),
        Arguments.of(0b0000011111_1, 5),
        Arguments.of(0b0000111111_1, 6),
        Arguments.of(0b0001111111_1, 7),
        Arguments.of(0b0011111111_1, 8),
        Arguments.of(0b0111111111_1, 9),
        Arguments.of(0b1111111111_1, 10)
    );
  }

  @ParameterizedTest
  @MethodSource("provideHashes")
  void computeSimilarity_isSymetric(int hash, int ignored) {
    final Hash hash1 = new Hash(1);
    final Hash hash2 = new Hash(hash);
    assertEquals(
        hash1.computeSimilarity(hash2).hammingDistance(),
        hash2.computeSimilarity(hash1).hammingDistance()
    );
    assertEquals(
        hash1.computeSimilarity(hash2).confidence(),
        hash2.computeSimilarity(hash1).confidence()
    );
  }

  @ParameterizedTest
  @MethodSource("provideHashes")
  void computeSimilarity_distance(int hash, int expectedDist) {
    final Hash hash1 = new Hash(1);
    final Hash hash2 = new Hash(hash);
    assertEquals(expectedDist, hash1.computeSimilarity(hash2).hammingDistance());
  }

  @Test
  void computeSimilarity_confidenceIsNot1WhenHashesAreEqual() {
    final Hash hash1 = new Hash(1);
    final Hash hash2 = new Hash(1);
    assertNotEquals(1, hash1.computeSimilarity(hash2).confidence());
  }

  @ParameterizedTest
  @MethodSource("provideHashes")
  void computeSimilarity_confidenceIsNot0WhenDistanceIsLessThan11(int hash, int ignored) {
    final Hash hash1 = new Hash(1);
    final Hash hash2 = new Hash(hash);
    assertNotEquals(0, hash1.computeSimilarity(hash2).confidence());
  }

  @Test
  void computeSimilarity_confidenceIs0WhenDistanceIsGreaterThan10() {
    final Hash hash1 = new Hash(1);
    final Hash hash2 = new Hash(4095);
    assertEquals(0, hash1.computeSimilarity(hash2).confidence());
  }

  @Test
  void computeForFile_identicalForSameFile() throws IOException {
    final Path path = Path.of("test_file.png");
    final Hash hash1 = Hash.computeForFile(path);
    final Hash hash2 = Hash.computeForFile(path);
    assertEquals(hash1, hash2);
  }

  @Test
  void computeForFile_differentForDifferentImages() throws IOException {
    final Hash hash1 = Hash.computeForFile(Path.of("test_file.png"));
    final Hash hash2 = Hash.computeForFile(Path.of("test_file_2.png"));
    assertNotEquals(hash1, hash2);
  }
}