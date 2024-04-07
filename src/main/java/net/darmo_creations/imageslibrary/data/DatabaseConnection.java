package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
import net.darmo_creations.imageslibrary.data.sql_functions.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.utils.ReflectionUtils;
import org.intellij.lang.annotations.*;
import org.jetbrains.annotations.*;
import org.reflections.*;
import org.reflections.scanners.*;
import org.reflections.util.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

/**
 * This class acts as the access point to an SQLite database file.
 * <p>
 * Instances maintain an internal cache of all tags and tag types.
 */
@SuppressWarnings("unused") // TEMP to better see other TODOs
public final class DatabaseConnection implements AutoCloseable {
  /**
   * This database path, when passed to the constructor of this class,
   * indicates that the connection should use an in-memory database instance.
   */
  public static final String MEMORY_DB_PATH = ":memory:";

  /**
   * A map of all pseudo-tags that can be used in tag queries.
   */
  @Unmodifiable
  public static final Map<String, PseudoTag> PSEUDO_TAGS = Map.of(
      "ext",
      new PseudoTag("""
          SELECT id, path, hash
          FROM images
          WHERE "REGEX"(SUBSTR(path, "RINSTR"(path, '.') + 1), '%s', '%s')
          """, true),

      "name",
      new PseudoTag("""
          SELECT id, path, hash
          FROM images
          WHERE "REGEX"(SUBSTR(path, "RINSTR"(path, '/') + 1), '%s', '%s')
          """.replace("/", File.separator), true),

      "path",
      new PseudoTag("""
          SELECT id, path, hash
          FROM images
          WHERE "REGEX"(path, '%s', '%s')
          """, true),

      "similar_to",
      new PseudoTag("""
          SELECT id, path, hash
          FROM images
          WHERE hash IS NOT NULL
            AND "SIMILAR_HASHES"(hash, (
              SELECT hash
              FROM images
              WHERE path = '%s'
            ))
          """, false)
  );

  /**
   * The current database schema version.
   */
  public static final int CURRENT_SCHEMA_VERSION = 0;
  /**
   * The name of the database setup file.
   */
  public static final String SETUP_FILE_NAME = "setup.sql";

  private final Logger logger;
  private final Connection connection;

  private final Map<Integer, TagType> tagTypesCache = new HashMap<>();
  private final Map<Integer, Integer> tagTypesCounts = new HashMap<>();
  private final Map<Integer, Tag> tagsCache = new HashMap<>();
  private final Map<Integer, Integer> tagsCounts = new HashMap<>();

  /**
   * Create a new connection to the given SQLite database file.
   *
   * @param file The file containing the database. If it does not exist, it will be created.
   * @throws IOException If the file exists but is not a database file or is incompatible.
   */
  public DatabaseConnection(Path file) throws IOException {
    Objects.requireNonNull(file);
    this.logger = Logger.getLogger("DB (%s)".formatted(file));
    this.logger.info("Connecting to database file at %s".formatted(file));
    try {
      this.connection = DriverManager.getConnection("jdbc:sqlite:%s".formatted(file));
      this.injectCustomFunctions();
      this.connection.setAutoCommit(false);
      this.executeQuery("PRAGMA FOREIGN_KEYS = ON");
      this.logger.info("Foreign keys enabled");
      if (!Files.exists(file)) // If the DB file does not exist, create it
        this.setupDatabase();
      else
        this.checkSchemaVersion();
    } catch (SQLException | IOException e) {
      if (e instanceof IOException ex)
        throw this.logThrownError(ex); // No need to wrap
      throw this.logThrownError(new IOException(e));
    }
    this.logger.info("Connection established.");

    this.initCaches();
  }

  /**
   * Auto-detect and inject custom SQL functions into the driver.
   * <p>
   * Functions are automatically detected by checking every class annotated with {@link SqlFunction} in the
   * {@link net.darmo_creations.imageslibrary.data.sql_functions} package.
   *
   * @throws SQLException If any database error occurs.
   */
  private void injectCustomFunctions() throws SQLException {
    this.logger.info("Injecting custom SQL functions…");
    final var reflections = new Reflections(new ConfigurationBuilder()
        .forPackage(this.getClass().getPackageName() + ".sql_functions")
        .setScanners(Scanners.TypesAnnotated));
    final var annotatedClasses = reflections.getTypesAnnotatedWith(SqlFunction.class);
    int total = 0;
    for (final var annotatedClass : annotatedClasses) {
      if (org.sqlite.Function.class.isAssignableFrom(annotatedClass)) {
        @SuppressWarnings("unchecked")
        final var functionClass = (Class<? extends org.sqlite.Function>) annotatedClass;
        final var annotation = functionClass.getAnnotation(SqlFunction.class);
        this.logger.info("Found SQL function '%s'.".formatted(annotation.name()));
        try {
          org.sqlite.Function.create(
              this.connection,
              annotation.name(),
              functionClass.getConstructor().newInstance(),
              annotation.nArgs(),
              annotation.flags()
          );
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
          this.logCaughtError(e);
        }
        total++;
      } else {
        this.logger.warning("Class %s does not extend org.sqlite.Function, skipping.".formatted(annotatedClass.getName()));
      }
    }
    this.logger.info("Loaded %d functions.".formatted(total));
  }

  @SQLite
  private static final String CHECK_FOR_PYTHON_DB_0001_QUERY = """
      SELECT name
      FROM sqlite_master
      WHERE type = 'table'
        AND name = 'version'
      """;

  /**
   * Check whether the connected database has the correct format.
   *
   * @throws SQLException If the database has an incorrect structure, or any database error occurs.
   */
  private void checkSchemaVersion() throws SQLException {
    final var pythonErrorMsg = "Python-generated database detected, please convert it before using it with this app";
    // Check if the "images.hash" column is missing
    try (final var resultSet1 = this.selectQuery("PRAGMA TABLE_INFO (images)")) {
      boolean hashFound = false;
      while (resultSet1.next()) {
        if (resultSet1.getString("name").equals("hash")) {
          hashFound = true;
          break;
        }
      }
      if (!hashFound)
        throw this.logThrownError(new SQLException(pythonErrorMsg));
    }
    // Check if the "version" table is present
    try (final var resultSet = this.selectQuery(CHECK_FOR_PYTHON_DB_0001_QUERY)) {
      if (resultSet.next())
        throw this.logThrownError(new SQLException(pythonErrorMsg));
    }

    try (final var resultSet = this.selectQuery("PRAGMA USER_VERSION")) {
      resultSet.next();
      final int schemaVersion = resultSet.getInt(1);
      if (schemaVersion > CURRENT_SCHEMA_VERSION)
        throw this.logThrownError(new SQLException("Invalid database schema version: %d".formatted(schemaVersion)));
    }
  }

  /**
   * The set of all tag types defined in the database.
   *
   * @return A new set.
   */
  @Contract(pure = true, value = "-> new")
  @Unmodifiable
  public Set<TagType> getAllTagTypes() {
    return this.tagTypesCache.values().stream().collect(Collectors.toUnmodifiableSet());
  }

  /**
   * A map containing the use counts of all tag types.
   *
   * @return A new map.
   */
  @Contract(pure = true, value = "-> new")
  @Unmodifiable
  public Map<Integer, Integer> getAllTagTypesCounts() {
    return Collections.unmodifiableMap(this.tagTypesCounts);
  }

  @SQLite
  public static final String INSERT_TAG_TYPES_QUERY = """
      INSERT INTO tag_types (label, symbol, color)
      VALUES (?, ?, ?)
      """;

  /**
   * Insert the given tag types. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tagTypeUpdates The list of tag types to insert. Updates are performed in the order of the list.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void insertTagTypes(final List<TagTypeUpdate> tagTypeUpdates) throws DatabaseOperationError {
    final List<Pair<Integer, TagTypeUpdate>> generatedIds = new LinkedList<>();

    try (final var statement = this.connection.prepareStatement(INSERT_TAG_TYPES_QUERY, Statement.RETURN_GENERATED_KEYS)) {
      for (final var tagTypeUpdate : tagTypeUpdates) {
        statement.setString(1, tagTypeUpdate.label());
        statement.setString(2, String.valueOf(tagTypeUpdate.symbol()));
        statement.setInt(3, tagTypeUpdate.color());
        statement.executeUpdate();
        final var id = getFirstGeneratedId(statement);
        if (id.isEmpty())
          throw this.logThrownError(new SQLException("Query did not generate any key"));
        generatedIds.add(new Pair<>(id.get(), tagTypeUpdate));
      }
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();

    for (final var entry : generatedIds) {
      final int id = entry.getKey();
      final TagTypeUpdate tagTypeUpdate = entry.getValue();
      this.tagTypesCache.put(id, new TagType(
          id,
          tagTypeUpdate.label(),
          tagTypeUpdate.symbol(),
          tagTypeUpdate.color()
      ));
      this.tagTypesCounts.put(id, 0);
    }
  }

  @SQLite
  public static final String UPDATE_TAG_TYPES_QUERY = """
      UPDATE tag_types
      SET label = ?1, symbol = ?2, color = ?3
      WHERE id = ?4
      """;

  /**
   * Update the given tag types. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tagTypeUpdates The list of tag type updates to perform. Updates are performed in the order of the list.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void updateTagTypes(final List<TagTypeUpdate> tagTypeUpdates) throws DatabaseOperationError {
    // FIXME what to do in case of label/symbol swap?
    try (final var statement = this.connection.prepareStatement(UPDATE_TAG_TYPES_QUERY)) {
      for (final var tagTypeUpdate : tagTypeUpdates) {
        final int id = tagTypeUpdate.id();
        statement.setString(1, tagTypeUpdate.label());
        statement.setString(2, String.valueOf(tagTypeUpdate.symbol()));
        statement.setInt(3, tagTypeUpdate.color());
        statement.setInt(4, id);
        if (statement.executeUpdate() == 0)
          throw this.logThrownError(new SQLException("No tag type with ID %d".formatted(id)));
      }
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();

    for (final var tagTypeUpdate : tagTypeUpdates) {
      final TagType tagType = this.tagTypesCache.get(tagTypeUpdate.id());
      tagType.setLabel(tagTypeUpdate.label());
      tagType.setSymbol(tagTypeUpdate.symbol());
      tagType.setColor(tagTypeUpdate.color());
    }
  }

  /**
   * Delete the given tag types. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tagTypes The set of tag types to delete.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void deleteTagTypes(final Set<TagType> tagTypes) throws DatabaseOperationError {
    this.deleteObjects(tagTypes, "tag_types");
    for (final var tagType : tagTypes) {
      this.tagTypesCache.remove(tagType.id());
      this.tagTypesCounts.remove(tagType.id());
      for (final var tag : this.tagsCache.values()) {
        tag.type().ifPresent(currentType -> {
          if (currentType.id() == tagType.id())
            tag.setType(null);
        });
      }
    }
  }

  /**
   * The set of all tag defined in the database.
   *
   * @return A new set.
   */
  @Contract(pure = true, value = "-> new")
  @Unmodifiable
  public Set<Tag> getAllTags() {
    return this.tagsCache.values().stream().collect(Collectors.toUnmodifiableSet());
  }

  /**
   * A map containing the use counts of all tags.
   *
   * @return A new map.
   */
  @Contract(pure = true, value = "-> new")
  @Unmodifiable
  public Map<Integer, Integer> getAllTagCounts() {
    return Collections.unmodifiableMap(this.tagsCounts);
  }

  @SQLite
  public static final String INSERT_TAGS_QUERY = """
      INSERT INTO tags (label, type_id, definition)
      VALUES (?, ?, ?)
      """;

  /**
   * Insert the given tags. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tagUpdates The list of tags to insert. Updates are performed in the order of the list.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void insertTags(final List<TagUpdate> tagUpdates) throws DatabaseOperationError {
    final List<Integer> generatedIds;
    try {
      generatedIds = this.insertTagsNoCommit(tagUpdates);
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();

    IntStream.range(0, tagUpdates.size())
        .mapToObj(i -> tagUpdates.get(i).withId(generatedIds.get(i)))
        .forEach(tagUpdate -> {
          final int id = tagUpdate.id();
          this.tagsCache.put(id, new Tag(
              id,
              tagUpdate.label(),
              tagUpdate.type().map(tt -> this.tagTypesCache.get(tt.id())).orElse(null),
              tagUpdate.definition().orElse(null)
          ));
          this.tagsCounts.put(id, 0);
        });
  }

  /**
   * Insert the given tags. This method does not perform any kind of transaction managment,
   * it is the responsablity of the caller to do so.
   *
   * @param tagUpdates The list of tags to insert. Updates are performed in the order of the list.
   * @return The list of generated IDs for each {@link TagUpdate} object, in the same order.
   * @throws SQLException If a tag with the same label already exists in the database, or any database error occurs.
   */
  private List<Integer> insertTagsNoCommit(final List<TagUpdate> tagUpdates) throws SQLException {
    final List<Integer> generatedIds = new LinkedList<>();
    try (final var statement = this.connection.prepareStatement(INSERT_TAGS_QUERY, Statement.RETURN_GENERATED_KEYS)) {
      for (final var tagUpdate : tagUpdates) {
        statement.setString(1, tagUpdate.label());
        if (tagUpdate.type().isEmpty())
          statement.setNull(2, Types.INTEGER);
        else
          statement.setInt(2, tagUpdate.type().get().id());
        statement.setString(3, tagUpdate.definition().orElse(null));
        statement.executeUpdate();
        final var id = getFirstGeneratedId(statement);
        if (id.isEmpty())
          throw this.logThrownError(new SQLException("Query did not generate any key"));
        generatedIds.add(id.get());
      }
    }
    return generatedIds;
  }

  @SQLite
  public static final String UPDATE_TAGS_QUERY = """
      UPDATE tags
      SET label = ?1, type_id = ?2, definition = ?3
      WHERE id = ?4
      """;

  /**
   * Update the given tags. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tagUpdates The list of tag updates to perform. Updates are performed in the order of the list.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void updateTags(final List<TagUpdate> tagUpdates) throws DatabaseOperationError {
    try {
      this.updateTagsNoCommit(tagUpdates);
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    } catch (DatabaseOperationError e) {
      this.rollback();
      throw e;
    }
    this.commit();

    for (final var tagUpdate : tagUpdates) {
      final Tag tag = this.tagsCache.get(tagUpdate.id());
      tag.setLabel(tagUpdate.label());
      tag.setType(tagUpdate.type().orElse(null));
      tag.setDefinition(tagUpdate.definition().orElse(null));
    }
  }

  /**
   * Update the given tags. This method does not perform any kind of transaction managment,
   * it is the responsablity of the caller to do so.
   *
   * @param tagUpdates The list of tag updates to perform. Updates are performed in the order of the list.
   * @throws SQLException           If any database error occurs.
   * @throws DatabaseOperationError If any database error occurs.
   */
  private void updateTagsNoCommit(final List<TagUpdate> tagUpdates) throws SQLException, DatabaseOperationError {
    // FIXME what to do in case of label swap?
    try (final var statement = this.connection.prepareStatement(UPDATE_TAGS_QUERY)) {
      for (final var tagUpdate : tagUpdates) {
        this.ensureInDatabase(tagUpdate);
        final String label = tagUpdate.label();
        final int id = tagUpdate.id();
        statement.setString(1, label);
        if (tagUpdate.type().isEmpty())
          statement.setNull(2, Types.INTEGER);
        else
          statement.setInt(2, tagUpdate.type().get().id());
        if (tagUpdate.definition().isPresent() && this.isTagUsed(tagUpdate))
          throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.BOUND_TAG_HAS_DEFINITION));
        statement.setString(3, tagUpdate.definition().orElse(null));
        statement.setInt(4, id);
        if (statement.executeUpdate() == 0)
          throw this.logThrownError(new SQLException("No tag with ID %d".formatted(id)));
      }
    }
  }

  @SQLite
  private static final String SELECT_PICTURES_FOR_TAG_QUERY = """
      SELECT *
      FROM image_tag
      WHERE tag_id = ?
      """;

  /**
   * Check whether the given tag is associated to any picture.
   *
   * @param tag The tag to check.
   * @return True if the tag is associated to at least one picture, false otherwise.
   * @throws SQLException If any database error occurs.
   */
  private boolean isTagUsed(final TagLike tag) throws SQLException {
    try (final var statement = this.connection.prepareStatement(SELECT_PICTURES_FOR_TAG_QUERY)) {
      statement.setInt(1, tag.id());
      try (final var resultSet = statement.executeQuery()) {
        return resultSet.next();
      }
    }
  }

  /**
   * Delete the given tags. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param tags The set of tags to delete.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void deleteTags(final Set<Tag> tags) throws DatabaseOperationError {
    this.deleteObjects(tags, "tags");
    // The query succeeded, update cache
    for (final var tagUpdate : tags) {
      this.tagsCache.remove(tagUpdate.id());
      this.tagsCounts.remove(tagUpdate.id());
    }
  }

  /**
   * Delete the given {@link DatabaseObject}s. This is done in a single transaction,
   * if any error occurs, the transaction is rolled back.
   *
   * @param objects   The set of objects to delete.
   * @param tableName The name of the table to delete the objects from.
   * @throws DatabaseOperationError If any database error occurs.
   */
  private <T extends DatabaseObject> void deleteObjects(
      final Set<T> objects,
      @Language(value = "sqlite", prefix = "DELETE FROM ", suffix = " WHERE 1") String tableName
  ) throws DatabaseOperationError {
    try (final var statement = this.connection.prepareStatement("DELETE FROM %s WHERE id = ?".formatted(tableName))) {
      for (final T o : objects) {
        this.ensureInDatabase(o);
        statement.setInt(1, o.id());
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    } catch (DatabaseOperationError e) {
      this.rollback();
      throw e;
    }
    this.commit();
  }

  /**
   * Fetch all images that match the given tag query.
   *
   * @param query A tag query.
   * @return The set of images that match the query.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true, value = "_ -> new")
  public Set<Picture> queryPictures(final TagQuery query) throws DatabaseOperationError {
    final Set<Picture> pictures = new HashSet<>();
    final var sql = query.asSQL();
    if (sql.isEmpty())
      return pictures;
    try (final var resultSet = this.selectQuery(sql.get())) {
      while (resultSet.next())
        pictures.add(new Picture(
            resultSet.getInt("id"),
            Path.of(resultSet.getString("path")),
            new Hash(resultSet.getLong("hash"))
        ));
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    return pictures;
  }

  @SQLite
  private static final String IMAGES_WITHOUT_TAGS_QUERY = """
      SELECT i.id, i.path, i.hash
      FROM images AS i
      WHERE (
        SELECT COUNT(*)
        FROM image_tag AS it
        WHERE it.image_id = i.id
      ) = 0
      """;

  /**
   * Fetch all images that do not have any tags.
   *
   * @return The set of all images that do not have any tags.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true, value = "-> new")
  public Set<Picture> getImagesWithNoTags() throws DatabaseOperationError {
    final Set<Picture> pictures = new HashSet<>();
    try (final var resultSet = this.selectQuery(IMAGES_WITHOUT_TAGS_QUERY)) {
      while (resultSet.next())
        pictures.add(new Picture(
            resultSet.getInt("id"),
            Path.of(resultSet.getString("path")),
            new Hash(resultSet.getLong("hash"))
        ));
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    return pictures;
  }

  @SQLite
  private static final String IMAGE_TAGS_QUERY = """
      SELECT t.id
      FROM tags AS t, image_tag AS it
      WHERE it.image_id = ?
        AND it.tag_id = t.id
      """;

  /**
   * Fetch all tags for the given image.
   *
   * @param picture The picture to fetch the tags of.
   * @return The set of all tags attached to the image.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true, value = "_ -> new")
  public Set<Tag> getImageTags(Picture picture) throws DatabaseOperationError {
    final Set<Tag> tags = new HashSet<>();
    try (final var statement = this.connection.prepareStatement(IMAGE_TAGS_QUERY)) {
      statement.setInt(1, picture.id());
      try (final var resultSet = statement.executeQuery()) {
        while (resultSet.next())
          tags.add(this.tagsCache.get(resultSet.getInt("id")));
      }
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    return tags;
  }

  @SQLite
  private static final String IMAGES_WITH_PATH_QUERY = """
      SELECT *
      FROM images
      WHERE path = ?1
      """;

  /**
   * Check whether the given file path is already registered in this database.
   * <p>
   * A path is considered registered if any picture has the <em>exact</em> same path.
   *
   * @param path The path to check.
   * @return True if the path is already registered, false otherwise.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true)
  public boolean isFileRegistered(Path path) throws DatabaseOperationError {
    try (final var statement = this.connection.prepareStatement(IMAGES_WITH_PATH_QUERY)) {
      statement.setString(1, path.toAbsolutePath().toString());
      try (final var resultSet = statement.executeQuery()) {
        return resultSet.next(); // Check if there are any rows
      }
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
  }

  @SQLite
  private static final String SIMILAR_IMAGES_QUERY = """
      SELECT id, path, hash, "SIMILARITY_CONFIDENCE"(hash, ?1) AS confidence
      FROM images
      WHERE id != ?2
        AND "SIMILAR_HASHES"(hash, ?1) = 1
      ORDER BY confidence DESC, path
      """;

  /**
   * Fetch all images that have a hash similar to the given one,
   * according to the {@link Hash#computeSimilarity(Hash)} method.
   *
   * @param hash    The reference hash.
   * @param exclude A picture that should be excluded from the result. May be null.
   * @return A list of pairs each containing a picture whose hash is similar the argument
   * and the similarity confidence index. Pairs are sorted in descending confidence index order.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true, value = "_, _ -> new")
  public List<Pair<Picture, Float>> getSimilarImages(Hash hash, @Nullable Picture exclude) throws DatabaseOperationError {
    final List<Pair<Picture, Float>> pictures = new LinkedList<>();
    try (final var statement = this.connection.prepareStatement(SIMILAR_IMAGES_QUERY)) {
      statement.setLong(1, hash.bytes());
      statement.setInt(2, exclude != null ? exclude.id() : -1);
      try (final var resultSet = statement.executeQuery()) {
        while (resultSet.next())
          pictures.add(new Pair<>(
              new Picture(
                  resultSet.getInt("id"),
                  Path.of(resultSet.getString("path")),
                  new Hash(resultSet.getLong("hash"))
              ),
              resultSet.getFloat("confidence")
          ));
      }
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    return pictures;
  }

  @SQLite
  private static final String INSERT_IMAGE_QUERY = """
      INSERT INTO images (path, hash)
      VALUES (?, ?)
      """;

  /**
   * Insert the given picture.
   *
   * @param pictureUpdate The picture to insert.
   * @throws DatabaseOperationError   If any data base error occurs.
   * @throws IllegalArgumentException If the {@code tagsToRemove} property is not empty.
   */
  public void insertPicture(PictureUpdate pictureUpdate) throws DatabaseOperationError {
    if (!pictureUpdate.tagsToRemove().isEmpty())
      throw this.logThrownError(new IllegalArgumentException("Cannot remove tags from a picture that is not yet registered"));

    final Pair<Set<Pair<Tag, Boolean>>, Set<Tag>> result;
    try (final var statement = this.connection.prepareStatement(INSERT_IMAGE_QUERY)) {
      statement.setString(1, pictureUpdate.path().toString());
      statement.setLong(2, pictureUpdate.hash().bytes());
      statement.executeUpdate();
      result = this.updatePictureTagsNoCommit(pictureUpdate);
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    } catch (DatabaseOperationError e) {
      this.rollback();
      throw e;
    }
    this.commit();
    this.updateTagsCache(result.getKey(), result.getValue());
  }

  @SQLite
  private static final String UPDATE_IMAGE_HASH_QUERY = """
      UPDATE images
      SET hash = ?1
      WHERE id = ?2
      """;

  /**
   * Update the given picture’s hash and tags.
   * <p>
   * To move picture, see {@link #movePicture(Picture, Path)}.
   * To rename a picture, see {@link #renamePicture(Picture, String)}.
   * To merge two pictures, see {@link #mergePictures(Picture, Picture, boolean)}.
   *
   * @param pictureUpdate The picture to update.
   * @throws DatabaseOperationError If any data base error occurs.
   */
  public void updatePicture(PictureUpdate pictureUpdate) throws DatabaseOperationError {
    this.ensureInDatabase(pictureUpdate);
    final Pair<Set<Pair<Tag, Boolean>>, Set<Tag>> result;
    try (final var statement = this.connection.prepareStatement(UPDATE_IMAGE_HASH_QUERY)) {
      statement.setLong(1, pictureUpdate.hash().bytes());
      statement.setInt(2, pictureUpdate.id());
      statement.executeUpdate();
      result = this.updatePictureTagsNoCommit(pictureUpdate);
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    } catch (DatabaseOperationError e) {
      this.rollback();
      throw e;
    }
    this.commit();
    this.updateTagsCache(result.getKey(), result.getValue());
  }

  @SQLite
  private static final String UPDATE_IMAGE_PATH_QUERY = """
      UPDATE images
      SET path = ?1
      WHERE id = ?2
      """;

  /**
   * Rename the given picture.
   *
   * @param picture The picture to rename.
   * @param newName The picture’s new name.
   * @throws DatabaseOperationError If any database or file system error occurs.
   */
  public void renamePicture(Picture picture, String newName) throws DatabaseOperationError {
    this.ensureInDatabase(picture);
    this.moveOrRenamePicture(picture, picture.path().getParent().resolve(newName));
  }

  /**
   * Move the given picture to another directory.
   *
   * @param picture The picture to rename.
   * @param destDir The picture’s new name.
   * @throws DatabaseOperationError If any database or file system error occurs.
   */
  public void movePicture(Picture picture, Path destDir) throws DatabaseOperationError {
    this.ensureInDatabase(picture);
    if (picture.path().getParent().equals(destDir.toAbsolutePath()))
      throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.FILE_ALREADY_IN_DEST_DIR));
    if (Files.exists(destDir.resolve(picture.path().getFileName())))
      throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.FILE_ALREADY_EXISTS_ERROR));

    this.moveOrRenamePicture(picture, destDir);
  }

  /**
   * Move/rename the given picture.
   *
   * @param picture The picture to move/rename.
   * @param newPath The destination/new name.
   * @throws DatabaseOperationError If any database or file system error occurs.
   */
  private void moveOrRenamePicture(Picture picture, Path newPath) throws DatabaseOperationError {
    try {
      Files.move(picture.path(), newPath);
    } catch (IOException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }

    try (final var statement = this.connection.prepareStatement(UPDATE_IMAGE_PATH_QUERY)) {
      statement.setString(1, newPath.toString());
      statement.setInt(2, picture.id());
      statement.executeUpdate();
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();
  }

  /**
   * Merge the tags of {@code picture1} into those of {@code picture2},
   * deleting {@code picture1} from the database and optionaly from the disk.
   *
   * @param picture1       The picture whose tags ought to be merged into those of {@code picture2}.
   * @param picture2       The picture which should receive the tags of {@code picture1}.
   * @param deleteFromDisk Whether {@code picture1} should be deleted from the disk.
   * @throws DatabaseOperationError   If any database error occurs.
   * @throws IllegalArgumentException If the two pictures have the same ID and/or path.
   */
  public void mergePictures(Picture picture1, Picture picture2, boolean deleteFromDisk)
      throws DatabaseOperationError {
    this.ensureInDatabase(picture1);
    this.ensureInDatabase(picture2);
    if (picture1.id() == picture2.id())
      throw this.logThrownError(new IllegalArgumentException("Both pictures have the same ID"));
    if (picture1.path().equals(picture2.path()))
      throw this.logThrownError(new IllegalArgumentException("Both pictures have the same path"));

    final var pic1Tags = this.getImageTags(picture1).stream()
        .map(t -> new Pair<>(t.type().orElse(null), t.label()))
        .collect(Collectors.toSet());
    final Pair<Set<Pair<Tag, Boolean>>, Set<Tag>> result;
    try {
      // Add tags of picture1 to picture2
      result = this.updatePictureTagsNoCommit(new PictureUpdate(picture2.id(), picture2.path(), picture2.hash(), pic1Tags, Set.of()));
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();
    this.updateTagsCache(result.getKey(), result.getValue());
    this.deletePicture(picture1, deleteFromDisk);
  }

  /**
   * Update the tags cache and counts.
   *
   * @param addedTags   The set of tags that were added to an image.
   *                    A boolean value of true indicates that the tag was created,
   *                    false indicates that it already existed.
   * @param removedTags The set of tags that were removed from an image.
   */
  private void updateTagsCache(final Set<Pair<Tag, Boolean>> addedTags, final Set<Tag> removedTags) {
    for (final var addedTag : addedTags) {
      final Tag tag = addedTag.getKey();
      final int tagId = tag.id();
      final boolean inserted = addedTag.getValue();
      if (inserted) {
        this.tagsCache.put(tagId, tag);
        this.tagsCounts.put(tagId, 1);
      } else
        this.tagsCounts.put(tagId, this.tagsCounts.get(tagId) + 1);
    }
    for (final var removedTag : removedTags)
      this.tagsCounts.put(removedTag.id(), this.tagsCounts.get(removedTag.id()) - 1);
  }

  @SQLite
  private static final String SELECT_TAG_FROM_LABEL_QUERY = """
      SELECT id, type_id, definition
      FROM tags
      WHERE label = ?
      """;
  @SQLite
  private static final String REMOVE_TAG_FROM_IMAGE_QUERY = """
      DELETE FROM image_tag
      WHERE image_id = ?1
        AND tag_id = ?2
      """;

  /**
   * Update the tags of the given image. This method does not perform any kind of transaction managment,
   * it is the responsablity of the caller to do so.
   *
   * @param pictureUpdate The image to update.
   * @return A pair containing the set of tags that were added to the image
   * and the set of those that were removed from it. In the left set,
   * a boolean value of true indicates that the tag was created,
   * false indicates that it already existed.
   * @throws SQLException           If any database error occurs.
   * @throws DatabaseOperationError If any database error occurs.
   */
  private Pair<Set<Pair<Tag, Boolean>>, Set<Tag>> updatePictureTagsNoCommit(PictureUpdate pictureUpdate)
      throws SQLException, DatabaseOperationError {
    // Insert tags
    final Set<Pair<Tag, Boolean>> addedTags = new HashSet<>();
    final List<TagUpdate> toInsert = new LinkedList<>();
    for (final var tagUpdate : pictureUpdate.tagsToAdd()) {
      final TagType tagType = tagUpdate.getKey();
      final String tagLabel = tagUpdate.getValue();
      final var tagOpt = this.getTagForLabel(tagLabel);
      if (tagOpt.isPresent()) {
        final Tag tag = tagOpt.get();
        if (tag.definition().isPresent())
          throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.BOUND_TAG_HAS_DEFINITION));
        this.addTagToImageNoCommit(pictureUpdate.id(), tag.id());
        addedTags.add(new Pair<>(this.tagsCache.get(tag.id()), false));
      } else
        toInsert.add(new TagUpdate(0, tagLabel, tagType, null));
    }
    final var generatedIds = this.insertTagsNoCommit(toInsert);

    // Remove tags
    try (final var statement = this.connection.prepareStatement(REMOVE_TAG_FROM_IMAGE_QUERY)) {
      statement.setInt(1, pictureUpdate.id());
      for (final var toRemove : pictureUpdate.tagsToRemove()) {
        final int tagId = toRemove.id();
        this.ensureInDatabase(toRemove);
        statement.setInt(2, tagId);
        statement.executeUpdate();
      }
    }

    final Set<Tag> removedTags = new HashSet<>();
    for (int i = 0, generatedIdsSize = generatedIds.size(); i < generatedIdsSize; i++) {
      final var generatedId = generatedIds.get(i);
      final var tagUpdate = toInsert.get(i);
      this.addTagToImageNoCommit(pictureUpdate.id(), generatedId);
      addedTags.add(new Pair<>(new Tag(generatedId, tagUpdate.label(), tagUpdate.type().orElse(null), null), true));
    }
    return new Pair<>(addedTags, removedTags);
  }

  /**
   * Return the tag for that has the given label.
   *
   * @param label A tag label.
   * @return An {@link Optional} containing the tag for the label if found, an empty {@link Optional} otherwise.
   * @throws SQLException If any database error occurs.
   */
  private Optional<Tag> getTagForLabel(String label) throws SQLException {
    try (final var statement = this.connection.prepareStatement(SELECT_TAG_FROM_LABEL_QUERY)) {
      statement.setString(1, label);
      try (final var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          final int id = resultSet.getInt("id");
          final int tagTypeId = resultSet.getInt("type_id");
          final String definition = resultSet.getString("definition");
          return Optional.of(new Tag(id, label, this.tagTypesCache.get(tagTypeId), definition));
        } else
          return Optional.empty();
      }
    }
  }

  @SQLite
  private static final String ADD_TAG_TO_IMAGE_QUERY = """
      INSERT INTO image_tag (image_id, tag_id)
      VALUES (?, ?)
      """;

  /**
   * Add a tag to an image. This method does not perform any kind of transaction managment,
   * it is the responsablity of the caller to do so.
   *
   * @param imageId The ID of the image to add the tag to.
   * @param tagId   The ID of the tag to add.
   * @throws SQLException If any database error occurs.
   */
  private void addTagToImageNoCommit(int imageId, int tagId) throws SQLException {
    try (final var statement1 = this.connection.prepareStatement(ADD_TAG_TO_IMAGE_QUERY)) {
      statement1.setInt(1, imageId);
      statement1.setInt(2, tagId);
      statement1.executeUpdate();
    }
  }

  @SQLite
  private static final String DELETE_IMAGE_QUERY = """
      DELETE FROM images
      WHERE id = ?
      """;

  /**
   * Delete the given picture from the database.
   * If the file cannot be deleted, the associated database entry will not be deleted.
   *
   * @param picture  The picture to delete.
   * @param fromDisk If true, the associated files will be deleted from the disk.
   * @throws DatabaseOperationError If any database or file system error occurs.
   */
  public void deletePicture(final Picture picture, boolean fromDisk) throws DatabaseOperationError {
    this.ensureInDatabase(picture);
    if (fromDisk) {
      try {
        Files.delete(picture.path());
      } catch (IOException e) {
        throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
      }
    }

    try (final var statement = this.connection.prepareStatement(DELETE_IMAGE_QUERY)) {
      statement.setInt(1, picture.id());
      statement.executeUpdate();
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
    this.commit();

    // Update tag counts
    this.getImageTags(picture)
        .forEach(imageTag -> this.tagsCounts.put(imageTag.id(), this.tagsCounts.get(imageTag.id()) - 1));
  }

  @SQLite
  private static final String SELECT_OBJECT_BY_ID_QUERY = """
      SELECT *
      FROM %s
      WHERE id = ?
      """;

  /**
   * Ensure that the given object exists in the database based on its ID.
   *
   * @param element The object to check.
   * @throws DatabaseOperationError If the object is not in the database or any database error occurs.
   */
  private void ensureInDatabase(final DatabaseElement element) throws DatabaseOperationError {
    @Language(value = "sqlite", prefix = "SELECT * FROM ", suffix = " WHERE 1")
    final String tableName;
    if (element instanceof TagTypeLike)
      tableName = "tag_types";
    else if (element instanceof TagLike)
      tableName = "tags";
    else if (element instanceof PictureLike)
      tableName = "images";
    else
      throw this.logThrownError(new IllegalArgumentException("Unsupported type: " + element.getClass().getName()));

    try (final var statement = this.connection.prepareStatement(SELECT_OBJECT_BY_ID_QUERY.formatted(tableName))) {
      statement.setInt(1, element.id());
      try (final var resultSet = statement.executeQuery()) {
        if (!resultSet.next())
          throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.OBJECT_DOES_NOT_EXIST));
      }
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
  }

  @Override
  public void close() throws DatabaseOperationError {
    try {
      this.connection.close();
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
  }

  /**
   * Setup the database using the file at {@link #SETUP_FILE_NAME}.
   */
  private void setupDatabase() throws SQLException, IOException {
    this.logger.info("Creating database file…");
    final InputStream stream = this.getClass().getResourceAsStream(SETUP_FILE_NAME);
    if (stream == null)
      throw this.logThrownError(new IOException("Missing file: %s".formatted(SETUP_FILE_NAME)));
    final StringBuilder query = new StringBuilder();
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      for (String line; (line = reader.readLine()) != null; )
        query.append(line);
    }
    this.executeQuery(query.toString());
    this.logger.info("Done");
  }

  /**
   * Initalize the internal tag and tag type caches.
   */
  private void initCaches() throws IOException {
    try (final var resultSet = this.selectQuery("SELECT id, label, symbol, color FROM tag_types")) {
      while (resultSet.next()) {
        final int id = resultSet.getInt("id");
        this.tagTypesCache.put(id, new TagType(
            id,
            resultSet.getString("label"),
            resultSet.getString("symbol").charAt(0),
            resultSet.getInt("color")
        ));
      }
    } catch (SQLException e) {
      throw this.logThrownError(new IOException(e));
    }

    try (final var resultSet = this.selectQuery("SELECT id, label, type_id, definition FROM tags")) {
      while (resultSet.next()) {
        final int id = resultSet.getInt("id");
        this.tagsCache.put(id, new Tag(
            id,
            resultSet.getString("label"),
            this.tagTypesCache.get(resultSet.getInt("type_id")),
            resultSet.getString("definition")
        ));
      }
    } catch (SQLException e) {
      throw this.logThrownError(new IOException(e));
    }
  }

  /**
   * Execute the given non-{@code SELECT} SQL query in a single transaction.
   * <p>
   * If the method throws a {@link SQLException}, the transaction is rollbacked.
   *
   * @param query The SQL query to execute, may contain several statements.
   */
  private void executeQuery(@SQLite String query) throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      statement.execute(query);
    } catch (SQLException e) {
      this.connection.rollback();
      throw this.logThrownError(e);
    }
    this.connection.commit();
  }

  /**
   * Execute a SQL {@code SELECT} query.
   *
   * @param query The SQL query to execute.
   * @return The rows returned by the given query in a {@link ResultSet} object.
   */
  @Contract(pure = true, value = "_ -> new")
  private ResultSet selectQuery(@SQLite String query) throws SQLException {
    try (final var statement = this.connection.createStatement()) {
      return statement.executeQuery(query);
    }
  }

  /**
   * Rollback the current transaction.
   *
   * @throws DatabaseOperationError If any database error occurs.
   */
  private void rollback() throws DatabaseOperationError {
    try {
      this.connection.rollback();
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
  }

  /**
   * Commit the current transaction.
   *
   * @throws DatabaseOperationError If any database error occurs.
   */
  private void commit() throws DatabaseOperationError {
    try {
      this.connection.commit();
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getErrorCode(e), e));
    }
  }

  /**
   * Log an exception that is being thrown by a method of this class.
   *
   * @param e The exception to log.
   * @return The passed exception.
   */
  @Contract("_ -> param1")
  private <E extends Exception> E logThrownError(final E e) {
    this.logger.throwing(this.getClass().getName(), ReflectionUtils.getCallingMethodName(), e);
    return e;
  }

  /**
   * Log an exception that was caught.
   *
   * @param e The exception to log.
   */
  private void logCaughtError(final Exception e) {
    final var stackTrack = new StringWriter();
    e.printStackTrace(new PrintWriter(stackTrack));
    this.logger.severe("Caught exception in method %s:\n%s".formatted(ReflectionUtils.getCallingMethodName(), stackTrack));
  }

  /**
   * Return a {@link DatabaseErrorCode} for the given {@link SQLException}.
   *
   * @param e The exception to get a code for.
   * @return The code for that exception.
   */
  private static DatabaseErrorCode getErrorCode(final SQLException e) {
    if (e instanceof org.sqlite.SQLiteException ex)
      return DatabaseErrorCode.forSQLiteCode(ex.getResultCode());
    return DatabaseErrorCode.UNKNOWN_ERROR;
  }

  /**
   * Get the {@link DatabaseErrorCode} for the given {@link IOException}.
   *
   * @param e The exception to get a code for.
   * @return The code for that exception.
   */
  private static DatabaseErrorCode getErrorCode(final IOException e) {
    if (e instanceof FileAlreadyExistsException)
      return DatabaseErrorCode.FILE_ALREADY_EXISTS_ERROR;
    if (e instanceof FileNotFoundException)
      return DatabaseErrorCode.MISSING_FILE_ERROR;
    return DatabaseErrorCode.UNKNOWN_ERROR;
  }

  /**
   * Return the first key generated by the given statement.
   *
   * @param statement The statement to retrieve the generated keys from.
   * @return An {@link Optional} containing the first key generated by the statement
   * or an empty {@link Optional} if the statement did not generate any key.
   */
  private static Optional<Integer> getFirstGeneratedId(Statement statement) throws SQLException {
    try (final var generatedKeys = statement.getGeneratedKeys()) {
      if (!generatedKeys.next())
        return Optional.empty();
      return Optional.of(generatedKeys.getInt(1));
    }
  }

  /**
   * Convert a database file created by the Python app to this app’s format.
   * The original file remains unchanged and the converted database is written to a new file.
   *
   * @param path The path to the file to convert.
   * @return The converted file.
   */
  public static Path convertPythonDatabase(final Path path) throws SQLException, IOException {
    // TODO
    return null;
  }
}
