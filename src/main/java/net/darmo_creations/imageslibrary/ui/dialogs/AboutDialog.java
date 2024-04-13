package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.utils.*;

import java.util.*;

/**
 * Dialog that displays information about this app. It is not resizable.
 */
public class AboutDialog extends DialogBase<ButtonType> {
  /**
   * Create an about dialog.
   *
   * @param config The app’s configuration.
   */
  public AboutDialog(final Config config) {
    super("about", false, config, ButtonTypes.CLOSE);
    final Label titleLabel = new Label();
    titleLabel.setText(App.NAME);
    titleLabel.setStyle("-fx-font-size: 1.2em; -fx-font-weight: bold");
    VBox.setMargin(titleLabel, new Insets(10));

    final TextArea contentView = new TextArea();
    contentView.setText("""
        App version: %s
                
        Developped by Damia Vergnet (@Darmo117 on GitHub).
        This application is available under GPL-3.0 license.
                
        System properties:
        %s
        """.formatted(App.VERSION, App.getSystemProperties()));
    contentView.setEditable(false);
    VBox.setVgrow(contentView, Priority.ALWAYS);

    final VBox vBox = new VBox(10, titleLabel, contentView);

    final var appIcon = config.theme().getAppIcon();
    final ImageView logo = new ImageView(appIcon.orElse(null));
    logo.setFitHeight(100);
    logo.setFitWidth(100);

    final HBox content = new HBox(10, logo, vBox);
    content.setPrefWidth(600);
    content.setPrefHeight(300);
    this.getDialogPane().setContent(content);

    appIcon.ifPresent(this::setIcon);
  }

  @Override
  protected List<FormatArg> getTitleFormatArgs() {
    return List.of(new FormatArg("app_name", App.NAME));
  }
}
