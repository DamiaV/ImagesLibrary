package net.darmo_creations.imageslibrary.data.sql_functions;

import org.junit.jupiter.api.*;

import java.sql.*;

public abstract class FunctionTest<T extends org.sqlite.Function> {
  protected Connection connection;

  @BeforeEach
  public void setUp() throws Exception {
    this.connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    final T function = this.getFunction();
    final var annotation = function.getClass().getAnnotation(SqlFunction.class);
    org.sqlite.Function.create(this.connection, annotation.name(), function, annotation.nArgs(), annotation.flags());
  }

  @AfterEach
  public void tearDown() throws Exception {
    this.connection.close();
  }

  protected abstract T getFunction();
}
