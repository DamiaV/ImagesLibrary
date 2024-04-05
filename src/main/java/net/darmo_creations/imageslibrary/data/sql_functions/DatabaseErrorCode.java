package net.darmo_creations.imageslibrary.data.sql_functions;

import net.darmo_creations.imageslibrary.io.*;

/**
 * This enum lists all available database error codes.
 */
public enum DatabaseErrorCode {
  UNKNOWN_ERROR,

  OBJECT_ALREADY_EXISTS,

  // SQL-related errors
  // TODO

  // File-related errors
  UNKNOWN_FILE_ERROR,
  MISSING_FILE_ERROR,
  MISSING_DIRECTORY_ERROR,
  MISSING_PERMISSIONS_ERROR,
  IS_NOT_FILE_ERROR,
  IS_NOT_DIRECTORY_ERROR,
  FILE_ALREADY_EXISTS_ERROR,
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

  /**
   * Get the error code for the given {@link FileOperationErrorCode}.
   *
   * @param code A file operation error code.
   * @return The corresponding {@link DatabaseErrorCode}.
   */
  public static DatabaseErrorCode forFileOperationErrorCode(FileOperationErrorCode code) {
    return switch (code) {
      case MISSING_FILE_ERROR -> MISSING_FILE_ERROR;
      case MISSING_DIRECTORY_ERROR -> MISSING_DIRECTORY_ERROR;
      case MISSING_PERMISSIONS_ERROR -> MISSING_PERMISSIONS_ERROR;
      case IS_NOT_FILE_ERROR -> IS_NOT_FILE_ERROR;
      case IS_NOT_DIRECTORY_ERROR -> IS_NOT_DIRECTORY_ERROR;
      case FILE_ALREADY_EXISTS_ERROR -> FILE_ALREADY_EXISTS_ERROR;
      case UNKNOWN_ERROR -> UNKNOWN_FILE_ERROR;
    };
  }
}
