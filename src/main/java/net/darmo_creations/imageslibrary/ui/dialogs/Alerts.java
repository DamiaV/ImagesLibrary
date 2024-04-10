package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class provides methods to open alert dialogs.
 */
public final class Alerts {
  /**
   * Open an error alert dialog to show the given database error code.
   *
   * @param code The error code.
   */
  public static void databaseError(DatabaseErrorCode code) {
    error(
        "alert.database_error.header",
        "alert.database_error.content",
        null,
        new FormatArg("error", code) // TODO translate error code
    );
  }

  /**
   * Open an alert dialog to show some information.
   *
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void info(
      String headerKey,
      @Nullable String contentKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    alert(Alert.AlertType.INFORMATION, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to show a warning message.
   *
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void warning(
      String headerKey,
      @Nullable String contentKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    alert(Alert.AlertType.WARNING, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to show an error message.
   *
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void error(
      String headerKey,
      @Nullable String contentKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    alert(Alert.AlertType.ERROR, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to prompt the user for confirmation.
   *
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   * @return True if the user clicked OK, false if they clicked CANCEL or dismissed the dialog.
   */
  public static boolean confirmation(
      String headerKey,
      @Nullable String contentKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    final var result = alert(Alert.AlertType.CONFIRMATION, headerKey, contentKey, titleKey, contentArgs);
    return result.isPresent() && !result.get().getButtonData().isCancelButton();
  }

  /**
   * Open an alert dialog to ask the user to choose an option from a combobox.
   *
   * @param headerKey   Header text key.
   * @param labelKey    Combobox label text key.
   * @param titleKey    Title key.
   * @param choices     The choices for the combobox.
   * @param contentArgs Format arguments to apply to the header and title.
   * @return The selected item.
   */
  public static <T> Optional<T> chooser(
      String headerKey,
      String labelKey,
      @Nullable String titleKey,
      final Collection<T> choices,
      final FormatArg... contentArgs
  ) {
    if (choices.isEmpty())
      throw new IllegalArgumentException("empty choices");
    final Alert alert = getAlert(Alert.AlertType.CONFIRMATION, headerKey, titleKey, contentArgs);
    final ComboBox<T> choicesCombo = new ComboBox<>();
    choicesCombo.getItems().addAll(choices);
    choicesCombo.getSelectionModel().select(0);
    final var buttonType = buildAndShow(labelKey, alert, choicesCombo, contentArgs);
    if (buttonType.isPresent() && !buttonType.get().getButtonData().isCancelButton())
      return Optional.of(choicesCombo.getSelectionModel().getSelectedItem());
    return Optional.empty();
  }

  /**
   * Open an alert dialog to prompt the use to input some text.
   *
   * @param headerKey   Header text key.
   * @param labelKey    Text field label text key.
   * @param titleKey    Title key.
   * @param defaultText Text to put into the text field.
   * @param contentArgs Format arguments to apply to the header, label and title.
   * @return The selected item.
   */
  public static Optional<String> textInput(
      String headerKey,
      String labelKey,
      @Nullable String titleKey,
      @Nullable String defaultText,
      final FormatArg... contentArgs
  ) {
    final Alert alert = getAlert(Alert.AlertType.CONFIRMATION, headerKey, titleKey, contentArgs);
    final TextField textField = new TextField();
    textField.textProperty().addListener((observable, oldValue, newValue) ->
        alert.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(StringUtils.stripNullable(newValue).isEmpty()));
    textField.setText(defaultText);
    final var buttonType = buildAndShow(labelKey, alert, textField, contentArgs);
    if (buttonType.isPresent() && !buttonType.get().getButtonData().isCancelButton())
      return StringUtils.stripNullable(textField.getText());
    return Optional.empty();
  }

  private static Optional<ButtonType> buildAndShow(
      String labelKey,
      Alert alert,
      Control choicesCombo,
      final FormatArg... contentArgs
  ) {
    final HBox hBox = new HBox(4);
    final Label label = new Label(App.config().language().translate(labelKey, contentArgs));
    hBox.getChildren().addAll(label, choicesCombo);
    hBox.setAlignment(Pos.CENTER);
    alert.getDialogPane().setContent(hBox);
    alert.setOnShown(e -> {
      Platform.runLater(choicesCombo::requestFocus);
      e.consume();
    });
    return alert.showAndWait();
  }

  private static Optional<ButtonType> alert(
      Alert.AlertType type,
      String headerKey,
      @Nullable String contentKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    final Alert alert = getAlert(type, headerKey, titleKey, contentArgs);
    if (contentKey != null)
      alert.setContentText(App.config().language().translate(contentKey, contentArgs));
    return alert.showAndWait();
  }

  /**
   * Create a basic alert dialog.
   *
   * @param type        Alert type. {@link Alert.AlertType#NONE} is not allowed.
   * @param headerKey   Header text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header and title.
   * @return The alert dialog.
   */
  private static Alert getAlert(
      Alert.AlertType type,
      String headerKey,
      @Nullable String titleKey,
      final FormatArg... contentArgs
  ) {
    if (type == Alert.AlertType.NONE)
      throw new IllegalArgumentException(type.name());
    final Alert alert = new Alert(type);
    final DialogPane dialogPane = alert.getDialogPane();
    // Replace default buttons to have proper translations
    dialogPane.getButtonTypes().setAll(switch (type) {
      case INFORMATION, WARNING, ERROR -> List.of(ButtonTypes.OK);
      case CONFIRMATION -> List.of(ButtonTypes.OK, ButtonTypes.CANCEL);
      case NONE -> throw new IllegalArgumentException(type.name()); // Should never happen
    });
    final Config config = App.config();
    config.theme().getStyleSheets()
        .forEach(url -> dialogPane.getStylesheets().add(url.toExternalForm()));
    if (titleKey == null)
      titleKey = "alert.%s.title".formatted(type.name().toLowerCase());
    final Language language = config.language();
    alert.setTitle(language.translate(titleKey));
    alert.setHeaderText(language.translate(headerKey, contentArgs));
    config.theme().getAppIcon().ifPresent(
        icon -> ((Stage) dialogPane.getScene().getWindow()).getIcons().add(icon));
    return alert;
  }

  private Alerts() {
  }
}
