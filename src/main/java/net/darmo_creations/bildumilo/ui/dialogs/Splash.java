package net.darmo_creations.bildumilo.ui.dialogs;

import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import org.jetbrains.annotations.*;

/**
 * Undecorated dialog that displays a splash image.
 */
public class Splash extends DialogBase<Void> {
  public Splash(final @NotNull Config config) {
    super(config, "splash", false, ButtonTypes.CLOSE);
    this.stage().initStyle(StageStyle.UNDECORATED);
    final DialogPane dialogPane = this.getDialogPane();
    final Node node = dialogPane.lookupButton(ButtonTypes.CLOSE);
    node.setDisable(true);
    node.setVisible(false);
    final var stream = this.getClass().getResourceAsStream(App.IMAGES_PATH + "splash.png");
    if (stream != null) {
      final Image image = new Image(stream);
      final AnchorPane content = new AnchorPane(new ImageView(image));
      content.setPrefWidth(image.getWidth());
      content.prefHeight(image.getHeight());
      dialogPane.setContent(content);
    }
    dialogPane.getStyleClass().remove("dialog-pane"); // Remove default margins, etc.
    dialogPane.getChildren().removeIf(child -> child instanceof ButtonBar); // Remove button bar at the bottom
    // Consume all key events to prevent closing with Escape key (doesn’t disable Alt+F4)
    this.stage().addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
    this.setResultConverter(buttonType -> null);
  }
}
