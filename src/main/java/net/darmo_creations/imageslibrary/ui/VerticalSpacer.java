package net.darmo_creations.imageslibrary.ui;

import javafx.scene.layout.*;

/**
 * An empty panel that streches vertically.
 */
public class VerticalSpacer extends Pane {
  public VerticalSpacer() {
    VBox.setVgrow(this, Priority.ALWAYS);
  }
}
