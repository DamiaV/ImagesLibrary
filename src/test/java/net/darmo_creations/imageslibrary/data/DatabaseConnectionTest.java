package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.ui.*;
import org.junit.jupiter.api.*;
import org.logicng.formulas.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {
  private DatabaseConnection db;

  // region before/after each

  @BeforeEach
  void setUp() throws DatabaseOperationException, IOException {
    this.db = new DatabaseConnection(null);
    // Reset test files
    final Path path = Path.of("test_file_3.png");
    final Path path1 = Path.of("test_file.png");
    if (Files.exists(path))
      Files.move(path, path1);
    final Path path2 = Path.of("dest", "test_file.png");
    if (Files.exists(path2))
      Files.move(path2, path1);
    final Path path3 = Path.of("test_file_2.png");
    if (!Files.exists(path3))
      Files.copy(Path.of("dest", "test_file_2.png"), path3);
  }

  @AfterEach
  void tearDown() throws DatabaseOperationException {
    this.db.close();
  }

  // endregion
  // region insertTagTypes

  @Test
  void insertTagTypes() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '$', 1),
        new TagTypeUpdate(0, "test2", '/', 2)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tt = this.db.getAllTagTypes().stream()
        .filter(t -> t.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = tt.id(), id2 = id1 == 1 ? 2 : 1;
    assertEquals(Set.of(
        new TagType(id1, "test1", '$', 1),
        new TagType(id2, "test2", '/', 2)
    ), this.db.getAllTagTypes());
    assertEquals(Map.of(
        id1, 0,
        id2, 0
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertTagTypes_duplicateLabelsError() {
    assertThrows(DatabaseOperationException.class, () -> this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '$', 1),
        new TagTypeUpdate(0, "test1", '/', 2)
    )));
    assertTrue(this.db.getAllTagTypes().isEmpty());
    assertTrue(this.db.getAllTagTypesCounts().isEmpty());
  }

  @Test
  void insertTagTypes_duplicateSymbolsError() {
    assertThrows(DatabaseOperationException.class, () -> this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 1),
        new TagTypeUpdate(0, "test2", '/', 2)
    )));
    assertTrue(this.db.getAllTagTypes().isEmpty());
    assertTrue(this.db.getAllTagTypesCounts().isEmpty());
  }

  // endregion
  // region updateTagTypes

  @Test
  void updateTagTypes_label() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(1, "test2", '/', 0)
    ));
    assertEquals(Set.of(
        new TagType(1, "test2", '/', 0)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_symbol() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(1, "test1", '$', 0)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '$', 0)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_color() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(1, "test1", '/', 1)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '/', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_swappedLabels() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tt = this.db.getAllTagTypes().stream()
        .filter(tagType -> tagType.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = tt.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(id1, "test2", '/', 0),
        new TagTypeUpdate(id2, "test1", '$', 1)
    ));
    assertEquals(Set.of(
        new TagType(id1, "test2", '/', 0),
        new TagType(id2, "test1", '$', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_swappedSymbols() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tt = this.db.getAllTagTypes().stream()
        .filter(tagType -> tagType.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = tt.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(id1, "test1", '$', 0),
        new TagTypeUpdate(id2, "test2", '/', 1)
    ));
    assertEquals(Set.of(
        new TagType(id1, "test1", '$', 0),
        new TagType(id2, "test2", '/', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_duplicateLabelsError() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tt = this.db.getAllTagTypes().stream()
        .filter(tagType -> tagType.label().equals("test1"))
        .findFirst()
        .get();
    assertThrows(DatabaseOperationException.class, () -> this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(tt.id(), "test2", '/', 0)
    )));
  }

  @Test
  void updateTagTypes_duplicateSymbolsError() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tt = this.db.getAllTagTypes().stream()
        .filter(tagType -> tagType.label().equals("test1"))
        .findFirst()
        .get();
    assertThrows(DatabaseOperationException.class, () -> this.db.updateTagTypes(Set.of(
        new TagTypeUpdate(tt.id(), "test1", '$', 0)
    )));
  }

  @Test
  void updateTagTypes_notInDbError() {
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateTagTypes(Set.of(new TagTypeUpdate(1, "test", '/', 0))));
  }

  // endregion
  // region deleteTagTypes

  @Test
  void deleteTagTypes() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "test", '/', 0)));
    assertEquals(
        Set.of(new TagType(1, "test", '/', 0)),
        this.db.getAllTagTypes()
    );
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
    this.db.deleteTagTypes(this.db.getAllTagTypes());
    assertTrue(this.db.getAllTagTypes().isEmpty());
    assertTrue(this.db.getAllTagTypesCounts().isEmpty());
  }

  @Test
  void deleteTagTypes_setsTagTypeToNull() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test", tagType, null)
    ));
    this.db.deleteTagTypes(this.db.getAllTagTypes());
    //noinspection OptionalGetWithoutIsPresent
    final Tag tag = this.db.getAllTags().stream().findFirst().get();
    assertTrue(tag.type().isEmpty());
  }

  @Test
  void deleteTagTypes_notInDbErrorDoesNotUpdateCache() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "test", '/', 0)));
    assertFalse(this.db.getAllTagTypes().isEmpty());
    assertFalse(this.db.getAllTagTypesCounts().isEmpty());
    assertThrows(DatabaseOperationException.class,
        () -> this.db.deleteTagTypes(Set.of(new TagType(2, "test1", '$', 0))));
    assertFalse(this.db.getAllTagTypes().isEmpty());
    assertFalse(this.db.getAllTagTypesCounts().isEmpty());
  }

  @Test
  void deleteTagTypes_notInDbError() {
    assertThrows(DatabaseOperationException.class,
        () -> this.db.deleteTagTypes(Set.of(new TagType(1, "test", '/', 0))));
  }

  // endregion
  // region insertTags

  @Test
  void insertTags() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null)
    ), this.db.getAllTags());
    assertEquals(Map.of(
        1, 0
    ), this.db.getAllTagsCounts());
  }

  @Test
  void insertTags_updatesTagTypesCounts() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Map.of(
        1, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertTags_duplicateLabelsError() {
    assertThrows(DatabaseOperationException.class, () -> this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test1", null, null)
    )));
    assertTrue(this.db.getAllTags().isEmpty());
    assertTrue(this.db.getAllTagsCounts().isEmpty());
  }

  // endregion
  // region updateTags

  @Test
  void updateTags_labels() throws DatabaseOperationException {
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null)
    ));
    this.db.updateTags(Set.of(
        new TagUpdate(1, "test2", null, null)
    ));
    assertEquals(Set.of(
        new Tag(1, "test2", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_types() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType, null),
        new TagUpdate(0, "test2", null, null)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final Tag t = this.db.getAllTags().stream()
        .filter(t_ -> t_.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = t.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateTags(Set.of(
        new TagUpdate(id1, "test1", null, null),
        new TagUpdate(id2, "test2", tagType, null)
    ));
    assertEquals(Set.of(
        new Tag(id1, "test1", null, null),
        new Tag(id2, "test2", tagType, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_nullToNonNull() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null)
    ));
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
    this.db.updateTags(Set.of(
        new TagUpdate(1, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_NonNullToNull() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.updateTags(Set.of(
        new TagUpdate(1, "test1", null, null)
    ));
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_NonNullToNonNull() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType1 = this.db.getAllTagTypes().stream().filter(t -> t.id() == 1).findFirst().get();
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType2 = this.db.getAllTagTypes().stream().filter(t -> t.id() == 2).findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType1, null)
    ));
    assertEquals(Map.of(
        tagType1.id(), 1,
        tagType2.id(), 0
    ), this.db.getAllTagTypesCounts());
    this.db.updateTags(Set.of(
        new TagUpdate(1, "test1", tagType2, null)
    ));
    assertEquals(Map.of(
        tagType1.id(), 0,
        tagType2.id(), 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesSameDoesNotUpdateTagTypesCounts() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.updateTags(Set.of(
        new TagUpdate(1, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_definitions() throws DatabaseOperationException {
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, "a b"),
        new TagUpdate(0, "test2", null, null)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final Tag t = this.db.getAllTags().stream()
        .filter(t_ -> t_.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = t.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateTags(Set.of(
        new TagUpdate(id1, "test1", null, null),
        new TagUpdate(id2, "test2", null, "c d")
    ));
    assertEquals(Set.of(
        new Tag(id1, "test1", null, null),
        new Tag(id2, "test2", null, "c d")
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_swappedLabels() throws DatabaseOperationException {
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final Tag tt = this.db.getAllTags().stream()
        .filter(t -> t.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = tt.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateTags(Set.of(
        new TagUpdate(id1, "test2", null, null),
        new TagUpdate(id2, "test1", null, null)
    ));
    assertEquals(Set.of(
        new Tag(id1, "test2", null, null),
        new Tag(id2, "test1", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_duplicateLabelsError() throws DatabaseOperationException {
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final Tag tt = this.db.getAllTags().stream()
        .filter(t -> t.label().equals("test1"))
        .findFirst()
        .get();
    assertThrows(DatabaseOperationException.class, () -> this.db.updateTags(Set.of(
        new TagUpdate(tt.id(), "test2", null, null)
    )));
  }

  @Test
  void updateTags_addDefinitionButAlreadyLinkedToMediaError() throws DatabaseOperationException {
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null)
    ));
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file.png"), Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1")
    ), Set.of()));
    assertThrows(DatabaseOperationException.class, () -> this.db.updateTags(Set.of(
        new TagUpdate(1, "test1", null, "c d")
    )));
  }

  @Test
  void updateTags_notInDbError() {
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateTags(Set.of(new TagUpdate(1, "test", null, null))));
  }

  // endregion
  // region deleteTags

  @Test
  void deleteTags() throws DatabaseOperationException {
    this.db.insertTags(Set.of(new TagUpdate(0, "test", null, null)));
    assertEquals(
        Set.of(new Tag(1, "test", null, null)),
        this.db.getAllTags()
    );
    assertEquals(Map.of(1, 0), this.db.getAllTagsCounts());
    this.db.deleteTags(this.db.getAllTags());
    assertTrue(this.db.getAllTags().isEmpty());
    assertTrue(this.db.getAllTagsCounts().isEmpty());
  }

  @Test
  void deleteTags_updatesTagTypesCounts() throws DatabaseOperationException {
    this.db.insertTagTypes(Set.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.deleteTags(this.db.getAllTags());
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
  }

  @Test
  void deleteTags_notInDbErrorDoesNotUpdateCache() throws DatabaseOperationException {
    this.db.insertTags(Set.of(new TagUpdate(0, "test", null, null)));
    assertFalse(this.db.getAllTags().isEmpty());
    assertFalse(this.db.getAllTagsCounts().isEmpty());
    assertThrows(DatabaseOperationException.class,
        () -> this.db.deleteTags(Set.of(new Tag(2, "test1", null, null))));
    assertFalse(this.db.getAllTags().isEmpty());
    assertFalse(this.db.getAllTagsCounts().isEmpty());
  }

  @Test
  void deleteTags_notInDbError() {
    assertThrows(DatabaseOperationException.class,
        () -> this.db.deleteTags(Set.of(new Tag(1, "test", null, null))));
  }

  // endregion
  // region queryMedias

  private FormulaFactory initQueryMediasTest() throws DatabaseOperationException {
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file.jpeg"), Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file_2.jpg"), Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test3")
    ), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file_3.png"), Optional.of(new Hash(-1)), Set.of(), Set.of()));
    return new FormulaFactory();
  }

  @Test
  void queryMedias_trueReturnsAll() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.verum(), Map.of(), null));
    assertEquals(3, medias.size());
  }

  @Test
  void queryMedias_falseReturnsNone() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.falsum(), Map.of(), null));
    assertTrue(medias.isEmpty());
  }

  @Test
  void queryMedias_or() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.or(ff.variable("test2"), ff.variable("test3")), Map.of(), null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_and() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.and(ff.variable("test1"), ff.variable("test2")), Map.of(), null));
    assertEquals(1, medias.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new MediaFile(1, Path.of("test_file.jpeg"), new Hash(0)), medias.stream().findFirst().get());
  }

  @Test
  void queryMedias_not() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.not(ff.variable("test2")), Map.of(), null));
    assertEquals(2, medias.size());
    assertEquals(Set.of(
        new MediaFile(2, Path.of("test_file_2.jpg"), new Hash(1)),
        new MediaFile(3, Path.of("test_file_3.png"), new Hash(-1))
    ), medias);
  }

  @Test
  void queryMedias_pseudoTag_extPlainString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("ext:string::jpeg"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(1, medias.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new MediaFile(1, Path.of("test_file.jpeg"), new Hash(0)), medias.stream().findFirst().get());
  }

  @Test
  void queryMedias_pseudoTag_extTemplateString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("ext:string::jp?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_extRegex() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("ext:regex::jp[e]?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_namePlainString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("name:string::test_file.jpeg"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(1, medias.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new MediaFile(1, Path.of("test_file.jpeg"), new Hash(0)), medias.stream().findFirst().get());
  }

  @Test
  void queryMedias_pseudoTag_nameTemplateString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("name:string::test_file*.jp?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_nameRegex() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("name:regex::test_file.*\\.jpe?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_pathPlainString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("path:string::%s/test_file.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(1, medias.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new MediaFile(1, Path.of("test_file.jpeg"), new Hash(0)), medias.stream().findFirst().get());
  }

  @Test
  void queryMedias_pseudoTag_pathTemplateString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("path:string::*/test_file*.jp?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_pathRegex() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("path:regex::.*/test_file.*\\.jpe?g"), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_similar_toPlainString() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("similar_to:string::%s/test_file.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS, null));
    assertEquals(2, medias.size());
  }

  @Test
  void queryMedias_pseudoTag_similar_toTemplateStringNoError() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(
        new TagQuery(ff.variable("similar_to:string::%s/test_file.jp?g".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS, null));
    assertTrue(medias.isEmpty());
  }

  @Test
  void queryMedias_pseudoTag_similar_toRegexError() throws DatabaseOperationException {
    final var ff = this.initQueryMediasTest();
    assertThrows(InvalidPseudoTagException.class,
        () -> this.db.queryMedias(new TagQuery(ff.variable("similar_to:regex::%s/test_file\\.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS, null)));
  }

  @Test
  void queryMedias_nonExistentTagNoError() throws DatabaseOperationException, InvalidPseudoTagException {
    final var ff = this.initQueryMediasTest();
    final var medias = this.db.queryMedias(new TagQuery(ff.variable("yo"), Map.of(), null));
    assertTrue(medias.isEmpty());
  }

  @Test
  void queryMedias_nonExistentPseudoTagError() throws DatabaseOperationException {
    final var ff = this.initQueryMediasTest();
    assertThrows(InvalidPseudoTagException.class, () -> this.db.queryMedias(new TagQuery(ff.variable("invalid:string::a"), Map.of(), null)));
  }

  // endregion
  // region getMediaTags

  @Test
  void getMediaTags() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    final Set<Tag> mediaTags = this.db.getMediaTags(new MediaFile(1, path, new Hash(0)));
    assertEquals(this.db.getAllTags(), mediaTags);
  }

  // endregion
  // region isFileRegistered

  @Test
  void isFileRegistered() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void isFileRegistered_not() throws DatabaseOperationException {
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file.png"), Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertFalse(this.db.isFileRegistered(Path.of("test_file_2.png")));
  }

  // endregion
  // region getSimilarImages

  @Test
  void getSimilarImages() throws DatabaseOperationException {
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file.png"), Optional.of(new Hash(0)), Set.of(), Set.of()));
    final Path path = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(-1)), Set.of(), Set.of()));
    final var similarImages = this.db.getSimilarImages(new Hash(-1), null);
    assertEquals(1, similarImages.size());
    assertEquals(new MediaFile(2, path, new Hash(-1)), similarImages.get(0).getKey());
  }

  @Test
  void getSimilarImages_returnsSameConfidenceIndexAsHashClass() throws DatabaseOperationException {
    this.db.insertMedia(new MediaFileUpdate(0, Path.of("test_file.png"), Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertEquals(
        new Hash(0).computeSimilarity(new Hash(0)).confidence(),
        this.db.getSimilarImages(new Hash(0), null).get(0).getValue(),
        1e-6f
    );
  }

  // endregion
  // region insertMedia

  @Test
  void insertMedia() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void insertMedia_addsTagsToMedia() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final Tag t = this.db.getAllTags().stream()
        .filter(t_ -> t_.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = t.id(), id2 = id1 == 1 ? 2 : 1;
    assertEquals(Set.of(
        new Tag(id1, "test1", null, null),
        new Tag(id2, "test2", null, null)
    ), this.db.getMediaTags(new MediaFile(1, path, new Hash(0))));
  }

  @Test
  void insertMedia_createsTags() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.of(tagType), "test1")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null)
    ), this.db.getAllTags());
  }

  @Test
  void insertMedia_updatesTagCounts() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1,
        2, 1
    ), this.db.getAllTagsCounts());
  }

  @Test
  void insertMedia_updatesTagTypeCounts() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.of(tagType), "test1")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertMedia_tagsToRemoveNotEmptyError() {
    final Path path = Path.of("test_file.png");
    assertThrows(IllegalArgumentException.class,
        () -> this.db.insertMedia(
            new MediaFileUpdate(0, path, Hash.computeForFile(path), Set.of(),
                Set.of(new Tag(1, "test", null, null)))));
  }

  @Test
  void insertMedia_undefinedTagTypeError() {
    final Path path = Path.of("test_file.png");
    final TagType tagType = new TagType(0, "test", '/', 0);
    assertThrows(DatabaseOperationException.class,
        () -> this.db.insertMedia(new MediaFileUpdate(0, path, Hash.computeForFile(path), Set.of(
            new ParsedTag(Optional.of(tagType), "test1")
        ), Set.of()))
    );
  }

  @Test
  void insertMedia_addTagsWithDefinitionsError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, "a b")
    ));
    assertThrows(DatabaseOperationException.class,
        () -> this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
            new ParsedTag(Optional.empty(), "test1")
        ), Set.of()))
    );
  }

  @Test
  void insertMedia_duplicatePathsError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class,
        () -> this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()))
    );
  }

  @Test
  void insertMedia_duplicateTagNamesError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    assertThrows(DatabaseOperationException.class,
        () -> this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
            new ParsedTag(Optional.of(tagType), "test"),
            new ParsedTag(Optional.empty(), "test")
        ), Set.of()))
    );
  }

  @Test
  void insertMedia_errorRollbacksEverything() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    assertThrows(DatabaseOperationException.class,
        () -> this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
            new ParsedTag(Optional.of(tagType), "test"),
            new ParsedTag(Optional.empty(), "test")
        ), Set.of()))
    );
    assertTrue(this.db.getAllTags().isEmpty());
    assertFalse(this.db.isFileRegistered(path));
  }

  // endregion
  // region updateMedia

  @Test
  void updateMedia_hash() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var media = this.getAllMedias()
        .stream().findFirst().get();
    assertEquals(Optional.of(new Hash(1)), media.hash());
  }

  @Test
  void updateMedia_addsTagsToMedia() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final Tag t = this.db.getAllTags().stream()
        .filter(t_ -> t_.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = t.id(), id2 = id1 == 1 ? 2 : 1;
    assertEquals(Set.of(
        new Tag(id1, "test1", null, null),
        new Tag(id2, "test2", null, null)
    ), this.db.getMediaTags(new MediaFile(1, path, new Hash(0))));
  }

  @Test
  void updateMedia_removesTagsFromMedia() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final Tag t = this.db.getAllTags().stream()
        .filter(t_ -> t_.label().equals("test1"))
        .findFirst()
        .get();
    final int id1 = t.id(), id2 = id1 == 1 ? 2 : 1;
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(), Set.of(
        new Tag(id1, "test1", null, null),
        new Tag(id2, "test2", null, null)
    )));
    assertTrue(this.db.getMediaTags(new MediaFile(1, path, new Hash(0))).isEmpty());
  }

  @Test
  void updateMedia_createsTags() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.of(tagType), "test1")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateMedia_updatesTagCounts() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.empty(), "test1"),
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1,
        2, 1
    ), this.db.getAllTagsCounts());
  }

  @Test
  void updateMedia_updatesTagTypeCounts() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.of(tagType), "test")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateMedia_undefinedTagTypeError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final TagType tagType = new TagType(0, "test", '/', 0);
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class, () -> this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.of(tagType), "test")
    ), Set.of())));
  }

  @Test
  void updateMedia_pathDoesNotImpactFile() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    final Path path2 = Path.of("test_file_10.png");
    this.db.updateMedia(new MediaFileUpdate(1, path2, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertFalse(this.db.isFileRegistered(path));
    assertTrue(this.db.isFileRegistered(path2));
    assertTrue(Files.exists(path));
    assertFalse(Files.exists(path2));
  }

  @Test
  void updateMedia_duplicateTagNamesError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(0)), Set.of(
            new ParsedTag(Optional.of(tagType), "test"),
            new ParsedTag(Optional.empty(), "test")
        ), Set.of()))
    );
  }

  @Test
  void updateMedia_addTagsWithDefinitionsError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(Set.of(
        new TagUpdate(0, "test1", null, "a b")
    ));
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(0)), Set.of(
            new ParsedTag(Optional.empty(), "test1")
        ), Set.of()))
    );
  }

  @Test
  void updateMedia_errorRollbacksEverything() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(Set.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(1)), Set.of(
            new ParsedTag(Optional.of(tagType), "test"),
            new ParsedTag(Optional.empty(), "test")
        ), Set.of()))
    );
    assertTrue(this.db.getAllTags().isEmpty());
    //noinspection OptionalGetWithoutIsPresent
    final var media = this.getAllMedias()
        .stream().findFirst().get();
    assertEquals(Optional.of(new Hash(0)), media.hash());
  }

  @Test
  void updateMedia_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationException.class,
        () -> this.db.updateMedia(new MediaFileUpdate(1, path, Optional.of(new Hash(0)), Set.of(), Set.of()))
    );
  }

  // endregion
  // region moveOrRenameMedia rename only

  @Test
  void renameMedia_updatesPath() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("test_file_3.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void renameMedia_renamesFile() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("test_file_3.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    assertTrue(Files.exists(targetPath));
  }

  @Test
  void renameMedia_worksIfFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_0.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("test_file_3.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void renameMedia_worksIfTargetExistsButFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_0.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("test_file_2.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void renameMedia_targetFileExistsError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationException.class,
        () -> this.db.moveOrRenameMedia(new MediaFile(1, path, new Hash(0)), Path.of("test_file_2.png"), false));
  }

  @Test
  void renameMedia_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationException.class,
        () -> this.db.moveOrRenameMedia(new MediaFile(1, path, new Hash(0)), Path.of("test_file_3.png"), false));
  }

  // endregion
  // region moveOrrenameMedia move

  @Test
  void moveMedia_updatesPath() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("dest", "test_file.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void moveMedia_movesFile() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("dest", "test_file.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    assertTrue(Files.exists(targetPath));
  }

  @Test
  void moveMedia_worksIfFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_0.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("dest", "test_file_0.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void moveMedia_worksIfTargetExistsButFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_0.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllMedias().stream().findFirst().get();
    final Path targetPath = Path.of("dest", "test_file_0.png");
    this.db.moveOrRenameMedia(pic, targetPath, false);
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllMedias().stream().findFirst().get();
    assertEquals(targetPath.toAbsolutePath(), pic.path());
  }

  @Test
  void moveMedia_targetFileExistsError() {
    final Path path = Path.of("test_file_2.png");
    assertThrows(DatabaseOperationException.class,
        () -> this.db.moveOrRenameMedia(new MediaFile(1, path, new Hash(0)), Path.of("dest", "test_file_2"), false));
  }

  @Test
  void moveMedia_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationException.class,
        () -> this.db.moveOrRenameMedia(new MediaFile(1, path, new Hash(0)), Path.of("dest", "test_file.png"), false));
  }

  // endregion
  // region mergeMedias

  @Test
  void mergeMedias_mergesTags() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test1")
    ), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.empty(), "test2")
    ), Set.of()));
    this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(2, path1, new Hash(0)),
        false
    );
    final var mediaTags = this.db.getMediaTags(new MediaFile(2, path1, new Hash(0)));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, null)
    ), mediaTags);
  }

  @Test
  void mergeMedias_deletesFirstMediaEntry() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(2, path1, new Hash(0)),
        false
    );
    assertFalse(this.db.isFileRegistered(path));
    assertTrue(this.db.isFileRegistered(path1));
  }

  @Test
  void mergeMedias_deletesFirstMediaFileIfRequested() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    this.db.mergeMedias(
        new MediaFile(2, path1, new Hash(0)),
        new MediaFile(1, path, new Hash(0)),
        true
    );
    assertTrue(Files.exists(path));
    assertFalse(Files.exists(path1));
  }

  @Test
  void mergeMedias_worksIfFirstFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_3.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    this.db.mergeMedias(
        new MediaFile(2, path1, new Hash(0)),
        new MediaFile(1, path, new Hash(0)),
        true
    );
    assertFalse(this.db.isFileRegistered(path1));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void mergeMedias_worksIfSecondFileDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_0.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    this.db.mergeMedias(
        new MediaFile(2, path1, new Hash(0)),
        new MediaFile(1, path, new Hash(0)),
        true
    );
    assertFalse(this.db.isFileRegistered(path1));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void mergeMedias_updateTagsCounts() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test")
    ), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(
        new ParsedTag(Optional.empty(), "test")
    ), Set.of()));
    assertEquals(Map.of(1, 2), this.db.getAllTagsCounts());
    this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(2, path1, new Hash(1)),
        false
    );
    assertEquals(Map.of(1, 1), this.db.getAllTagsCounts());
  }

  @Test
  void mergeMedias_sameIdsError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    assertThrows(IllegalArgumentException.class, () -> this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(1, path1, new Hash(0)),
        false
    ));
  }

  @Test
  void mergeMedias_samePathsError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.insertMedia(new MediaFileUpdate(0, path1, Optional.of(new Hash(1)), Set.of(), Set.of()));
    assertThrows(IllegalArgumentException.class, () -> this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(2, path, new Hash(0)),
        false
    ));
  }

  @Test
  void mergeMedias_firstNotInDbError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class, () -> this.db.mergeMedias(
        new MediaFile(2, Path.of("test_file_2.png"), new Hash(0)),
        new MediaFile(1, path, new Hash(0)),
        false
    ));
  }

  @Test
  void mergeMedias_secondNotInDbError() throws DatabaseOperationException {
    final Path path = Path.of("test_file.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    assertThrows(DatabaseOperationException.class, () -> this.db.mergeMedias(
        new MediaFile(1, path, new Hash(0)),
        new MediaFile(2, Path.of("test_file_2.png"), new Hash(0)),
        false
    ));
  }

  // endregion
  // region deleteMedia

  @Test
  void deleteMedia_deletesIfRequested() throws DatabaseOperationException {
    final Path path = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.deleteMedia(new MediaFile(1, path, new Hash(0)), true);
    assertFalse(this.db.isFileRegistered(path));
    assertFalse(Files.exists(path));
  }

  @Test
  void deleteMedia_doesNotDeleteFileIfNotRequested() throws DatabaseOperationException {
    final Path path = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.deleteMedia(new MediaFile(1, path, new Hash(0)), false);
    assertFalse(this.db.isFileRegistered(path));
    assertTrue(Files.exists(path));
  }

  @Test
  void deleteMedia_worksIfDoesNotExist() throws DatabaseOperationException {
    final Path path = Path.of("test_file_3.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(), Set.of()));
    this.db.deleteMedia(new MediaFile(1, path, new Hash(0)), true);
    assertFalse(this.db.isFileRegistered(path));
  }

  @Test
  void deleteMedia_updatesTagCount() throws DatabaseOperationException {
    final Path path = Path.of("test_file_2.png");
    this.db.insertMedia(new MediaFileUpdate(0, path, Optional.of(new Hash(0)), Set.of(
        new ParsedTag(Optional.empty(), "test")
    ), Set.of()));
    assertEquals(Map.of(1, 1), this.db.getAllTagsCounts());
    this.db.deleteMedia(new MediaFile(1, path, new Hash(0)), false);
    assertEquals(Map.of(1, 0), this.db.getAllTagsCounts());
  }

  @Test
  void deleteMedia_notInDbError() {
    assertThrows(DatabaseOperationException.class,
        () -> this.db.deleteMedia(new MediaFile(1, Path.of("test_file_2.png"), new Hash(0)), false));
  }

  // endregion

  private Set<MediaFile> getAllMedias() {
    try {
      return this.db.queryMedias(TagQueryParser.parse("a + -a", Map.of(), DatabaseConnection.PSEUDO_TAGS, null));
    } catch (final InvalidPseudoTagException | DatabaseOperationException e) {
      throw new RuntimeException(e);
    }
  }
}