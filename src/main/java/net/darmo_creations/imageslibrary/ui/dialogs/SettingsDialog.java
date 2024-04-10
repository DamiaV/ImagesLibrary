package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;
import javafx.util.converter.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.themes.*;

import java.io.*;
import java.nio.file.*;

/**
 * Dialog to update the app’s settings. It is not resizable.
 */
public class SettingsDialog extends DialogBase<ButtonType> {
  private final ComboBox<Language> languageCombo = new ComboBox<>();
  private final ComboBox<Theme> themeCombo = new ComboBox<>();
  private final TextField dbFileField = new TextField();
  private final Spinner<Integer> maxImagesShown = new Spinner<>(0, Integer.MAX_VALUE, Config.DEFAULT_MAX_IMAGES);
  private final CheckBox caseSensitiveByDefaultCheckBox = new CheckBox();

  private Config initialConfig;
  private Config localConfig;

  /**
   * Create a settings dialog.
   */
  public SettingsDialog() {
    super("settings", false, ButtonTypes.OK, ButtonTypes.CANCEL);

    final VBox content = new VBox(
        this.createInterfaceForm(),
        new Separator(),
        this.createQueriesForm(),
        new Separator(),
        this.createDatabaseForm()
    );
    content.setPrefWidth(550);
    this.getDialogPane().setContent(content);
    App.config().theme().getAppIcon().ifPresent(this::setIcon);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        final ChangeType changeType = this.configChanged();
        if (changeType.changed()) {
          try {
            App.updateConfig(this.localConfig);
            this.localConfig.save();
            if (changeType.needsRestart())
              Alerts.info("dialog.settings.alert.needs_restart.header", null, null);
          } catch (IOException e) {
            App.LOGGER.error("Exception caught while saving settings", e);
            Alerts.error("dialog.settings.alert.save_error.header", null, null);
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
    this.maxImagesShown.valueProperty()
        .addListener((observable, oldValue, newValue) -> this.onMaxImagesShownUpdate(newValue));
    this.maxImagesShown.setEditable(true);
    this.maxImagesShown.getValueFactory().setConverter(new PermissibleIntegerStringConverter());

    //noinspection unchecked
    return this.getBorderPane(
        "dialog.settings.interface_box.title",
        new Pair<>("dialog.settings.interface_box.language.label", this.languageCombo),
        new Pair<>("dialog.settings.interface_box.theme.label", this.themeCombo),
        new Pair<>("dialog.settings.interface_box.max_images_shown.label", this.maxImagesShown)
    );
  }

  private Pane createQueriesForm() {
    this.caseSensitiveByDefaultCheckBox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.onCaseSensitiveUpdate(newValue));

    //noinspection unchecked
    return this.getBorderPane(
        "dialog.settings.queries_box.title",
        new Pair<>("dialog.settings.queries_box.case_sensitive_by_default.label", this.caseSensitiveByDefaultCheckBox)
    );
  }

  private Pane createDatabaseForm() {
    final Config config = App.config();

    HBox.setHgrow(this.dbFileField, Priority.ALWAYS);
    this.dbFileField.setEditable(false);
    final Button selectDbFileButton = new Button(null, config.theme().getIcon(Icon.OPEN_DB_FILE, Icon.Size.SMALL));
    selectDbFileButton.setOnAction(e -> this.onSelectDatabaseFile());
    selectDbFileButton.setTooltip(new Tooltip(
        config.language().translate("dialog.settings.database_box.db_file.select_button.tooltip")));
    final Button goToDbFileButton = new Button(null, config.theme().getIcon(Icon.GO_TO_DB_FILE, Icon.Size.SMALL));
    goToDbFileButton.setOnAction(e -> this.onGoToDatabaseFile());
    goToDbFileButton.setTooltip(new Tooltip(
        config.language().translate("dialog.settings.database_box.db_file.open_containing_directory_button.tooltip")));

    //noinspection unchecked
    return this.getBorderPane(
        "dialog.settings.database_box.title",
        new Pair<>("dialog.settings.database_box.db_file.label",
            new HBox(5, this.dbFileField, selectDbFileButton, goToDbFileButton))
    );
  }

  private void onSelectDatabaseFile() {
    final var path = FileChoosers.showDatabaseFileChooser(this.stage(), null);
    if (path.isPresent()) {
      final Path file = path.get();
      this.dbFileField.setText(file.toString());
      this.onDatabaseFileSelect(file);
    }
  }

  private void onGoToDatabaseFile() {
    // Cannot use Desktop.getDesktop().open(File) as it does not work properly outside of Windows
    final String path = this.dbFileField.getText();
    // Possible values: https://runmodule.com/2020/10/12/possible-values-of-os-dependent-java-system-properties/
    final String osName = System.getProperty("os.name").toLowerCase();
    final String[] command;
    if (osName.contains("linux"))
      command = new String[] {"dbus-send", "--dest=org.freedesktop.FileManager1", "--type=method_call",
          "/org/freedesktop/FileManager1", "org.freedesktop.FileManager1.ShowItems",
          "array:string:file:%s".formatted(path), "string:\"\""};
    else if (osName.contains("win"))
      command = new String[] {"explorer /select,\"{path}\""};
    else if (osName.contains("mac"))
      command = new String[] {"open", "-R", path};
    else {
      App.LOGGER.error("Unable to open file system explorer: unsupported operating system {}", osName);
      return;
    }

    try {
      Runtime.getRuntime().exec(command);
    } catch (IOException e) {
      App.LOGGER.error("Unable to open file system explorer", e);
    }
  }

  @SuppressWarnings("unchecked")
  private BorderPane getBorderPane(String title, final Pair<String, ? extends Node>... rows) {
    final Label titleLabel = new Label(App.config().language().translate(title));
    BorderPane.setAlignment(titleLabel, Pos.CENTER);

    final GridPane gridPane = new GridPane();
    gridPane.setHgap(10);
    gridPane.setVgap(5);
    gridPane.setPadding(new Insets(10, 0, 10, 0));
    BorderPane.setAlignment(gridPane, Pos.CENTER);

    for (int i = 0; i < rows.length; i++) {
      final Label nodeLabel = new Label(App.config().language().translate(rows[i].getKey()));
      nodeLabel.setWrapText(true);
      GridPane.setHalignment(nodeLabel, HPos.RIGHT);
      final Node node = rows[i].getValue();
      GridPane.setHalignment(node, HPos.LEFT);
      gridPane.addRow(i, nodeLabel, node);
      final RowConstraints rc = new RowConstraints();
      rc.setVgrow(Priority.SOMETIMES);
      gridPane.getRowConstraints().add(rc);
    }

    final var cc1 = new ColumnConstraints();
    cc1.setMaxWidth(400);
    cc1.setHgrow(Priority.SOMETIMES);
    final var cc2 = new ColumnConstraints();
    cc2.setHgrow(Priority.SOMETIMES);
    gridPane.getColumnConstraints().addAll(cc1, cc2);

    return new BorderPane(gridPane, titleLabel, null, null, null);
  }

  /**
   * Reset the local {@link Config} object of this dialog.
   */
  public void resetLocalConfig() {
    this.localConfig = App.config().clone();
    this.initialConfig = this.localConfig.clone();

    this.languageCombo.getSelectionModel().select(this.localConfig.language());
    this.themeCombo.getSelectionModel().select(this.localConfig.theme());
    this.dbFileField.setText(this.localConfig.databaseFile().toString());
    this.maxImagesShown.getValueFactory().setValue(this.localConfig.maxImagesShown());
    this.caseSensitiveByDefaultCheckBox.setSelected(this.localConfig.caseSensitiveQueriesByDefault());

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

  private void onLanguageSelect(Language newValue) {
    this.localConfig = this.localConfig.withLanguage(newValue);
    this.updateState();
  }

  private void onThemeSelect(Theme newValue) {
    this.localConfig = this.localConfig.withTheme(newValue);
    this.updateState();
  }

  private void onDatabaseFileSelect(Path newValue) {
    this.localConfig = this.localConfig.withDatabaseFile(newValue);
    this.updateState();
  }

  private void onMaxImagesShownUpdate(int newValue) {
    this.localConfig.setMaxImagesShown(newValue);
    this.updateState();
  }

  private void onCaseSensitiveUpdate(boolean newValue) {
    this.localConfig.setCaseSensitiveQueriesByDefault(newValue);
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

  /**
   * Integer string converter that returns 0 instead of throwing an exception
   * when non-digit characters are fed to its {@link #fromString(String)} method.
   */
  private static class PermissibleIntegerStringConverter extends IntegerStringConverter {
    @Override
    public Integer fromString(String value) {
      try {
        return super.fromString(value);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
  }
}
