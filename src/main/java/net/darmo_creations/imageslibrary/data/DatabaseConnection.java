package net.darmo_creations.imageslibrary.data;

import javafx.util.*;
import net.darmo_creations.imageslibrary.data.sql_functions.*;
import net.darmo_creations.imageslibrary.io.*;
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
          WHERE "REGEX"(SUBSTR(path, "RINSTR"(path, '/') + 1), '%s', '%s') -- TODO use system separator?
          """, true),

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
  private final FilesManager filesManager;

  // TODO cache tag and tag type use counts
  private final Map<Integer, TagType> tagTypesCache = new HashMap<>();
  private final Map<Integer, Tag> tagsCache = new HashMap<>();

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
    this.filesManager = new FilesManager();
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
    final Map<Integer, TagTypeUpdate> toInsert = new HashMap<>();

    try (final var statement = this.connection.prepareStatement(INSERT_TAG_TYPES_QUERY, Statement.RETURN_GENERATED_KEYS)) {
      for (final var tagTypeUpdate : tagTypeUpdates) {
        statement.setString(1, tagTypeUpdate.label());
        statement.setString(2, String.valueOf(tagTypeUpdate.symbol()));
        statement.setInt(3, tagTypeUpdate.color());
        statement.executeUpdate();
        final var id = getFirstGeneratedId(statement);
        if (id.isEmpty())
          throw this.logThrownError(new SQLException("Query did not generate any key"));
        toInsert.put(id.get(), tagTypeUpdate);
      }
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
    this.commit();

    // The query succeeded, update cache
    for (final var entry : toInsert.entrySet()) {
      final int id = entry.getKey();
      final TagTypeUpdate tagTypeUpdate = entry.getValue();
      this.tagTypesCache.put(id, new TagType(id, tagTypeUpdate.label(), tagTypeUpdate.symbol(), tagTypeUpdate.color()));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
    this.commit();

    // The query succeeded, update cache
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
    // The query succeeded, update cache and unparsedTags
    for (final var tagType : tagTypes) {
      this.tagTypesCache.remove(tagType.id());
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
    this.commit();
    final var insertions = IntStream.range(0, tagUpdates.size())
        .mapToObj(i -> tagUpdates.get(i).withId(generatedIds.get(i)))
        .collect(Collectors.toSet());
    this.updateTagCaches(insertions, Set.of());
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
        if (tagUpdate.type() == null)
          statement.setNull(2, Types.INTEGER);
        else
          statement.setInt(2, tagUpdate.type().id());
        statement.setString(3, tagUpdate.definition());
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
  public void updateTags(final Set<TagUpdate> tagUpdates) throws DatabaseOperationError {
    try {
      this.updateTagsNoCommit(tagUpdates);
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
    this.commit();
    this.updateTagCaches(Set.of(), tagUpdates);
  }

  /**
   * Update the given tags. This method does not perform any kind of transaction managment,
   * it is the responsablity of the caller to do so.
   *
   * @param tagUpdates The list of tag updates to perform. Updates are performed in the order of the list.
   * @throws SQLException If a tag with the same label already exists in the database, or any database error occurs.
   */
  private void updateTagsNoCommit(final Set<TagUpdate> tagUpdates) throws SQLException {
    try (final var statement = this.connection.prepareStatement(UPDATE_TAGS_QUERY)) {
      for (final var tagUpdate : tagUpdates) {
        final String label = tagUpdate.label();
        final int id = tagUpdate.id();
        statement.setString(1, label);
        if (tagUpdate.type() == null)
          statement.setNull(2, Types.INTEGER);
        else
          statement.setInt(2, tagUpdate.type().id());
        statement.setString(3, tagUpdate.definition());
        statement.setInt(4, id);
        if (statement.executeUpdate() == 0)
          throw this.logThrownError(new SQLException("No tag with ID %d".formatted(id)));
      }
    }
  }

  /**
   * Update the tags cache by inserting/updating the specified tags.
   *
   * @param toInsert The tags to insert.
   * @param toUpdate The tags to update.
   */
  private void updateTagCaches(final Set<TagUpdate> toInsert, final Set<TagUpdate> toUpdate) {
    for (final var tagUpdate : toInsert) {
      final int id = tagUpdate.id();
      final var tagType = tagUpdate.type() != null ? this.tagTypesCache.get(tagUpdate.type().id()) : null;
      this.tagsCache.put(id, new Tag(id, tagUpdate.label(), tagType, tagUpdate.definition()));
    }
    for (final var tagUpdate : toUpdate) {
      final Tag tag = this.tagsCache.get(tagUpdate.id());
      tag.setLabel(tagUpdate.label());
      tag.setType(tagUpdate.type());
      tag.setDefinition(tagUpdate.definition());
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
    for (final var tagUpdate : tags)
      this.tagsCache.remove(tagUpdate.id());
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
        statement.setInt(1, o.id());
        statement.executeUpdate();
      }
    } catch (SQLException e) {
      this.rollback();
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
    return pictures;
  }

  @SQLite
  private static final String SELECT_IMAGE_PATH_QUERY = """
      SELECT id, hash
      FROM images
      WHERE path = ?
      """;

  /**
   * Look for the picture with the given path.
   *
   * @param path The path to look for.
   * @return An {@link Optional} containing the picture, or an empty {@link Optional} if no row matched.
   * @throws DatabaseOperationError If any database error occurs.
   */
  @Contract(pure = true, value = "_ -> new")
  private Optional<Picture> getPictureForPath(Path path) throws DatabaseOperationError {
    try (final var statement = this.connection.prepareStatement(SELECT_IMAGE_ID_QUERY)) {
      final Path absolutePath = path.toAbsolutePath();
      statement.setString(1, absolutePath.toString());
      try (final var resultSet = statement.executeQuery()) {
        if (resultSet.next())
          return Optional.of(
              new Picture(resultSet.getInt("id"), absolutePath, new Hash(resultSet.getLong("hash"))));
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
    }
  }

  @SQLite
  private static final String INSERT_IMAGE_QUERY = """
      INSERT INTO images (path, hash)
      VALUES (?, ?)
      """;

  @SQLite
  private static final String UPDATE_IMAGE_QUERY = """
      UPDATE images
      SET path = ?1, hash = ?2
      WHERE id = ?3
      """;

  @SQLite
  private static final String PURGE_TAGS_FOR_IMAGE_QUERY = """
      DELETE FROM image_tag
      WHERE image_id = ?
      """;

  /**
   * Insert the given picture. Picture and tag updates are done in a single transaction.
   * If the picture could not be moved or any database error occurs, the transaction is rolled back.
   *
   * @param pictureUpdate The picture to insert.
   * @param destDir       If not null, the path to the folder into which the picture file should be moved.
   * @param resolver      If {@code destDir} is not null, a function the returns how the name collision
   *                      for the two given source and target files should be resolved.
   * @return True if the file was inserted, false otherwise (likely skipped after a name conflict).
   * @throws DatabaseOperationError   If any database error occurs or there was an unsolved file name conflict.
   * @throws NullPointerException     If {@code resolution} is null but {@code destDir} is not.
   * @throws IllegalArgumentException If the {@code tagsToRemove} property is not empty.
   */
  @Contract("_, !null, null -> fail")
  public boolean insertPicture(
      PictureUpdate pictureUpdate,
      @Nullable Path destDir,
      @Nullable FilesManager.FileNameConflictResolutionProvider resolver
  ) throws DatabaseOperationError {
    if (destDir != null && resolver == null)
      throw this.logThrownError(new NullPointerException("Missing file name conflict resolution provider"));
    if (!pictureUpdate.tagsToRemove().isEmpty())
      throw this.logThrownError(new IllegalArgumentException("Cannot remove tags from a picture that is not yet registered"));
    if (this.isFileRegistered(pictureUpdate.path()))
      throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.OBJECT_ALREADY_EXISTS));

    Picture overwrittenPicture = null;
    OverwrittenTagsHandling tagsHandling = null;
    if (destDir != null) {
      final FileMovingOutcome result;
      try {
        result = this.filesManager.moveFile(pictureUpdate.path(), destDir, resolver);
      } catch (FileOperationError e) {
        throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.forFileOperationErrorCode(e.errorCode()), e));
      }

      final var resolution = result.resolution();
      if (resolution.isPresent()) {
        final var res = resolution.get();
        if (res instanceof Skip) {
          return false;
        } else if (res instanceof Overwrite overwrite) {
          final var picture = this.getPictureForPath(result.newPath());
          if (picture.isPresent()) {
            overwrittenPicture = picture.get();
            tagsHandling = overwrite.overwrittenTagsHandling();
          }
        } else if (res instanceof Rename) {
          pictureUpdate = new PictureUpdate(
              pictureUpdate.id(),
              result.newPath(),
              pictureUpdate.hash(),
              pictureUpdate.tagsToAdd(),
              pictureUpdate.tagsToRemove()
          );
        } else {
          throw this.logThrownError(new RuntimeException("Invalid resolution type: " + res.getClass().getName()));
        }
      }
    }

    boolean ignoreTags = false;
    if (tagsHandling != null) {
      switch (tagsHandling) {
        case KEEP_ORIGINAL_TAGS -> ignoreTags = true;
        case MERGE_TAGS -> {
          // Nothing to do here
        }
        case REPLACE_ORIGINAL_TAGS -> {
          // Remove all tags from the overwritten image
          try (final var statement = this.connection.prepareStatement(PURGE_TAGS_FOR_IMAGE_QUERY)) {
            statement.setInt(1, overwrittenPicture.id());
            statement.executeUpdate();
          } catch (SQLException e) {
            this.rollback();
            throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
          }
        }
      }
      // We overwrote an already registered file, update its entry instead of creating a new one
      try (final var statement = this.connection.prepareStatement(UPDATE_IMAGE_QUERY)) {
        statement.setString(1, pictureUpdate.path().toString());
        statement.setLong(2, pictureUpdate.hash().bytes());
        statement.setInt(3, pictureUpdate.id());
        statement.executeUpdate();
        if (!ignoreTags)
          this.updatePictureTags(pictureUpdate);
      } catch (SQLException e) {
        this.rollback();
        throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
      }
    } else {
      try (final var statement = this.connection.prepareStatement(INSERT_IMAGE_QUERY)) {
        statement.setString(1, pictureUpdate.path().toString());
        statement.setLong(2, pictureUpdate.hash().bytes());
        statement.executeUpdate();
        this.updatePictureTags(pictureUpdate);
      } catch (SQLException e) {
        this.rollback();
        throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
      }
    }

    this.commit();
    return true;
  }

  @SQLite
  private static final String UPDATE_IMAGE_HASH_QUERY = """
      UPDATE images
      SET hash = ?1
      WHERE id = ?2
      """;

  /**
   * Update the given picture. Picture and tag updates are done in a single transaction.
   * If the picture could not be moved or any database error occurs, the transaction is rolled back.
   *
   * @param pictureUpdate The picture to update.
   * @param destDir       If not null, the path to the folder into which the picture file should be moved.
   * @param resolver      If {@code destDir} is not null, a function the returns how the name collision
   *                      for the two given source and target files should be resolved.
   * @return True if the file was updated, false otherwise (likely skipped after a name conflict).
   * @throws DatabaseOperationError If any database error occurs or there was an unsolved file name conflict.
   * @throws NullPointerException   If {@code resolution} is null but {@code destDir} is not.
   */
  @Contract("_, !null, null -> fail")
  public boolean updatePicture(
      PictureUpdate pictureUpdate,
      @Nullable Path destDir,
      @Nullable FilesManager.FileNameConflictResolutionProvider resolver
  ) throws DatabaseOperationError {
    if (destDir != null && resolver == null)
      throw new NullPointerException("Missing file name conflict resolution provider");

    Picture overwrittenPicture = null;
    OverwrittenTagsHandling tagsHandling = null;
    if (destDir != null) {
      final FileMovingOutcome result;
      try {
        result = this.filesManager.moveFile(pictureUpdate.path(), destDir, resolver);
      } catch (FileOperationError e) {
        throw this.logThrownError(new DatabaseOperationError(DatabaseErrorCode.forFileOperationErrorCode(e.errorCode()), e));
      }

      final var resolution = result.resolution();
      if (resolution.isPresent()) {
        final var res = resolution.get();
        if (res instanceof Skip) {
          return false;
        } else if (res instanceof Overwrite overwrite) {
          final var picture = this.getPictureForPath(result.newPath());
          if (picture.isPresent()) {
            overwrittenPicture = picture.get();
            tagsHandling = overwrite.overwrittenTagsHandling();
          }
        } else if (res instanceof Rename) {
          pictureUpdate = new PictureUpdate(
              pictureUpdate.id(),
              result.newPath(),
              pictureUpdate.hash(),
              pictureUpdate.tagsToAdd(),
              pictureUpdate.tagsToRemove()
          );
        } else {
          throw this.logThrownError(new RuntimeException("Invalid resolution type: " + res.getClass().getName()));
        }
      }
    }

    boolean ignoreTags = false;
    if (tagsHandling != null) {
      switch (tagsHandling) {
        case KEEP_ORIGINAL_TAGS -> ignoreTags = true;
        case MERGE_TAGS -> {
          // Nothing to do here
        }
        case REPLACE_ORIGINAL_TAGS -> {
          // Remove all tags from the overwritten image
          try (final var statement = this.connection.prepareStatement(PURGE_TAGS_FOR_IMAGE_QUERY)) {
            statement.setInt(1, overwrittenPicture.id());
            statement.executeUpdate();
          } catch (SQLException e) {
            this.rollback();
            throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
          }
        }
      }
      try (final var statement = this.connection.prepareStatement(UPDATE_IMAGE_QUERY)) {
        statement.setString(1, pictureUpdate.path().toString());
        statement.setLong(2, pictureUpdate.hash().bytes());
        statement.setInt(3, pictureUpdate.id());
        statement.executeUpdate();
        if (!ignoreTags)
          this.updatePictureTags(pictureUpdate);
      } catch (SQLException e) {
        this.rollback();
        throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
      }
    } else {
      try (final var statement = this.connection.prepareStatement(UPDATE_IMAGE_HASH_QUERY)) {
        statement.setLong(1, pictureUpdate.hash().bytes());
        statement.setInt(2, pictureUpdate.id());
        statement.executeUpdate();
        this.updatePictureTags(pictureUpdate);
      } catch (SQLException e) {
        this.rollback();
        throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
      }
    }
    this.commit();

    return true;
  }

  @SQLite
  private static final String ADD_TAG_TO_IMAGE_QUERY = """
      INSERT INTO image_tag (image_id, tag_id)
      VALUES (?, ?)
      """;
  @SQLite
  private static final String REMOVE_TAG_FROM_IMAGE_QUERY = """
      DELETE FROM image_tag
      WHERE image_id = ?1
        AND tag_id = ?2
      """;

  private void updatePictureTags(PictureUpdate pictureUpdate) throws SQLException {
    // TODO insert new tags
    // TODO add tags to picture
    try (final var statement = this.connection.prepareStatement(REMOVE_TAG_FROM_IMAGE_QUERY)) {
      statement.setInt(1, pictureUpdate.id());
      for (final var toRemove : pictureUpdate.tagsToRemove()) {
        statement.setInt(2, toRemove.id());
        statement.executeUpdate();
      }
    }
  }

  @SQLite
  private static final String MOVE_IMAGE_QUERY = """
      UPDATE images
      SET path = ?1
      WHERE id = ?2
      """;

  /**
   * Move the given pictures into the given folder.
   *
   * @param pictures   The picture to move.
   * @param destDir    The path to the folder into which the picture files should be moved.
   * @param resolution A function the returns how the name collision
   *                   for the two given source and target files should be resolved.
   * @return A set of the pictures that could not be moved or were skipped.
   * @throws SQLException If any database error occurs.
   */
  @Contract("_, _, _ -> new")
  public Set<Picture> movePictures(
      final Set<Picture> pictures,
      Path destDir,
      FilesManager.FileNameConflictResolutionProvider resolution
  ) throws SQLException {
    final Set<Picture> nonMovedPictures = new HashSet<>();
    for (final var picture : pictures) {
      try (final var statement = this.connection.prepareStatement(MOVE_IMAGE_QUERY)) {
        statement.setString(1, picture.path().toString());
        statement.setInt(2, picture.id());
      } catch (SQLException e) {
        this.logger.severe("Could not move picture: " + picture.path());
        this.connection.rollback();
        nonMovedPictures.add(picture);
        continue;
      }
      try {
        this.movePictureFile(picture, destDir, resolution, picture.id());
      } catch (SQLException | IOException e) {
        this.logger.severe("Could not move picture: " + picture.path());
        this.connection.rollback(); // File could not be moved, cancel everything
        nonMovedPictures.add(picture);
      } catch (UnknownDatabaseErrorException e) {
        throw new RuntimeException(e);
      }
      this.connection.commit();
    }
    return nonMovedPictures;
  }

  /**
   * Merges the tags of {@code picture1} into those of {@code picture2},
   * deleting {@code picture1} from the database and optionaly from the disk.
   *
   * @param picture1       The picture whose tags ought to be merged into those of {@code picture2}.
   * @param picture2       The picture which should receive the tags of {@code picture1}.
   * @param deleteFromDisk Whether {@code picture1} should be deleted from the disk.
   * @throws DatabaseOperationError If any database error occurs.
   */
  public void mergePictures(Picture picture1, Picture picture2, boolean deleteFromDisk)
      throws DatabaseOperationError {
    // TODO
  }

  @SQLite
  private static final String SELECT_IMAGE_ID_QUERY = """
      SELECT id
      FROM images
      WHERE path = ?
      """;

  /**
   * Move the specified file into the given directory.
   *
   * @param picture       The file to move.
   * @param destDir       The directory where to move the file.
   * @param resolution    A function the returns how the name collision
   *                      for the two given source and target files should be resolved.
   * @param sourceImageId The database ID of the file being moved.
   * @throws IOException If any file management error occurs.
   */
  private void movePictureFile(Picture picture, Path destDir, FilesManager.FileNameConflictResolutionProvider resolution, int sourceImageId)
      throws SQLException, IOException, UnknownDatabaseErrorException {
    final Path targetFile = destDir.toAbsolutePath().resolve(picture.path().getFileName());
    if (Files.exists(targetFile)) {
      final int targetImageId;
      try (final var statement = this.connection.prepareStatement(SELECT_IMAGE_ID_QUERY)) {
        statement.setString(1, picture.path().toString());
        try (final var resultSet = statement.executeQuery()) {
          if (resultSet.next())
            targetImageId = resultSet.getInt("id");
          else
            targetImageId = 0;
        }
      }

      boolean solved;
      do {
        final var res = resolution.forFiles(picture.path(), targetFile);
        if (res instanceof Skip) {
          solved = true;
        } else if (res instanceof Overwrite overwrite) {
          this.overwriteFile(picture, targetFile, overwrite.overwrittenTagsHandling());
          solved = true;
        } else if (res instanceof Rename rename) {
          solved = this.renameFile(picture, rename.newName());
        } else {
          throw new RuntimeException("Invalid resolution type: " + res.getClass().getName());
        }
      } while (!solved);

    } else {
      final Path newPath = Files.move(picture.path(), destDir).toAbsolutePath();
      this.updateImagePath(new PictureUpdate(picture.id(), newPath, picture.hash(), Set.of(), Set.of()));
    }
  }

  private void overwriteFile(Picture picture, Path targetFile, OverwrittenTagsHandling overwrittenTagsHandling)
      throws SQLException, IOException, UnknownDatabaseErrorException {
    final Path newPath = Files.move(picture.path(), targetFile, StandardCopyOption.REPLACE_EXISTING);
    if (this.isFileRegistered(newPath.toAbsolutePath().toString())) {
      switch (overwrittenTagsHandling) {
        case KEEP_ORIGINAL_TAGS -> {
          // Nothing to do
        }
        case MERGE_TAGS -> {
          // TODO
        }
        case REPLACE_ORIGINAL_TAGS -> {
          // TODO
        }
      }
    }
  }

  @SQLite
  private static final String RENAME_IMAGE_QUERY = """
      UPDATE images
      SET path = ?1, hash = ?2
      WHERE id = ?3
      """;

  /**
   * Rename the given file to the specified name in the file system and database if it has an entry.
   *
   * @param picture The picture to rename. Specify an ID < 1 if the file is not in the database.
   * @param newName The file’s new name.
   * @return True if the file was renamed, false otherwise.
   */
  private boolean renameFile(Picture picture, String newName) throws SQLException {
    final Path newPath = picture.path().getParent().toAbsolutePath().resolve(newName);
    try {
      Files.move(picture.path(), newPath, StandardCopyOption.REPLACE_EXISTING);
      // Check if the image has an entry in the database, if so, update its path
      if (picture.id() >= 1)
        this.updateImagePath(new PictureUpdate(picture.id(), newPath, picture.hash(), Set.of(), Set.of()));
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  /**
   * Update the path column of the given image.
   *
   * @param pictureUpdate The picture to update.
   * @throws SQLException If any database error occurs.
   */
  private void updateImagePath(PictureUpdate pictureUpdate) throws SQLException, IOException {
    try (final var statement = this.connection.prepareStatement(RENAME_IMAGE_QUERY)) {
      statement.setString(1, pictureUpdate.path().toString());
      statement.setLong(2, pictureUpdate.hash().bytes());
      statement.setInt(3, pictureUpdate.id());
      statement.executeUpdate();
    }
  }

  @SQLite
  private static final String DELETE_IMAGE_QUERY = """
      DELETE FROM images
      WHERE id = ?
      """;

  /**
   * Delete the given pictures from the database. Each picture deletion is done in separate transactions.
   * If a file cannot be deleted, the associated database entry will not be deleted.
   *
   * @param pictures The pictures to delete.
   * @param fromDisk If true, the associated files will be deleted from the disk.
   * @return The set of images that were deleted.
   */
  public Set<Picture> deletePictures(final Set<Picture> pictures, boolean fromDisk) throws DatabaseOperationError {
    final Set<Picture> deletedPictures = new HashSet<>();
    for (final var picture : pictures) {
      boolean proceed = true;
      try (final var statement = this.connection.createStatement()) {
        statement.execute(DELETE_IMAGE_QUERY);
      } catch (SQLException e) {
        this.logCaughtError(e);
        this.rollback();
        proceed = false;
      }
      this.commit();
      if (proceed && fromDisk) {
        final Path path = picture.path();
        try {
          this.filesManager.deleteFile(path);
        } catch (FileOperationError e) {
          this.logCaughtError(e);
          proceed = false;
        }
      }
      if (proceed)
        deletedPictures.add(picture);
    }
    return deletedPictures;
  }

  @Override
  public void close() throws SQLException {
    try {
      this.connection.close();
    } catch (SQLException e) {
      throw this.logThrownError(e);
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
      throw this.logThrownError(new DatabaseOperationError(getStatusCode(e), e));
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
   * @param e An exception.
   * @return The code for that exception.
   */
  private static DatabaseErrorCode getStatusCode(SQLException e) {
    if (e instanceof org.sqlite.SQLiteException ex)
      return DatabaseErrorCode.forSQLiteCode(ex.getResultCode());
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
