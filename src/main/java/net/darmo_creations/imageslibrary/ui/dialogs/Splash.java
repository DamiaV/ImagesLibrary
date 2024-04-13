package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;

/**
 * Undecorated dialog that displays a splash image.
 */
public class Splash extends DialogBase<ButtonType> {
  public Splash(final Config config) {
    super("splash", false, config, ButtonTypes.OK);
    this.stage().initStyle(StageStyle.UNDECORATED);
    final DialogPane dialogPane = this.getDialogPane();
    final Node node = dialogPane.lookupButton(ButtonTypes.OK);
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
  }
}
