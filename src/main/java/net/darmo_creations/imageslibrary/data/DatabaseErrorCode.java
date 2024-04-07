package net.darmo_creations.imageslibrary.data;

/**
 * This enum lists all available database error codes.
 */
public enum DatabaseErrorCode {
  UNKNOWN_ERROR,

  // Assertion-related errors
  OBJECT_DOES_NOT_EXIST,
  BOUND_TAG_HAS_DEFINITION,

  // SQLite errors
  // TODO

  // File-related errors
  UNKNOWN_FILE_ERROR,
  MISSING_FILE_ERROR,
  MISSING_DIRECTORY_ERROR,
  MISSING_PERMISSIONS_ERROR,
  IS_NOT_FILE_ERROR,
  IS_NOT_DIRECTORY_ERROR,
  FILE_ALREADY_EXISTS_ERROR,
  FILE_ALREADY_IN_DEST_DIR,
  ;

  /**
   * Get the error code for the given {@link org.sqlite.SQLiteErrorCode}.
   *
   * @param code A SQLite error code.
   * @return The corresponding {@link DatabaseErrorCode}.
   */
  public static DatabaseErrorCode forSQLiteCode(org.sqlite.SQLiteErrorCode code) {
    return null; // TODO
  }
}
