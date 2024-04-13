package net.darmo_creations.imageslibrary.query_parser;

import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import org.junit.jupiter.api.*;
import org.logicng.formulas.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TagQueryParserTest {
  private static final FormulaFactory ff = new FormulaFactory();

  @Test
  void parse_tag() throws InvalidPseudoTagException {
    assertEquals(ff.variable("a"), TagQueryParser.parse("a", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_not() throws InvalidPseudoTagException {
    assertEquals(ff.not(ff.variable("a")), TagQueryParser.parse("-a", Map.of(), Map.of(), null).formula());
    assertEquals(ff.not(ff.variable("a")), TagQueryParser.parse("- a", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_notSimplifies() throws InvalidPseudoTagException {
    assertEquals(ff.variable("a"), TagQueryParser.parse("-(-a)", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_or() throws InvalidPseudoTagException {
    assertEquals(ff.or(ff.variable("a"), ff.variable("b")),
        TagQueryParser.parse("a + b", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_orSimplifies() throws InvalidPseudoTagException {
    assertEquals(ff.variable("a"), TagQueryParser.parse("a + a", Map.of(), Map.of(), null).formula());
    assertEquals(ff.verum(), TagQueryParser.parse("a + -a", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_and() throws InvalidPseudoTagException {
    assertEquals(ff.and(ff.variable("a"), ff.variable("b")),
        TagQueryParser.parse("a b", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_andSimplifies() throws InvalidPseudoTagException {
    assertEquals(ff.variable("a"), TagQueryParser.parse("a a", Map.of(), Map.of(), null).formula());
    assertEquals(ff.falsum(), TagQueryParser.parse("a -a", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_andPriorityOverOr() throws InvalidPseudoTagException {
    assertEquals(ff.or(ff.variable("a"), ff.and(ff.variable("b"), ff.variable("c"))),
        TagQueryParser.parse("a + b c", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_group() throws InvalidPseudoTagException {
    assertEquals(ff.and(ff.or(ff.variable("a"), ff.variable("b")), ff.variable("c")),
        TagQueryParser.parse("(a + b) c", Map.of(), Map.of(), null).formula());
  }

  @Test
  void parse_booleanPseudoTag() throws InvalidPseudoTagException {
    assertEquals(ff.variable("no_file:boolean"),
        TagQueryParser.parse("#no_file", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagStringNoFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:string::pattern"),
        TagQueryParser.parse("ext=\"pattern\"", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagStringIFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:string:i:pattern"),
        TagQueryParser.parse("ext=i\"pattern\"", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagStringSFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:string:s:pattern"),
        TagQueryParser.parse("ext=s\"pattern\"", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagRegexNoFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:regex::pattern"),
        TagQueryParser.parse("ext=/pattern/", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagRegexIFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:regex:i:pattern"),
        TagQueryParser.parse("ext=i/pattern/", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_pseudoTagRegexSFlags() throws InvalidPseudoTagException {
    assertEquals(ff.variable("ext:regex:s:pattern"),
        TagQueryParser.parse("ext=s/pattern/", Map.of(), DatabaseConnection.PSEUDO_TAGS, null).formula());
  }

  @Test
  void parse_invalidPseudoTag() {
    assertThrows(InvalidPseudoTagException.class,
        () -> TagQueryParser.parse("a=\"pattern\"", Map.of(), DatabaseConnection.PSEUDO_TAGS, null));
  }

  @Test
  void parse_invalidPseudoTagFlag() {
    assertThrows(InvalidPseudoTagException.class,
        () -> TagQueryParser.parse("ext=I\"pattern\"", Map.of(), DatabaseConnection.PSEUDO_TAGS, null));
  }

  @Test
  void parse_lexerErrorThrows() {
    assertThrows(TagQuerySyntaxErrorException.class,
        () -> TagQueryParser.parse("a$", Map.of(), Map.of(), null));
  }

  @Test
  void parse_parserErrorThrows() {
    assertThrows(TagQuerySyntaxErrorException.class,
        () -> TagQueryParser.parse("a-", Map.of(), Map.of(), null));
  }
}