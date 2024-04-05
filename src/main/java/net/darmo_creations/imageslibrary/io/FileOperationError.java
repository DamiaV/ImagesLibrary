package net.darmo_creations.imageslibrary.io;

/**
 * This exception is raised to signal that an error occured while handling files.
 */
public class FileOperationError extends Exception {
  private final FileOperationErrorCode errorCode;

  /**
   * Create a new exception.
   *
   * @param errorCode The code describing the error.
   */
  public FileOperationError(FileOperationErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Create a new exception.
   *
   * @param errorCode The code describing the error.
   * @param cause     The cause of this exception.
   */
  public FileOperationError(FileOperationErrorCode errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  /**
   * The code describing the error.
   */
  public FileOperationErrorCode errorCode() {
    return this.errorCode;
  }
}
