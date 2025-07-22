module net.darmo_creations.bildumilo {
  // JavaFX
  requires javafx.controls;
  requires javafx.media;
  requires java.desktop;

  // JavaFX libs
  requires org.controlsfx.controls;
  requires org.fxmisc.richtext;
  requires org.fxmisc.flowless;
  requires reactfx;

  // ImageIO
  requires javafx.swing;

  // Video frame extraction
  requires org.bytedeco.javacv;

  // Database
  requires org.xerial.sqlitejdbc;

  // Tag query parsing
  requires org.antlr.antlr4.runtime;
  requires logicng;

  // Logging
  requires org.slf4j;

  // Config and CLI
  requires ini4j;
  requires com.google.gson;
  requires org.apache.commons.cli;

  // Annotations
  requires org.jetbrains.annotations;
  requires org.bytedeco.opencv;

  exports net.darmo_creations.bildumilo;
  exports net.darmo_creations.bildumilo.ui;
  exports net.darmo_creations.bildumilo.ui.syntax_highlighting;
  exports net.darmo_creations.bildumilo.ui.dialogs;
}