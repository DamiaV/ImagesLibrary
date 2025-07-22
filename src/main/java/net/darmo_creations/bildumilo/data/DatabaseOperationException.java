package net.darmo_creations.bildumilo.data;

public class DatabaseOperationException extends Exception {
  private final DatabaseErrorCode errorCode;

  public DatabaseOperationException(DatabaseErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public DatabaseOperationException(DatabaseErrorCode errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  public DatabaseErrorCode errorCode() {
    return this.errorCode;
  }
}
