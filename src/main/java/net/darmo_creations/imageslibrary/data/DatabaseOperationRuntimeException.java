package net.darmo_creations.imageslibrary.data;

public class DatabaseOperationRuntimeException extends RuntimeException {
  private final DatabaseErrorCode errorCode;

  public DatabaseOperationRuntimeException(DatabaseErrorCode errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  public DatabaseErrorCode errorCode() {
    return this.errorCode;
  }
}
