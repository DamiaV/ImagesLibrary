package net.darmo_creations.imageslibrary.io;

/**
 * This enum lists all error code are accepted by the {@link FileOperationError} class.
 */
public enum FileOperationErrorCode {
  MISSING_FILE_ERROR,
  MISSING_DIRECTORY_ERROR,
  MISSING_PERMISSIONS_ERROR,
  IS_NOT_FILE_ERROR,
  IS_NOT_DIRECTORY_ERROR,
  FILE_ALREADY_EXISTS_ERROR,
  UNKNOWN_ERROR,
}
