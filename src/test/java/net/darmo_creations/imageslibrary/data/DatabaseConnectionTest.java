package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import org.junit.jupiter.api.*;
import org.logicng.formulas.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {
  private DatabaseConnection db;

  // region before/after each

  @BeforeEach
  void setUp() throws DatabaseOperationError, IOException {
    this.db = new DatabaseConnection(null, Level.SEVERE);
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
  void tearDown() throws DatabaseOperationError {
    this.db.close();
  }

  // endregion
  // region insertTagTypes

  @Test
  void insertTagTypes() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '$', 1),
        new TagTypeUpdate(0, "test2", '/', 2)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '$', 1),
        new TagType(2, "test2", '/', 2)
    ), this.db.getAllTagTypes());
    assertEquals(Map.of(
        1, 0,
        2, 0
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertTagTypes_duplicateLabelsError() {
    assertThrows(DatabaseOperationError.class, () -> this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '$', 1),
        new TagTypeUpdate(0, "test1", '/', 2)
    )));
    assertTrue(this.db.getAllTagTypes().isEmpty());
    assertTrue(this.db.getAllTagTypesCounts().isEmpty());
  }

  @Test
  void insertTagTypes_duplicateSymbolsError() {
    assertThrows(DatabaseOperationError.class, () -> this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 1),
        new TagTypeUpdate(0, "test2", '/', 2)
    )));
    assertTrue(this.db.getAllTagTypes().isEmpty());
    assertTrue(this.db.getAllTagTypesCounts().isEmpty());
  }

  // endregion
  // region updateTagTypes

  @Test
  void updateTagTypes_label() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(List.of(
        new TagTypeUpdate(1, "test2", '/', 0)
    ));
    assertEquals(Set.of(
        new TagType(1, "test2", '/', 0)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_symbol() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(List.of(
        new TagTypeUpdate(1, "test1", '$', 0)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '$', 0)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_color() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    this.db.updateTagTypes(List.of(
        new TagTypeUpdate(1, "test1", '/', 1)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '/', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_swappedLabels() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    this.db.updateTagTypes(List.of(
        new TagTypeUpdate(1, "test2", '/', 0),
        new TagTypeUpdate(2, "test1", '$', 1)
    ));
    assertEquals(Set.of(
        new TagType(1, "test2", '/', 0),
        new TagType(2, "test1", '$', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_swappedSymbols() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    this.db.updateTagTypes(List.of(
        new TagTypeUpdate(1, "test1", '$', 0),
        new TagTypeUpdate(2, "test2", '/', 1)
    ));
    assertEquals(Set.of(
        new TagType(1, "test1", '$', 0),
        new TagType(2, "test2", '/', 1)
    ), this.db.getAllTagTypes());
  }

  @Test
  void updateTagTypes_notInDbError() {
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updateTagTypes(List.of(new TagTypeUpdate(1, "test", '/', 0))));
  }

  // endregion
  // region deleteTagTypes

  @Test
  void deleteTagTypes() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "test", '/', 0)));
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
  void deleteTagTypes_setsTagTypeToNull() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test", tagType, null)
    ));
    this.db.deleteTagTypes(this.db.getAllTagTypes());
    //noinspection OptionalGetWithoutIsPresent
    final Tag tag = this.db.getAllTags().stream().findFirst().get();
    assertTrue(tag.type().isEmpty());
  }

  @Test
  void deleteTagTypes_notInDbErrorDoesNotUpdateCache() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "test", '/', 0)));
    assertFalse(this.db.getAllTagTypes().isEmpty());
    assertFalse(this.db.getAllTagTypesCounts().isEmpty());
    assertThrows(DatabaseOperationError.class,
        () -> this.db.deleteTagTypes(Set.of(new TagType(2, "test1", '$', 0))));
    assertFalse(this.db.getAllTagTypes().isEmpty());
    assertFalse(this.db.getAllTagTypesCounts().isEmpty());
  }

  @Test
  void deleteTagTypes_notInDbError() {
    assertThrows(DatabaseOperationError.class,
        () -> this.db.deleteTagTypes(Set.of(new TagType(1, "test", '/', 0))));
  }

  // endregion
  // region insertTags

  @Test
  void insertTags() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType, null),
        new TagUpdate(0, "test2", null, "a b"),
        new TagUpdate(0, "test3", tagType, "c d")
    ));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null),
        new Tag(2, "test2", null, "a b"),
        new Tag(3, "test3", tagType, "c d")
    ), this.db.getAllTags());
    assertEquals(Map.of(
        1, 0,
        2, 0,
        3, 0
    ), this.db.getAllTagsCounts());
  }

  @Test
  void insertTags_updatesTagTypesCounts() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType, null),
        new TagUpdate(0, "test2", null, "a b"),
        new TagUpdate(0, "test3", tagType, "c d")
    ));
    assertEquals(Map.of(
        1, 2
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertTags_duplicateLabelsError() {
    assertThrows(DatabaseOperationError.class, () -> this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test1", null, null)
    )));
    assertTrue(this.db.getAllTags().isEmpty());
    assertTrue(this.db.getAllTagsCounts().isEmpty());
  }

  // endregion
  // region updateTags

  @Test
  void updateTags_labels() throws DatabaseOperationError {
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null)
    ));
    this.db.updateTags(List.of(
        new TagUpdate(1, "test2", null, null)
    ));
    assertEquals(Set.of(
        new Tag(1, "test2", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_types() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", null, null),
        new TagUpdate(2, "test2", tagType, null)
    ));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", tagType, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_nullToNonNull() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null)
    ));
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_NonNullToNull() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", null, null)
    ));
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesUpdatesTagTypesCounts_NonNullToNonNull() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0),
        new TagTypeUpdate(0, "test2", '$', 1)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType1 = this.db.getAllTagTypes().stream().filter(t -> t.id() == 1).findFirst().get();
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType2 = this.db.getAllTagTypes().stream().filter(t -> t.id() == 2).findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType1, null)
    ));
    assertEquals(Map.of(
        1, 1,
        2, 0
    ), this.db.getAllTagTypesCounts());
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", tagType2, null)
    ));
    assertEquals(Map.of(
        1, 0,
        2, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_typesSameDoesNotUpdateTagTypesCounts() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "test1", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
  }

  @Test
  void updateTags_definitions() throws DatabaseOperationError {
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, "a b"),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.updateTags(List.of(
        new TagUpdate(1, "test1", null, null),
        new TagUpdate(2, "test2", null, "c d")
    ));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, "c d")
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_swappedLabels() throws DatabaseOperationError {
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.updateTags(List.of(
        new TagUpdate(1, "test2", null, null),
        new TagUpdate(2, "test1", null, null)
    ));
    assertEquals(Set.of(
        new Tag(1, "test2", null, null),
        new Tag(2, "test1", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void updateTags_addDefinitionButAlreadyLinkedToImageError() throws DatabaseOperationError {
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null)
    ));
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file.png"), new Hash(0), Set.of(
        new Pair<>(null, "test1")
    ), Set.of()));
    assertThrows(DatabaseOperationError.class, () -> this.db.updateTags(List.of(
        new TagUpdate(1, "test1", null, "c d")
    )));
  }

  @Test
  void updateTags_notInDbError() {
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updateTags(List.of(new TagUpdate(1, "test", null, null))));
  }

  // endregion
  // region deleteTags

  @Test
  void deleteTags() throws DatabaseOperationError {
    this.db.insertTags(List.of(new TagUpdate(0, "test", null, null)));
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
  void deleteTags_updatesTagTypesCounts() throws DatabaseOperationError {
    this.db.insertTagTypes(List.of(
        new TagTypeUpdate(0, "type", '/', 0)
    ));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertTags(List.of(
        new TagUpdate(0, "test", tagType, null)
    ));
    assertEquals(Map.of(1, 1), this.db.getAllTagTypesCounts());
    this.db.deleteTags(this.db.getAllTags());
    assertEquals(Map.of(1, 0), this.db.getAllTagTypesCounts());
  }

  @Test
  void deleteTags_notInDbErrorDoesNotUpdateCache() throws DatabaseOperationError {
    this.db.insertTags(List.of(new TagUpdate(0, "test", null, null)));
    assertFalse(this.db.getAllTags().isEmpty());
    assertFalse(this.db.getAllTagsCounts().isEmpty());
    assertThrows(DatabaseOperationError.class,
        () -> this.db.deleteTags(Set.of(new Tag(2, "test1", null, null))));
    assertFalse(this.db.getAllTags().isEmpty());
    assertFalse(this.db.getAllTagsCounts().isEmpty());
  }

  @Test
  void deleteTags_notInDbError() {
    assertThrows(DatabaseOperationError.class,
        () -> this.db.deleteTags(Set.of(new Tag(1, "test", null, null))));
  }

  // endregion
  // region queryPictures

  private FormulaFactory initQueryPicturesTest() throws DatabaseOperationError {
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file.jpeg"), new Hash(0), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file_2.jpg"), new Hash(1), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test3")
    ), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file_3.png"), new Hash(-1), Set.of(), Set.of()));
    return new FormulaFactory();
  }

  @Test
  void queryPictures_trueReturnsAll() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.verum(), Map.of()));
    assertEquals(3, pictures.size());
  }

  @Test
  void queryPictures_falseReturnsNone() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.falsum(), Map.of()));
    assertTrue(pictures.isEmpty());
  }

  @Test
  void queryPictures_or() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.or(ff.variable("test2"), ff.variable("test3")), Map.of()));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_and() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.and(ff.variable("test1"), ff.variable("test2")), Map.of()));
    assertEquals(1, pictures.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new Picture(1, Path.of("test_file.jpeg"), new Hash(0)), pictures.stream().findFirst().get());
  }

  @Test
  void queryPictures_not() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.not(ff.variable("test2")), Map.of()));
    assertEquals(2, pictures.size());
    assertEquals(Set.of(
        new Picture(2, Path.of("test_file_2.jpg"), new Hash(1)),
        new Picture(3, Path.of("test_file_3.png"), new Hash(-1))
    ), pictures);
  }

  @Test
  void queryPictures_pseudoTag_extPlainString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("ext:string::jpeg"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(1, pictures.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new Picture(1, Path.of("test_file.jpeg"), new Hash(0)), pictures.stream().findFirst().get());
  }

  @Test
  void queryPictures_pseudoTag_extTemplateString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("ext:string::jp?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_extRegex() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("ext:regex::jp[e]?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_namePlainString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("name:string::test_file.jpeg"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(1, pictures.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new Picture(1, Path.of("test_file.jpeg"), new Hash(0)), pictures.stream().findFirst().get());
  }

  @Test
  void queryPictures_pseudoTag_nameTemplateString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("name:string::test_file*.jp?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_nameRegex() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("name:regex::test_file.*\\.jpe?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_pathPlainString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("path:string::%s/test_file.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(1, pictures.size());
    //noinspection OptionalGetWithoutIsPresent
    assertEquals(new Picture(1, Path.of("test_file.jpeg"), new Hash(0)), pictures.stream().findFirst().get());
  }

  @Test
  void queryPictures_pseudoTag_pathTemplateString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("path:string::*/test_file*.jp?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_pathRegex() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("path:regex::.*/test_file.*\\.jpe?g"), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_similar_toPlainString() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("similar_to:string::%s/test_file.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS));
    assertEquals(2, pictures.size());
  }

  @Test
  void queryPictures_pseudoTag_similar_toTemplateStringNoError() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(
        new TagQuery(ff.variable("similar_to:string::%s/test_file.jp?g".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS));
    assertTrue(pictures.isEmpty());
  }

  @Test
  void queryPictures_pseudoTag_similar_toRegexError() throws DatabaseOperationError {
    final var ff = this.initQueryPicturesTest();
    assertThrows(InvalidPseudoTagException.class,
        () -> this.db.queryPictures(new TagQuery(ff.variable("similar_to:regex::%s/test_file\\.jpeg".formatted(Path.of("").toAbsolutePath())), DatabaseConnection.PSEUDO_TAGS)));
  }

  @Test
  void queryPictures_nonExistentTagNoError() throws DatabaseOperationError, InvalidPseudoTagException {
    final var ff = this.initQueryPicturesTest();
    final var pictures = this.db.queryPictures(new TagQuery(ff.variable("yo"), Map.of()));
    assertTrue(pictures.isEmpty());
  }

  @Test
  void queryPictures_nonExistentPseudoTagError() throws DatabaseOperationError {
    final var ff = this.initQueryPicturesTest();
    assertThrows(InvalidPseudoTagException.class, () -> this.db.queryPictures(new TagQuery(ff.variable("invalid:string::a"), Map.of())));
  }

  // endregion
  // region getImagesWithNoTags

  @Test
  void getImagesWithNoTags() throws IOException, DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, Hash.computeForFile(path), Set.of(), Set.of()));
    final var imagesWithNoTags = this.db.getImagesWithNoTags();
    assertFalse(imagesWithNoTags.isEmpty());
  }

  // endregion
  // region getImageTags

  @Test
  void getImageTags() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test"),
        new Pair<>(null, "test2")
    ), Set.of()));
    final Set<Tag> imageTags = this.db.getImageTags(new Picture(1, path, new Hash(0)));
    assertEquals(this.db.getAllTags(), imageTags);
  }

  // endregion
  // region isFileRegistered

  @Test
  void isFileRegistered() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void isFileRegistered_not() throws DatabaseOperationError {
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file.png"), new Hash(0), Set.of(), Set.of()));
    assertFalse(this.db.isFileRegistered(Path.of("test_file_2.png")));
  }

  // endregion
  // region getSimilarImages

  @Test
  void getSimilarImages() throws DatabaseOperationError {
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file.png"), new Hash(0), Set.of(), Set.of()));
    final Path path = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(-1), Set.of(), Set.of()));
    final var similarImages = this.db.getSimilarImages(new Hash(-1), null);
    assertEquals(1, similarImages.size());
    assertEquals(new Picture(2, path, new Hash(-1)), similarImages.get(0).getKey());
  }

  @Test
  void getSimilarImages_returnsSameConfidenceIndexAsHashClass() throws DatabaseOperationError {
    this.db.insertPicture(new PictureUpdate(0, Path.of("test_file.png"), new Hash(0), Set.of(), Set.of()));
    assertEquals(
        new Hash(0).computeSimilarity(new Hash(0)).confidence(),
        this.db.getSimilarImages(new Hash(0), null).get(0).getValue(),
        1e-6f
    );
  }

  // endregion
  // region insertPicture

  @Test
  void insertPicture() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void insertPicture_addsTagsToPicture() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, null)
    ), this.db.getImageTags(new Picture(1, path, new Hash(0))));
  }

  @Test
  void insertPicture_createsTags() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(tagType, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null),
        new Tag(2, "test2", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void insertPicture_updatesTagCounts() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1,
        2, 1
    ), this.db.getAllTagsCounts());
  }

  @Test
  void insertPicture_updatesTagTypeCounts() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(tagType, "test1")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void insertPicture_tagsToRemoveNotEmptyError() {
    final Path path = Path.of("test_file.png");
    assertThrows(IllegalArgumentException.class,
        () -> this.db.insertPicture(
            new PictureUpdate(0, path, Hash.computeForFile(path), Set.of(),
                Set.of(new Tag(1, "test", null, null)))));
  }

  @Test
  void insertPicture_undefinedTagTypeError() {
    final Path path = Path.of("test_file.png");
    final TagType tagType = new TagType(0, "test", '/', 0);
    assertThrows(DatabaseOperationError.class,
        () -> this.db.insertPicture(new PictureUpdate(0, path, Hash.computeForFile(path), Set.of(
            new Pair<>(tagType, "test1")
        ), Set.of()))
    );
  }

  @Test
  void insertPicture_addTagsWithDefinitionsError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, "a b")
    ));
    assertThrows(DatabaseOperationError.class,
        () -> this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
            new Pair<>(null, "test1")
        ), Set.of()))
    );
  }

  @Test
  void insertPicture_duplicatePathsError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class,
        () -> this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()))
    );
  }

  @Test
  void insertPicture_duplicateTagNamesError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    assertThrows(DatabaseOperationError.class,
        () -> this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
            new Pair<>(tagType, "test"),
            new Pair<>(null, "test")
        ), Set.of()))
    );
  }

  @Test
  void insertPicture_errorRollbacksEverything() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    assertThrows(DatabaseOperationError.class,
        () -> this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
            new Pair<>(tagType, "test"),
            new Pair<>(null, "test")
        ), Set.of()))
    );
    assertTrue(this.db.getAllTags().isEmpty());
    assertFalse(this.db.isFileRegistered(path));
  }

  // endregion
  // region updatePicture

  @Test
  void updatePicture_hash() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var picture = this.getAllPictures()
        .stream().findFirst().get();
    assertEquals(new Hash(1), picture.hash());
  }

  @Test
  void updatePicture_addsTagsToPicture() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, null)
    ), this.db.getImageTags(new Picture(1, path, new Hash(0))));
  }

  @Test
  void updatePicture_removesTagsFromPicture() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, null),
        new TagUpdate(0, "test2", null, null)
    ));
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(), Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, null)
    )));
    assertTrue(this.db.getImageTags(new Picture(1, path, new Hash(0))).isEmpty());
  }

  @Test
  void updatePicture_createsTags() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(
        new Pair<>(tagType, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Set.of(
        new Tag(1, "test1", tagType, null),
        new Tag(2, "test2", null, null)
    ), this.db.getAllTags());
  }

  @Test
  void updatePicture_updatesTagCounts() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(
        new Pair<>(null, "test1"),
        new Pair<>(null, "test2")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1,
        2, 1
    ), this.db.getAllTagsCounts());
  }

  @Test
  void updatePicture_updatesTagTypeCounts() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(
        new Pair<>(tagType, "test")
    ), Set.of()));
    assertEquals(Map.of(
        1, 1
    ), this.db.getAllTagTypesCounts());
  }

  @Test
  void updatePicture_undefinedTagTypeError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final TagType tagType = new TagType(0, "test", '/', 0);
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class, () -> this.db.updatePicture(new PictureUpdate(1, path, new Hash(1), Set.of(
        new Pair<>(tagType, "test")
    ), Set.of())));
  }

  @Test
  void updatePicture_pathDoesNothing() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    Path path2 = Path.of("test_file_2.png");
    this.db.updatePicture(new PictureUpdate(1, path2, new Hash(0), Set.of(), Set.of()));
    assertTrue(this.db.isFileRegistered(path));
    assertFalse(this.db.isFileRegistered(path2));
  }

  @Test
  void updatePicture_duplicateTagNamesError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updatePicture(new PictureUpdate(1, path, new Hash(0), Set.of(
            new Pair<>(tagType, "test"),
            new Pair<>(null, "test")
        ), Set.of()))
    );
  }

  @Test
  void updatePicture_addTagsWithDefinitionsError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTags(List.of(
        new TagUpdate(0, "test1", null, "a b")
    ));
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updatePicture(new PictureUpdate(1, path, new Hash(0), Set.of(
            new Pair<>(null, "test1")
        ), Set.of()))
    );
  }

  @Test
  void updatePicture_errorRollbacksEverything() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertTagTypes(List.of(new TagTypeUpdate(0, "type", '/', 0)));
    //noinspection OptionalGetWithoutIsPresent
    final TagType tagType = this.db.getAllTagTypes().stream().findFirst().get();
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updatePicture(new PictureUpdate(0, path, new Hash(1), Set.of(
            new Pair<>(tagType, "test"),
            new Pair<>(null, "test")
        ), Set.of()))
    );
    assertTrue(this.db.getAllTags().isEmpty());
    //noinspection OptionalGetWithoutIsPresent
    final var picture = this.getAllPictures()
        .stream().findFirst().get();
    assertEquals(new Hash(0), picture.hash());
  }

  @Test
  void updatePicture_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationError.class,
        () -> this.db.updatePicture(new PictureUpdate(1, path, new Hash(0), Set.of(), Set.of()))
    );
  }

  // endregion
  // region renamePicture

  @Test
  void renamePicture_updatesPath() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.renamePicture(pic, "test_file_3.png");
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("test_file_3.png").toAbsolutePath(), pic.path());
  }

  @Test
  void renamePicture_renamesFile() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var pic = this.getAllPictures().stream().findFirst().get();
    this.db.renamePicture(pic, "test_file_3.png");
    assertTrue(Files.exists(Path.of("test_file_3.png")));
  }

  @Test
  void renamePicture_worksIfFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_0.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.renamePicture(pic, "test_file_3.png");
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("test_file_3.png").toAbsolutePath(), pic.path());
  }

  @Test
  void renamePicture_worksIfTargetExistsButFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_0.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.renamePicture(pic, "test_file_2.png");
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("test_file_2.png").toAbsolutePath(), pic.path());
  }

  @Test
  void renamePicture_targetFileExistsError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationError.class,
        () -> this.db.renamePicture(new Picture(1, path, new Hash(0)), "test_file_2.png"));
  }

  @Test
  void renamePicture_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationError.class,
        () -> this.db.renamePicture(new Picture(1, path, new Hash(0)), "test_file_3.png"));
  }

  // endregion
  // region movePicture

  @Test
  void movePicture_updatesPath() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.movePicture(pic, Path.of("dest"));
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("dest", "test_file.png").toAbsolutePath(), pic.path());
  }

  @Test
  void movePicture_movesFile() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    final var pic = this.getAllPictures().stream().findFirst().get();
    this.db.movePicture(pic, Path.of("dest"));
    assertTrue(Files.exists(Path.of("dest", "test_file.png")));
  }

  @Test
  void movePicture_worksIfFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_0.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.movePicture(pic, Path.of("dest"));
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("dest", "test_file_0.png").toAbsolutePath(), pic.path());
  }

  @Test
  void movePicture_worksIfTargetExistsButFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_0.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    //noinspection OptionalGetWithoutIsPresent
    var pic = this.getAllPictures().stream().findFirst().get();
    this.db.movePicture(pic, Path.of("dest"));
    //noinspection OptionalGetWithoutIsPresent
    pic = this.getAllPictures().stream().findFirst().get();
    assertEquals(Path.of("dest", "test_file_0.png").toAbsolutePath(), pic.path());
  }

  @Test
  void movePicture_targetFileExistsError() {
    final Path path = Path.of("test_file_2.png");
    assertThrows(DatabaseOperationError.class,
        () -> this.db.movePicture(new Picture(1, path, new Hash(0)), Path.of("dest")));
  }

  @Test
  void movePicture_notInDbError() {
    final Path path = Path.of("test_file.png");
    assertThrows(DatabaseOperationError.class,
        () -> this.db.movePicture(new Picture(1, path, new Hash(0)), Path.of("dest")));
  }

  // endregion
  // region mergePictures

  @Test
  void mergePictures_mergesTags() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test1")
    ), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(
        new Pair<>(null, "test2")
    ), Set.of()));
    this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(2, path1, new Hash(0)),
        false
    );
    final var imageTags = this.db.getImageTags(new Picture(2, path1, new Hash(0)));
    assertEquals(Set.of(
        new Tag(1, "test1", null, null),
        new Tag(2, "test2", null, null)
    ), imageTags);
  }

  @Test
  void mergePictures_deletesFirstImageEntry() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(2, path1, new Hash(0)),
        false
    );
    assertFalse(this.db.isFileRegistered(path));
    assertTrue(this.db.isFileRegistered(path1));
  }

  @Test
  void mergePictures_deletesFirstImageFileIfRequested() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    this.db.mergePictures(
        new Picture(2, path1, new Hash(0)),
        new Picture(1, path, new Hash(0)),
        true
    );
    assertTrue(Files.exists(path));
    assertFalse(Files.exists(path1));
  }

  @Test
  void mergePictures_worksIfFirstFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_3.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    this.db.mergePictures(
        new Picture(2, path1, new Hash(0)),
        new Picture(1, path, new Hash(0)),
        true
    );
    assertFalse(this.db.isFileRegistered(path1));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void mergePictures_worksIfSecondFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_0.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    this.db.mergePictures(
        new Picture(2, path1, new Hash(0)),
        new Picture(1, path, new Hash(0)),
        true
    );
    assertFalse(this.db.isFileRegistered(path1));
    assertTrue(this.db.isFileRegistered(path));
  }

  @Test
  void mergePictures_updateTagsCounts() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test")
    ), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(
        new Pair<>(null, "test")
    ), Set.of()));
    assertEquals(Map.of(1, 2), this.db.getAllTagsCounts());
    this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(2, path1, new Hash(1)),
        false
    );
    assertEquals(Map.of(1, 1), this.db.getAllTagsCounts());
  }

  @Test
  void mergePictures_sameIdsError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    assertThrows(IllegalArgumentException.class, () -> this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(1, path1, new Hash(0)),
        false
    ));
  }

  @Test
  void mergePictures_samePathsError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    final Path path1 = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.insertPicture(new PictureUpdate(0, path1, new Hash(1), Set.of(), Set.of()));
    assertThrows(IllegalArgumentException.class, () -> this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(2, path, new Hash(0)),
        false
    ));
  }

  @Test
  void mergePictures_firstNotInDbError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class, () -> this.db.mergePictures(
        new Picture(2, Path.of("test_file_2.png"), new Hash(0)),
        new Picture(1, path, new Hash(0)),
        false
    ));
  }

  @Test
  void mergePictures_secondNotInDbError() throws DatabaseOperationError {
    final Path path = Path.of("test_file.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    assertThrows(DatabaseOperationError.class, () -> this.db.mergePictures(
        new Picture(1, path, new Hash(0)),
        new Picture(2, Path.of("test_file_2.png"), new Hash(0)),
        false
    ));
  }

  // endregion
  // region deletePicture

  @Test
  void deletePicture_deletesFileIfRequested() throws DatabaseOperationError {
    final Path path = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.deletePicture(new Picture(1, path, new Hash(0)), true);
    assertFalse(this.db.isFileRegistered(path));
    assertFalse(Files.exists(path));
  }

  @Test
  void deletePicture_doesNotDeleteFileIfNotRequested() throws DatabaseOperationError {
    final Path path = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.deletePicture(new Picture(1, path, new Hash(0)), false);
    assertFalse(this.db.isFileRegistered(path));
    assertTrue(Files.exists(path));
  }

  @Test
  void deletePicture_worksIfFileDoesNotExist() throws DatabaseOperationError {
    final Path path = Path.of("test_file_3.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(), Set.of()));
    this.db.deletePicture(new Picture(1, path, new Hash(0)), true);
    assertFalse(this.db.isFileRegistered(path));
  }

  @Test
  void deletePicture_updatesTagCount() throws DatabaseOperationError {
    final Path path = Path.of("test_file_2.png");
    this.db.insertPicture(new PictureUpdate(0, path, new Hash(0), Set.of(
        new Pair<>(null, "test")
    ), Set.of()));
    assertEquals(Map.of(1, 1), this.db.getAllTagsCounts());
    this.db.deletePicture(new Picture(1, path, new Hash(0)), false);
    assertEquals(Map.of(1, 0), this.db.getAllTagsCounts());
  }

  @Test
  void deletePicture_notInDbError() {
    assertThrows(DatabaseOperationError.class,
        () -> this.db.deletePicture(new Picture(1, Path.of("test_file_2.png"), new Hash(0)), false));
  }

  // endregion

  private Set<Picture> getAllPictures() {
    try {
      return this.db.queryPictures(TagQueryParser.parse("a + -a", Map.of(), DatabaseConnection.PSEUDO_TAGS));
    } catch (InvalidPseudoTagException | DatabaseOperationError e) {
      throw new RuntimeException(e);
    }
  }
}