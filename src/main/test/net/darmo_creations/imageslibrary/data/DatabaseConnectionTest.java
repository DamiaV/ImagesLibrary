package net.darmo_creations.imageslibrary.data;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.logging.*;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {
  private DatabaseConnection db;

  // region before/after each

  @BeforeEach
  void setUp() throws DatabaseOperationError {
    this.db = new DatabaseConnection(null, Level.SEVERE);
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

  @Test
  @Disabled
  void queryPictures() {
    // TODO
  }

  @Test
  @Disabled
  void getImagesWithNoTags() {
    // TODO
  }

  @Test
  @Disabled
  void getImageTags() {
    // TODO
  }

  @Test
  @Disabled
  void isFileRegistered() {
    // TODO
  }

  @Test
  @Disabled
  void getSimilarImages() {
    // TODO
  }

  @Test
  @Disabled
  void insertPicture() {
    // TODO
  }

  @Test
  @Disabled
  void updatePicture() {
    // TODO
  }

  @Test
  @Disabled
  void renamePicture() {
    // TODO
  }

  @Test
  @Disabled
  void movePicture() {
    // TODO
  }

  @Test
  @Disabled
  void mergePictures() {
    // TODO
  }

  @Test
  @Disabled
  void deletePicture() {
    // TODO
  }
}