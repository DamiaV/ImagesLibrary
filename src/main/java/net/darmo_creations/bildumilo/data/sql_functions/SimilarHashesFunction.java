package net.darmo_creations.bildumilo.data.sql_functions;

import net.darmo_creations.bildumilo.data.*;

import java.sql.*;

/**
 * This function returns 1 if the provided hashes are similar, 0 otherwise.
 *
 * @see Hash#computeSimilarity(Hash)
 */
@SqlFunction(name = "SIMILAR_HASHES", nArgs = 2, flags = org.sqlite.Function.FLAG_DETERMINISTIC)
public class SimilarHashesFunction extends org.sqlite.Function {
  @Override
  protected void xFunc() throws SQLException {
    if (this.value_text(0) == null || this.value_text(1) == null) {
      this.result(0);
      return;
    }
    final Hash hash1 = new Hash(this.value_long(0));
    final Hash hash2 = new Hash(this.value_long(1));
    final var similarity = hash1.computeSimilarity(hash2);
    this.result(similarity.hammingDistance() <= Hash.SIM_DIST_THRESHOLD ? 1 : 0);
  }
}
