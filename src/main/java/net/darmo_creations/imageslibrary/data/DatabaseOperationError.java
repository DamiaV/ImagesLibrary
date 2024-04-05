package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.data.sql_functions.*;

public class DatabaseOperationError extends Exception {
  private final DatabaseErrorCode errorCode;

  public DatabaseOperationError(DatabaseErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public DatabaseOperationError(DatabaseErrorCode errorCode, Throwable cause) {
    super(cause);
    this.errorCode = errorCode;
  }

  public DatabaseErrorCode errorCode() {
    return this.errorCode;
  }
}
