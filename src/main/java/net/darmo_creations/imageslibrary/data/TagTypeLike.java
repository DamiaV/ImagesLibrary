package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

/**
 * Base interface for classes representing tag types.
 */
public interface TagTypeLike extends DatabaseElement {
  /**
   * This tag type’s label.
   */
  String label();

  /**
   * This tag type’s symbol.
   */
  char symbol();

  /**
   * This tag type’s RGB color.
   */
  int color();

  /**
   * Raise an exception if the given tag type label is invalid.
   * A label is considered invalid if it only contains whitespace characters.
   *
   * @param label The label to check.
   * @throws IllegalArgumentException If the label is invalid.
   */
  static void ensureValidLabel(@NotNull String label) {
    if (!isLabelValid(label))
      throw new IllegalArgumentException("Invalid label: " + label);
  }

  /**
   * Check whether the given tag type label is valid.
   *
   * @param label The label to check.
   * @return True if the label is valid, false otherwise.
   */
  static boolean isLabelValid(@NotNull String label) {
    return !label.isBlank();
  }

  /**
   * Raise an exception if the given tag type symbol is invalid.
   * A symbol is considered invalid if it is an underscore '_' or
   * is not in any of the following Unicode General Categories:
   * Pc, Pd, Ps, Pe, Pi, Pf, Po, Sm, Sc, So.
   * <p>
   * Cf. https://www.unicode.org/versions/Unicode15.1.0/ch04.pdf#G124142
   *
   * @param symbol The symbol to check.
   * @throws IllegalArgumentException If the symbol is invalid.
   */
  static void ensureValidSymbol(char symbol) {
    if (!isSymbolValid(symbol))
      throw new IllegalArgumentException("Invalid symbol: " + symbol);
  }

  /**
   * Check whether the given tag type symbol is valid.
   *
   * @param symbol The symbol to check.
   * @return True if the symbol is valid, false otherwise.
   */
  static boolean isSymbolValid(char symbol) {
    return symbol != '_' && String.valueOf(symbol).matches("[\\p{IsPc}\\p{IsPd}\\p{IsPs}\\p{IsPe}\\p{IsPi}\\p{IsPf}\\p{IsPo}\\p{IsSm}\\p{IsSc}\\p{IsSo}]");
  }
}
