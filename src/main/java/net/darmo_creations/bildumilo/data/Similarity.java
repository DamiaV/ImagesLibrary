package net.darmo_creations.bildumilo.data;

/**
 * This class is a simple container for two hashes’ Hamming distance and similarity confidence index.
 *
 * @param hammingDistance The Hamming distance between two hashes.
 * @param confidence      The associated similarity confidence index.
 * @see Hash#computeSimilarity(Hash)
 */
public record Similarity(
    long hammingDistance,
    float confidence
) {
}
