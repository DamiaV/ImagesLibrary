package net.darmo_creations.bildumilo.ui;

import javafx.scene.layout.*;

/**
 * An empty panel that streches horizontaly.
 */
public class HorizontalSpacer extends Pane {
  public HorizontalSpacer() {
    HBox.setHgrow(this, Priority.ALWAYS);
  }
}
