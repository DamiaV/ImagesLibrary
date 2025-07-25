package net.darmo_creations.bildumilo.ui.dialogs;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class provides methods to open alert dialogs.
 */
public final class Alerts {
  /**
   * Open an error alert dialog to show the given database error code.
   *
   * @param config The app’s config.
   * @param code   The error code.
   */
  public static void databaseError(
      final @NotNull Config config,
      @NotNull DatabaseErrorCode code
  ) {
    databaseError(
        config,
        code,
        "alert.database_error.header",
        null
    );
  }

  /**
   * Open an error alert dialog to show the given database error code
   * with custom header, title and {@link FormatArg}s.
   *
   * @param config      The app’s config.
   * @param code        The error code.
   * @param headerKey   Header text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void databaseError(
      final @NotNull Config config,
      @NotNull DatabaseErrorCode code,
      String headerKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    final FormatArg[] formatArgs = new FormatArg[contentArgs.length + 1];
    formatArgs[0] = new FormatArg("code", code.name());
    System.arraycopy(contentArgs, 0, formatArgs, 1, contentArgs.length);
    error(
        config,
        headerKey,
        config.language().translate("error_code." + code.name().toLowerCase()),
        titleKey,
        formatArgs
    );
  }

  /**
   * Open an alert dialog to show some information.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void info(
      final @NotNull Config config,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    alert(config, AlertType.INFORMATION, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to show a warning message.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void warning(
      final @NotNull Config config,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    alert(config, AlertType.WARNING, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to show an error message.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   */
  public static void error(
      final @NotNull Config config,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    alert(config, AlertType.ERROR, headerKey, contentKey, titleKey, contentArgs);
  }

  /**
   * Open an alert dialog to prompt the user for confirmation.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   * @return True if the user clicked OK, false if they clicked CANCEL or dismissed the dialog.
   */
  public static boolean confirmation(
      final @NotNull Config config,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    final var result = alert(config, AlertType.CONFIRMATION, headerKey, contentKey, titleKey, contentArgs);
    return result.isPresent() && !result.get().getButtonData().isCancelButton();
  }

  /**
   * Open an alert dialog to prompt the user for confirmation.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param contentKey  Content text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header, content and title.
   * @return True if the user clicked YES, false if they clicked NO,
   * an empty {@link Optional} if they clicked CANCEL or dismissed the dialog.
   */
  public static Optional<Boolean> confirmationWithCancel(
      final @NotNull Config config,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    final var result = alert(config, AlertType.CONFIRMATION_CANCEL, headerKey, contentKey, titleKey, contentArgs);
    if (result.isEmpty() || result.get() == ButtonTypes.CANCEL)
      return Optional.empty();
    return Optional.of(!result.get().getButtonData().isCancelButton());
  }

  /**
   * Open an alert dialog to prompt the user for confirmation, featuring a checkbox.
   *
   * @param config      The current config.
   * @param headerKey   Header text key.
   * @param labelKey    Checkbox label text key.
   * @param titleKey    Title key.
   * @param checked     Whether the checkbox should be checked by default.
   * @param contentArgs Format arguments to apply to the header and title.
   * @return An {@link Optional} containing a boolean indicating whether the checkbox was checked;
   * an empty {@link Optional} if the dialog was cancelled.
   */
  public static Optional<Boolean> confirmCheckbox(
      final @NotNull Config config,
      @NotNull String headerKey,
      @NotNull String labelKey,
      String titleKey,
      boolean checked,
      final @NotNull FormatArg... contentArgs
  ) {
    final Alert alert = getAlert(config, AlertType.CONFIRMATION, headerKey, titleKey, contentArgs);
    final CheckBox checkBox = new CheckBox();
    checkBox.setSelected(checked);
    final var buttonType = buildAndShow(config, labelKey, alert, checkBox, contentArgs);
    if (buttonType.isPresent() && !buttonType.get().getButtonData().isCancelButton())
      return Optional.of(checkBox.isSelected());
    return Optional.empty();
  }

  /**
   * Open an alert dialog to prompt the user to input some text.
   *
   * @param config        The app’s config.
   * @param headerKey     Header text key.
   * @param labelKey      Text field label text key.
   * @param titleKey      Title key.
   * @param defaultText   Text to put into the text field.
   * @param textFormatter An optional text formatter to apply to the text field.
   * @param contentArgs   Format arguments to apply to the header, label and title.
   * @return The text typed by the user, stripped of any leading and trailing whitespace.
   */
  public static Optional<String> textInput(
      final @NotNull Config config,
      @NotNull String headerKey,
      @NotNull String labelKey,
      String titleKey,
      String defaultText,
      TextFormatter<?> textFormatter,
      final @NotNull FormatArg... contentArgs
  ) {
    final Alert alert = getAlert(config, AlertType.TEXT, headerKey, titleKey, contentArgs);
    final TextField textField = new TextField();
    textField.textProperty().addListener((observable, oldValue, newValue) ->
        alert.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(StringUtils.stripNullable(newValue).isEmpty()));
    if (textFormatter != null)
      textField.setTextFormatter(textFormatter);
    textField.setText(defaultText);
    final var buttonType = buildAndShow(config, labelKey, alert, textField, contentArgs);
    if (buttonType.isPresent() && !buttonType.get().getButtonData().isCancelButton())
      return StringUtils.stripNullable(textField.getText());
    return Optional.empty();
  }

  private static Optional<ButtonType> buildAndShow(
      final @NotNull Config config,
      @NotNull String labelKey,
      @NotNull Alert alert,
      @NotNull Control control,
      final @NotNull FormatArg... contentArgs
  ) {
    final HBox hBox = new HBox(4);
    final Label label = new Label(config.language().translate(labelKey, contentArgs));
    hBox.getChildren().addAll(label, control);
    hBox.setAlignment(Pos.CENTER);
    alert.getDialogPane().setContent(hBox);
    alert.setOnShown(e -> {
      Platform.runLater(control::requestFocus);
      e.consume();
    });
    return alert.showAndWait();
  }

  private static Optional<ButtonType> alert(
      final @NotNull Config config,
      @NotNull AlertType type,
      @NotNull String headerKey,
      String contentKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    final Alert alert = getAlert(config, type, headerKey, titleKey, contentArgs);
    if (contentKey != null)
      alert.setContentText(config.language().translate(contentKey, contentArgs));
    return alert.showAndWait();
  }

  /**
   * Create a basic alert dialog.
   *
   * @param config      The current config.
   * @param type        Alert type.
   * @param headerKey   Header text key.
   * @param titleKey    Title key.
   * @param contentArgs Format arguments to apply to the header and title.
   * @return The alert dialog.
   */
  private static Alert getAlert(
      final @NotNull Config config,
      @NotNull AlertType type,
      @NotNull String headerKey,
      String titleKey,
      final @NotNull FormatArg... contentArgs
  ) {
    final Alert alert = new Alert(type.type());
    final DialogPane dialogPane = alert.getDialogPane();
    // Replace default buttons to have proper translations
    dialogPane.getButtonTypes().setAll(switch (type) {
      case INFORMATION, WARNING, ERROR -> List.of(ButtonTypes.OK);
      case CONFIRMATION -> List.of(ButtonTypes.YES, ButtonTypes.NO);
      case CONFIRMATION_CANCEL -> List.of(ButtonTypes.YES, ButtonTypes.NO, ButtonTypes.CANCEL);
      case TEXT -> List.of(ButtonTypes.OK, ButtonTypes.CANCEL);
    });
    config.theme().applyTo(dialogPane);
    if (titleKey == null)
      titleKey = "alert.%s.title".formatted(type.key());
    final Language language = config.language();
    alert.setTitle(language.translate(titleKey, contentArgs) + " – " + App.NAME);
    alert.setHeaderText(language.translate(headerKey, contentArgs));
    config.theme().getAppIcon().ifPresent(
        icon -> ((Stage) dialogPane.getScene().getWindow()).getIcons().add(icon));
    return alert;
  }

  private Alerts() {
  }

  public enum AlertType {
    CONFIRMATION(Alert.AlertType.CONFIRMATION, "confirmation"),
    CONFIRMATION_CANCEL(Alert.AlertType.CONFIRMATION, "confirmation"),
    INFORMATION(Alert.AlertType.INFORMATION, "information"),
    ERROR(Alert.AlertType.ERROR, "error"),
    WARNING(Alert.AlertType.ERROR, "warning"),
    TEXT(Alert.AlertType.CONFIRMATION, "text"),
    ;

    private final Alert.AlertType type;
    private final String key;

    AlertType(@NotNull Alert.AlertType type, String key) {
      this.type = type;
      this.key = key;
    }

    public Alert.AlertType type() {
      return this.type;
    }

    public String key() {
      return this.key;
    }
  }
}
