package net.darmo_creations.imageslibrary.data.sql_functions;

import net.darmo_creations.imageslibrary.data.*;

import java.sql.*;

/**
 * This function computes the similarity confidence index between two hashes.
 *
 * @see Hash#computeSimilarity(Hash)
 */
@SqlFunction(name = "SIMILARITY_CONFIDENCE", nArgs = 2, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class HashesSimilarityFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    final Hash blob1 = new Hash(this.value_long(0));
    final Hash blob2 = new Hash(this.value_long(1));
    final var similarity = blob1.computeSimilarity(blob2);
    this.result(similarity.confidence());
  }
}
