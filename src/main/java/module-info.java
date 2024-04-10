module net.darmo_creations.imageslibrary {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.controlsfx.controls;
  requires java.desktop;

  requires org.jetbrains.annotations;

  requires org.xerial.sqlitejdbc;
  requires org.antlr.antlr4.runtime;
  requires logicng;
  requires org.slf4j;
  requires ini4j;
  requires org.apache.commons.cli;
  requires com.google.gson;

  opens net.darmo_creations.imageslibrary to javafx.fxml;
  exports net.darmo_creations.imageslibrary;
}