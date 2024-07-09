package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;

/**
 * Dialog to update the app’s settings. It is not resizable.
 */
public class SettingsDialog extends DialogBase<ButtonType> {
  private final ComboBox<Language> languageCombo = new ComboBox<>();
  private final ComboBox<Theme> themeCombo = new ComboBox<>();
  private final TextField dbFileField = new TextField();

  private Config initialConfig;
  private Config localConfig;

  /**
   * Create a settings dialog.
   *
   * @param config The app’s configuration.
   */
  public SettingsDialog(@NotNull Config config) {
    super(config, "settings", false, ButtonTypes.OK, ButtonTypes.CANCEL);

    final VBox content = new VBox(
        this.createInterfaceForm(),
        new Separator(),
        this.createDatabaseForm()
    );
    content.setPrefWidth(450);
    this.getDialogPane().setContent(content);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        final ChangeType changeType = this.configChanged();
        if (changeType.changed()) {
          try {
            this.localConfig.save();
            if (changeType.needsRestart())
              Alerts.info(config, "dialog.settings.alert.needs_restart.header", null, null);
          } catch (final IOException e) {
            App.logger().error("Exception caught while saving settings", e);
            Alerts.error(config, "dialog.settings.alert.save_error.header", null, null);
          }
        }
      }
      return buttonType;
    });
  }

  private Pane createInterfaceForm() {
    this.languageCombo.getItems().addAll(Config.languages());
    this.languageCombo.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> this.onLanguageSelect(newValue));
    this.themeCombo.getItems().addAll(Theme.themes());
    this.themeCombo.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> this.onThemeSelect(newValue));

    //noinspection unchecked
    return JavaFxUtils.newBorderPane(
        this.config,
        "dialog.settings.interface_box.title",
        new Pair<>("dialog.settings.interface_box.language.label", this.languageCombo),
        new Pair<>("dialog.settings.interface_box.theme.label", this.themeCombo)
    );
  }

  private Pane createDatabaseForm() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    HBox.setHgrow(this.dbFileField, Priority.ALWAYS);
    this.dbFileField.setEditable(false);
    final Button selectDbFileButton = new Button(null, theme.getIcon(Icon.OPEN_DB_FILE, Icon.Size.SMALL));
    selectDbFileButton.setOnAction(e -> this.onSelectDatabaseFile());
    selectDbFileButton.setTooltip(new Tooltip(
        language.translate("dialog.settings.database_box.db_file.select_button.tooltip")));
    final Button goToDbFileButton = new Button(null, theme.getIcon(Icon.OPEN_FILE_IN_EXPLORER, Icon.Size.SMALL));
    goToDbFileButton.setOnAction(e -> this.onGoToDatabaseFile());
    goToDbFileButton.setTooltip(new Tooltip(
        language.translate("dialog.settings.database_box.db_file.open_containing_directory_button.tooltip")));

    //noinspection unchecked
    return JavaFxUtils.newBorderPane(
        this.config,
        "dialog.settings.database_box.title",
        new Pair<>("dialog.settings.database_box.db_file.label",
            new HBox(5, this.dbFileField, selectDbFileButton, goToDbFileButton))
    );
  }

  private void onSelectDatabaseFile() {
    final var path = FileChoosers.showDatabaseFileChooser(this.config, this.stage(), null);
    if (path.isPresent()) {
      final Path file = path.get();
      this.dbFileField.setText(file.toString());
      this.onDatabaseFileSelect(file);
    }
  }

  private void onGoToDatabaseFile() {
    FileUtils.openInFileExplorer(this.dbFileField.getText());
  }

  /**
   * Reset the local {@link Config} object of this dialog.
   */
  public void resetLocalConfig() {
    this.localConfig = this.config.clone();
    this.initialConfig = this.localConfig.clone();

    this.languageCombo.getSelectionModel().select(this.localConfig.language());
    this.themeCombo.getSelectionModel().select(this.localConfig.theme());
    this.dbFileField.setText(this.localConfig.databaseFile().toString());

    this.updateState();
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  private void updateState() {
    final boolean configChanged = this.configChanged().changed();
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(!configChanged);
  }

  /**
   * Indicate whether the local config object has changed.
   *
   * @return The type of change.
   */
  private ChangeType configChanged() {
    if (!this.localConfig.language().equals(this.initialConfig.language())
        || !this.localConfig.theme().equals(this.initialConfig.theme())
        || !this.localConfig.databaseFile().equals(this.initialConfig.databaseFile()))
      return ChangeType.NEEDS_RESTART;
    return !this.localConfig.equals(this.initialConfig) ? ChangeType.NO_RESTART_NEEDED : ChangeType.NONE;
  }

  private void onLanguageSelect(@NotNull Language newValue) {
    this.localConfig = this.localConfig.withLanguage(newValue);
    this.updateState();
  }

  private void onThemeSelect(@NotNull Theme newValue) {
    this.localConfig = this.localConfig.withTheme(newValue);
    this.updateState();
  }

  private void onDatabaseFileSelect(@NotNull Path newValue) {
    this.localConfig = this.localConfig.withDatabaseFile(newValue);
    this.updateState();
  }

  /**
   * Enumeration of the differente types of config changes.
   */
  private enum ChangeType {
    /**
     * No changes.
     */
    NONE(false, false),
    /**
     * Some changes were made. No need to restart the app to apply them.
     */
    NO_RESTART_NEEDED(true, false),
    /**
     * Some changes were made. Some require to restart the app to apply them.
     */
    NEEDS_RESTART(true, true),
    ;

    private final boolean changed;
    private final boolean needsRestart;

    ChangeType(boolean changed, boolean needsRestart) {
      this.changed = changed;
      this.needsRestart = needsRestart;
    }

    /**
     * Indicate whether this change type indicates changes were made.
     */
    public boolean changed() {
      return this.changed;
    }

    /**
     * Indicate whether this change type requires the app to restart to apply fully.
     */
    public boolean needsRestart() {
      return this.needsRestart;
    }
  }
}
