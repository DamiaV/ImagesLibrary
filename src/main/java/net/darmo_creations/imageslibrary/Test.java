package net.darmo_creations.imageslibrary;

import java.sql.*;

public class Test {
  public static void main(String[] args) {
    try {
      var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
      Statement statement = connection.createStatement();
      statement.execute("CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT NOT NULL )");
      statement.execute("INSERT INTO test VALUES(1, 'test1')");
      statement.execute("INSERT INTO test VALUES(2, 'test2')");
      statement.execute("INSERT INTO test VALUES(1, 'test3')");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void test1() {
    final var stacktrace = Thread.currentThread().getStackTrace();
    final var e = stacktrace[1];
    final String methodName = e.getMethodName();
    System.out.println(methodName);
  }
}
