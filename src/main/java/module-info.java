module net.darmo_creations.imageslibrary {
  requires javafx.controls;
  requires javafx.fxml;
  requires org.controlsfx.controls;
  requires java.desktop;

  requires org.jetbrains.annotations;

  requires org.xerial.sqlitejdbc;
  requires org.antlr.antlr4.runtime;
  requires logicng;
  requires org.reflections;

  opens net.darmo_creations.imageslibrary to javafx.fxml;
  exports net.darmo_creations.imageslibrary;
}