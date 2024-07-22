package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Dialog that displays information about this app. It is not resizable.
 */
public class AboutDialog extends DialogBase<Void> {
  /**
   * Create an about dialog.
   *
   * @param config The app’s configuration.
   */
  public AboutDialog(final @NotNull Config config) {
    super(config, "about", true, ButtonTypes.CLOSE);
    final Label titleLabel = new Label();
    titleLabel.setText(App.NAME);
    titleLabel.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold");

    final Label systemPropsLabel = new Label(config.language().translate("dialog.about.system_properties"));

    final Pane spacer = new Pane();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    final Button button = new Button(null, config.theme().getIcon(Icon.COPY_TO_CLIPBOARD, Icon.Size.SMALL));
    button.setTooltip(new Tooltip(config.language().translate("dialog.about.copy_specs_button.tooltip")));
    button.setOnAction(event -> {
      final var clipboardContent = new ClipboardContent();
      clipboardContent.putString(App.getSystemProperties());
      Clipboard.getSystemClipboard().setContent(clipboardContent);
    });

    final HBox buttonBox = new HBox(5, systemPropsLabel, spacer, button);
    buttonBox.setAlignment(Pos.CENTER_LEFT);

    final TextArea systemPropsTextArea = new TextArea(App.getSystemProperties());
    systemPropsTextArea.setEditable(false);
    VBox.setVgrow(systemPropsTextArea, Priority.ALWAYS);

    final VBox vBox = new VBox(5, titleLabel, getTextArea(), buttonBox, systemPropsTextArea);
    HBox.setHgrow(vBox, Priority.ALWAYS);

    final ImageView logo = new ImageView(config.theme().getAppIcon().orElse(null));
    logo.setFitHeight(32);
    logo.setFitWidth(32);

    final HBox content = new HBox(10, logo, vBox);
    content.setPrefWidth(600);
    content.setPrefHeight(600);
    this.getDialogPane().setContent(content);

    final Stage stage = this.stage();
    stage.setMinHeight(400);
    stage.setMinWidth(600);

    this.setResultConverter(buttonType -> null);
  }

  private static Node getTextArea() {
    final TextFlow textFlow = new TextFlow();
    final Text version = new Text(App.VERSION);
    version.setStyle("-fx-font-weight: bold");
    textFlow.getChildren().addAll(
        new Text("Version: "),
        version,
        new Text("\n\nDevelopped by Damia Vergnet ("),
        createLink("@Darmo117", "https://github.com/Darmo117"),
        new Text(") on GitHub).\nThis application is available under "),
        createLink("GPL-3.0 license", "https://github.com/Darmo117/ImagesLibrary/blob/master/LICENSE"),
        new Text(".\nCheck for updates at "),
        createLink("https://github.com/Darmo117/ImagesLibrary", "https://github.com/Darmo117/ImagesLibrary"),
        new Text(".\n\nIcons from "),
        createLink("FatCow", "https://github.com/gammasoft/fatcow"),
        new Text(".")
    );
    return textFlow;
  }

  private static Text createLink(@NotNull String text, @NotNull String url) {
    final Text link = new Text(text);
    link.getStyleClass().add("hyperlink"); // Add built-in JavaFX CSS class to format link
    link.setOnMouseClicked(event -> App.openURL(url));
    return link;
  }

  @Override
  protected List<FormatArg> getTitleFormatArgs() {
    return List.of(new FormatArg("app_name", App.NAME));
  }
}
