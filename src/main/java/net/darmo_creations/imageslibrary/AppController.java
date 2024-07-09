package net.darmo_creations.imageslibrary;

import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AppController implements ResultsView.SearchListener {
  private final DatabaseConnection db;

  /**
   * The stage associated to this controller.
   */
  private final Stage stage;
  private final Config config;

  private final CreateTagTypeDialog createTagTypeDialog;
  private final EditTagTypeDialog editTagTypeDialog;
  private final EditTagDialog editTagDialog;
  private final SettingsDialog settingsDialog;
  private final AboutDialog aboutDialog;

  private final Map<MenuItem, Boolean> menuItemStates = new HashMap<>();
  private MenuItem renameImagesMenuItem;
  private Button renameImagesButton;
  private MenuItem moveImagesMenuItem;
  private Button moveImagesButton;
  private MenuItem editMenuItem;
  private Button editButton;
  private MenuItem deleteMenuItem;
  private Button deleteButton;
  private MenuItem slideshowMenuItem;
  private Button slideshowButton;
  private MenuItem slideshowSelectedMenuItem;
  private Button slideshowSelectedButton;

  private final TagsView tagsView;
  private final ResultsView resultsView;

  private final List<Picture> selectedPictures = new ArrayList<>();
  private final List<Tag> selectedTags = new ArrayList<>();

  /**
   * Create a controller for the given {@link Stage}.
   *
   * @param stage  The stage to associate this controller to.
   * @param config The app’s config.
   * @param db     The database.
   * @throws DatabaseOperationException If any database initialization error occurs.
   */
  public AppController(@NotNull Stage stage, @NotNull Config config, @NotNull DatabaseConnection db)
      throws DatabaseOperationException {
    this.stage = Objects.requireNonNull(stage);
    this.config = config;
    this.db = db;
    final Theme theme = config.theme();
    theme.getAppIcon().ifPresent(icon -> stage.getIcons().add(icon));
    stage.setMinWidth(300);
    stage.setMinHeight(200);
    stage.setTitle(App.NAME + (config.isDebug() ? " [DEBUG]" : ""));
    stage.setMaximized(true);

    this.createTagTypeDialog = new CreateTagTypeDialog(config, db);
    this.editTagTypeDialog = new EditTagTypeDialog(config, db);
    this.editTagDialog = new EditTagDialog(config, db);
    this.settingsDialog = new SettingsDialog(config);
    this.aboutDialog = new AboutDialog(config);

    this.tagsView = new TagsView(
        config,
        this.db.getAllTags(),
        this.db.getAllTagsCounts(),
        this.db.getAllTagTypes()
    );
    this.resultsView = new ResultsView(config, this.db);
    final Scene scene = new Scene(new VBox(this.createMenuBar(), this.createToolBar(), this.createContent()));
    stage.setScene(scene);
    theme.applyTo(scene);

    // Files/directories drag-and-drop
    scene.setOnDragOver(event -> {
      if (event.getGestureSource() == null // From another application
          && this.isDragAndDropValid(event.getDragboard())) {
        event.acceptTransferModes(TransferMode.COPY);
      }
      event.consume();
    });
    scene.setOnDragDropped(event -> {
      final Dragboard dragboard = event.getDragboard();
      final boolean success = this.isDragAndDropValid(dragboard);
      if (success)
        this.loadFiles(dragboard.getFiles());
      event.setDropCompleted(success);
      event.consume();
    });
  }

  private boolean isDragAndDropValid(final @NotNull Dragboard dragboard) {
    return dragboard.hasFiles() && dragboard.getFiles().stream().allMatch(
        // Accept directories and files with valid extensions
        file -> file.isDirectory() || App.VALID_IMAGE_EXTENSIONS.contains(FileUtils.getExtension(file.toPath()).toLowerCase()));
  }

  private MenuBar createMenuBar() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final Menu fileMenu = new Menu(language.translate("menu.file"));
    final MenuItem importImagesMenuItem = new MenuItem(
        language.translate("menu.file.import_images"),
        theme.getIcon(Icon.IMPORT_IMAGES, Icon.Size.SMALL)
    );
    importImagesMenuItem.setOnAction(e -> this.onImportImages());
    importImagesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(importImagesMenuItem, importImagesMenuItem.isDisable());
    final MenuItem importDirectoriesMenuItem = new MenuItem(
        language.translate("menu.file.import_directories"),
        theme.getIcon(Icon.IMPORT_DIRECTORIES, Icon.Size.SMALL)
    );
    importDirectoriesMenuItem.setOnAction(e -> this.onImportDirectories());
    importDirectoriesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(importDirectoriesMenuItem, importDirectoriesMenuItem.isDisable());
    final MenuItem settingsMenuItem = new MenuItem(
        language.translate("menu.file.settings"),
        theme.getIcon(Icon.SETTINGS, Icon.Size.SMALL)
    );
    settingsMenuItem.setOnAction(e -> this.onSettings());
    settingsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN));
    this.menuItemStates.put(settingsMenuItem, settingsMenuItem.isDisable());
    final MenuItem quitMenuItem = new MenuItem(
        language.translate("menu.file.quit"),
        theme.getIcon(Icon.QUIT, Icon.Size.SMALL)
    );
    quitMenuItem.setOnAction(e -> this.onQuit());
    quitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(quitMenuItem, quitMenuItem.isDisable());
    fileMenu.getItems().addAll(
        importImagesMenuItem,
        importDirectoriesMenuItem,
        new SeparatorMenuItem(),
        settingsMenuItem,
        new SeparatorMenuItem(),
        quitMenuItem
    );

    final Menu editMenu = new Menu(language.translate("menu.edit"));
    this.editMenuItem = new MenuItem(
        language.translate("menu.edit.edit_images"),
        theme.getIcon(Icon.EDIT_IMAGES, Icon.Size.SMALL)
    );
    this.editMenuItem.setOnAction(e -> this.onEdit());
    this.editMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
    this.editMenuItem.setDisable(true);
    this.menuItemStates.put(this.editMenuItem, this.editMenuItem.isDisable());
    this.deleteMenuItem = new MenuItem(
        language.translate("menu.edit.delete_images"),
        theme.getIcon(Icon.DELETE_IMAGES, Icon.Size.SMALL)
    );
    this.deleteMenuItem.setOnAction(e -> this.onDelete());
    this.deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
    this.deleteMenuItem.setDisable(true);
    this.menuItemStates.put(this.deleteMenuItem, this.deleteMenuItem.isDisable());
    this.renameImagesMenuItem = new MenuItem(
        language.translate("menu.edit.rename_images"),
        theme.getIcon(Icon.RENAME_IMAGES, Icon.Size.SMALL)
    );
    this.renameImagesMenuItem.setOnAction(e -> this.onRenameSelectedImages());
    this.renameImagesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
    this.renameImagesMenuItem.setDisable(true);
    this.menuItemStates.put(this.renameImagesMenuItem, this.renameImagesMenuItem.isDisable());
    this.moveImagesMenuItem = new MenuItem(
        language.translate("menu.edit.move_images"),
        theme.getIcon(Icon.MOVE_IMAGES, Icon.Size.SMALL)
    );
    this.moveImagesMenuItem.setOnAction(e -> this.onMoveSelectedImages());
    this.moveImagesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));
    this.moveImagesMenuItem.setDisable(true);
    this.menuItemStates.put(this.moveImagesMenuItem, this.moveImagesMenuItem.isDisable());
    editMenu.getItems().addAll(
        this.editMenuItem,
        this.deleteMenuItem,
        this.renameImagesMenuItem,
        this.moveImagesMenuItem
    );

    final Menu viewMenu = new Menu(language.translate("menu.view"));
    this.slideshowMenuItem = new MenuItem(
        language.translate("menu.view.slideshow"),
        theme.getIcon(Icon.SLIDESHOW, Icon.Size.SMALL)
    );
    this.slideshowMenuItem.setOnAction(e -> this.onSlideshow(false));
    this.slideshowMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
    this.slideshowMenuItem.setDisable(true);
    this.menuItemStates.put(this.slideshowMenuItem, this.slideshowMenuItem.isDisable());
    this.slideshowSelectedMenuItem = new MenuItem(
        language.translate("menu.view.slideshow_selected"),
        theme.getIcon(Icon.SLIDESHOW_SELECTED, Icon.Size.SMALL)
    );
    this.slideshowSelectedMenuItem.setOnAction(e -> this.onSlideshow(true));
    this.slideshowSelectedMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F11, KeyCombination.CONTROL_DOWN));
    this.slideshowSelectedMenuItem.setDisable(true);
    this.menuItemStates.put(this.slideshowSelectedMenuItem, this.slideshowSelectedMenuItem.isDisable());
    viewMenu.getItems().addAll(this.slideshowMenuItem, this.slideshowSelectedMenuItem);

    final Menu toolsMenu = new Menu(language.translate("menu.tools"));
    final MenuItem showNoTagsMenuItem = new MenuItem(
        language.translate("menu.tools.show_images_with_no_tags"),
        theme.getIcon(Icon.SEARCH_NO_TAGS, Icon.Size.SMALL)
    );
    showNoTagsMenuItem.setOnAction(e -> this.onShowImagesWithNoTags());
    showNoTagsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(showNoTagsMenuItem, showNoTagsMenuItem.isDisable());
    final MenuItem showNoFileMenuItem = new MenuItem(
        language.translate("menu.tools.show_images_with_no_file"),
        theme.getIcon(Icon.SEARCH_NO_FILE, Icon.Size.SMALL)
    );
    showNoFileMenuItem.setOnAction(e -> this.onShowImagesWithNoFile());
    showNoFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.menuItemStates.put(showNoFileMenuItem, showNoFileMenuItem.isDisable());
    final MenuItem convertPythonDbMenuItem = new MenuItem(
        language.translate("menu.tools.convert_python_db"),
        theme.getIcon(Icon.CONVERT_PYTHON_DB, Icon.Size.SMALL)
    );
    convertPythonDbMenuItem.setOnAction(e -> this.onConvertPythonDbMenuItem());
    final MenuItem focusSearchBarMenuItem = new MenuItem(
        language.translate("menu.tools.focus_search_bar"),
        theme.getIcon(Icon.FOCUS_SEARCH_BAR, Icon.Size.SMALL)
    );
    focusSearchBarMenuItem.setOnAction(e -> this.onFocusSearchBar());
    focusSearchBarMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(focusSearchBarMenuItem, focusSearchBarMenuItem.isDisable());
    this.menuItemStates.put(convertPythonDbMenuItem, convertPythonDbMenuItem.isDisable());
    toolsMenu.getItems().addAll(
        showNoTagsMenuItem,
        showNoFileMenuItem,
        new SeparatorMenuItem(),
        convertPythonDbMenuItem,
        new SeparatorMenuItem(),
        focusSearchBarMenuItem
    );

    final Menu helpMenu = new Menu(language.translate("menu.help"));
    final MenuItem aboutMenuItem = new MenuItem(
        language.translate("menu.help.about"),
        theme.getIcon(Icon.ABOUT, Icon.Size.SMALL)
    );
    aboutMenuItem.setOnAction(e -> this.onAbout());
    this.menuItemStates.put(aboutMenuItem, aboutMenuItem.isDisable());
    final MenuItem helpMenuItem = new MenuItem(
        language.translate("menu.help.help"),
        theme.getIcon(Icon.HELP, Icon.Size.SMALL)
    );
    helpMenuItem.setOnAction(e -> this.onHelp());
    helpMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(helpMenuItem, helpMenuItem.isDisable());
    helpMenu.getItems().addAll(aboutMenuItem, helpMenuItem);

    return new MenuBar(fileMenu, editMenu, viewMenu, toolsMenu, helpMenu);
  }

  private ToolBar createToolBar() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final Button importImagesButton = new Button(null, theme.getIcon(Icon.IMPORT_IMAGES, Icon.Size.BIG));
    importImagesButton.setOnAction(e -> this.onImportImages());
    importImagesButton.setTooltip(new Tooltip(language.translate("toolbar.file.import_images")));
    final Button importDirectoriesButton = new Button(null, theme.getIcon(Icon.IMPORT_DIRECTORIES, Icon.Size.BIG));
    importDirectoriesButton.setOnAction(e -> this.onImportDirectories());
    importDirectoriesButton.setTooltip(new Tooltip(language.translate("toolbar.file.import_directories")));

    this.editButton = new Button(null, theme.getIcon(Icon.EDIT_IMAGES, Icon.Size.BIG));
    this.editButton.setOnAction(e -> this.onEdit());
    this.editButton.setTooltip(new Tooltip(language.translate("toolbar.edit.edit_images")));
    this.editButton.setDisable(true);
    this.deleteButton = new Button(null, theme.getIcon(Icon.DELETE_IMAGES, Icon.Size.BIG));
    this.deleteButton.setOnAction(e -> this.onDelete());
    this.deleteButton.setTooltip(new Tooltip(language.translate("toolbar.edit.delete_images")));
    this.deleteButton.setDisable(true);
    this.renameImagesButton = new Button(null, theme.getIcon(Icon.RENAME_IMAGES, Icon.Size.BIG));
    this.renameImagesButton.setOnAction(e -> this.onRenameSelectedImages());
    this.renameImagesButton.setTooltip(new Tooltip(language.translate("toolbar.edit.rename_images")));
    this.renameImagesButton.setDisable(true);
    this.moveImagesButton = new Button(null, theme.getIcon(Icon.MOVE_IMAGES, Icon.Size.BIG));
    this.moveImagesButton.setOnAction(e -> this.onMoveSelectedImages());
    this.moveImagesButton.setTooltip(new Tooltip(language.translate("toolbar.edit.move_images")));
    this.moveImagesButton.setDisable(true);

    this.slideshowButton = new Button(null, theme.getIcon(Icon.SLIDESHOW, Icon.Size.BIG));
    this.slideshowButton.setOnAction(e -> this.onSlideshow(false));
    this.slideshowButton.setTooltip(new Tooltip(language.translate("toolbar.view.slideshow")));
    this.slideshowButton.setDisable(true);
    this.slideshowSelectedButton = new Button(null, theme.getIcon(Icon.SLIDESHOW_SELECTED, Icon.Size.BIG));
    this.slideshowSelectedButton.setOnAction(e -> this.onSlideshow(true));
    this.slideshowSelectedButton.setTooltip(new Tooltip(language.translate("toolbar.view.slideshow_selected")));
    this.slideshowSelectedButton.setDisable(true);

    final Button showNoTagsButton = new Button(null, theme.getIcon(Icon.SEARCH_NO_TAGS, Icon.Size.BIG));
    showNoTagsButton.setOnAction(e -> this.onShowImagesWithNoTags());
    showNoTagsButton.setTooltip(new Tooltip(language.translate("toolbar.tools.show_images_with_no_tags")));
    final Button showNoFileButton = new Button(null, theme.getIcon(Icon.SEARCH_NO_FILE, Icon.Size.BIG));
    showNoFileButton.setOnAction(e -> this.onShowImagesWithNoFile());
    showNoFileButton.setTooltip(new Tooltip(language.translate("toolbar.tools.show_images_with_no_file")));

    final Button helpButton = new Button(null, theme.getIcon(Icon.HELP, Icon.Size.BIG));
    helpButton.setOnAction(e -> this.onHelp());
    helpButton.setTooltip(new Tooltip(language.translate("toolbar.help.help")));

    return new ToolBar(
        importImagesButton,
        importDirectoriesButton,
        new Separator(),
        this.editButton,
        this.deleteButton,
        this.renameImagesButton,
        this.moveImagesButton,
        new Separator(),
        this.slideshowButton,
        this.slideshowSelectedButton,
        new Separator(),
        showNoTagsButton,
        showNoFileButton,
        new Separator(),
        helpButton
    );
  }

  private SplitPane createContent() {
    final SplitPane splitPane = new SplitPane();
    this.tagsView.addTagClickListener(this::onTagClick);
    this.tagsView.addTagSelectionListener(this::onTagSelectionChange);
    this.tagsView.addEditTagTypeListener(this::onEditTagType);
    this.tagsView.addDeleteTagTypeListener(this::onDeleteTagType);
    this.tagsView.addCreateTagTypeListener(this::onCreateTagType);
    splitPane.getItems().add(this.tagsView);
    this.resultsView.addImageClickListener(this::onImageClick);
    this.resultsView.addImageSelectionListener(this::onImageSelectionChange);
    this.resultsView.addSearchListener(this);
    splitPane.getItems().add(this.resultsView);
    splitPane.setDividerPositions(0.1);
    VBox.setVgrow(splitPane, Priority.ALWAYS);
    return splitPane;
  }

  /**
   * Show the stage.
   * <p>
   * Hooks callbacks to the stage’s close event and load the specified tree or the default one.
   */
  public void show() {
    this.stage.show();
    this.stage.setOnCloseRequest(event -> {
      event.consume();
      this.onQuit();
    });
  }

  private void loadFiles(final @NotNull List<File> filesOrDirs) {
    // TODO
  }

  private void onTagClick(Tag tag) {
    this.resultsView.searchTag(tag);
  }

  private void onTagSelectionChange(final @NotNull List<Tag> tags) {
    this.selectedPictures.clear();
    this.selectedTags.clear();
    this.selectedTags.addAll(tags);

    this.renameImagesMenuItem.setDisable(true);
    this.renameImagesButton.setDisable(true);
    this.moveImagesMenuItem.setDisable(true);
    this.moveImagesButton.setDisable(true);
    this.deleteMenuItem.setDisable(true);
    this.deleteButton.setDisable(true);
    this.slideshowSelectedMenuItem.setDisable(true);
    this.slideshowSelectedButton.setDisable(true);

    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final boolean empty = tags.isEmpty();
    final boolean nonSingleSelection = tags.size() != 1;
    this.editMenuItem.setDisable(nonSingleSelection);
    this.editMenuItem.setGraphic(theme.getIcon(Icon.EDIT_TAG, Icon.Size.SMALL));
    this.editMenuItem.setText(language.translate("menu.edit.edit_tag"));
    this.editButton.setDisable(nonSingleSelection);
    this.editButton.setGraphic(theme.getIcon(Icon.EDIT_TAG, Icon.Size.BIG));
    this.editButton.setTooltip(new Tooltip(language.translate("toolbar.edit.edit_tag")));
    this.deleteMenuItem.setDisable(empty);
    this.deleteMenuItem.setGraphic(theme.getIcon(Icon.DELETE_TAGS, Icon.Size.SMALL));
    this.deleteMenuItem.setText(language.translate("menu.edit.delete_tags"));
    this.deleteButton.setDisable(empty);
    this.deleteButton.setGraphic(theme.getIcon(Icon.DELETE_TAGS, Icon.Size.BIG));
    this.deleteButton.setTooltip(new Tooltip(language.translate("toolbar.edit.delete_tags")));
  }

  private void onEditTagType(TagType tagType) {
    this.editTagTypeDialog.setTagType(tagType);
    this.editTagTypeDialog.showAndWait().ifPresent(type -> {
      this.tagsView.refresh();
      this.tagsView.selectTagType(type);
    });
  }

  private void onDeleteTagType(TagType tagType) {
    final boolean proceed = Alerts.confirmation(
        this.config,
        "alert.delete_tag_type.header",
        null,
        null,
        new FormatArg("label", tagType.label())
    );
    if (!proceed)
      return;
    try {
      this.db.deleteTagTypes(Set.of(tagType));
    } catch (DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return;
    }
    this.tagsView.refresh();
  }

  private void onCreateTagType() {
    this.createTagTypeDialog.reset();
    this.createTagTypeDialog.showAndWait().ifPresent(type -> {
      this.tagsView.refresh();
      this.tagsView.selectTagType(type);
    });
  }

  private void onImageClick(Picture picture) {
    // TODO
    System.out.println("image clicked: " + picture.path());
  }

  private void onImageSelectionChange(final @NotNull List<Picture> pictures) {
    this.selectedTags.clear();
    this.selectedPictures.clear();
    this.selectedPictures.addAll(pictures);

    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final boolean empty = pictures.isEmpty();
    this.editMenuItem.setDisable(empty);
    this.editMenuItem.setGraphic(theme.getIcon(Icon.EDIT_IMAGES, Icon.Size.SMALL));
    this.editMenuItem.setText(language.translate("menu.edit.edit_images"));
    this.editButton.setDisable(empty);
    this.editButton.setGraphic(theme.getIcon(Icon.EDIT_IMAGES, Icon.Size.BIG));
    this.editButton.setTooltip(new Tooltip(language.translate("toolbar.edit.edit_images")));
    this.deleteMenuItem.setDisable(empty);
    this.deleteMenuItem.setGraphic(theme.getIcon(Icon.DELETE_IMAGES, Icon.Size.SMALL));
    this.deleteMenuItem.setText(language.translate("menu.edit.delete_images"));
    this.deleteButton.setDisable(empty);
    this.deleteButton.setGraphic(theme.getIcon(Icon.DELETE_IMAGES, Icon.Size.BIG));
    this.deleteButton.setTooltip(new Tooltip(language.translate("toolbar.edit.delete_images")));
    this.renameImagesMenuItem.setDisable(empty);
    this.renameImagesButton.setDisable(empty);
    this.moveImagesMenuItem.setDisable(empty);
    this.moveImagesButton.setDisable(empty);
    this.slideshowSelectedMenuItem.setDisable(empty);
    this.slideshowSelectedButton.setDisable(empty);
  }

  @Override
  public void onSearchStart() {
    this.disableInteractions();
  }

  @Override
  public void onSearchEnd(int resultsCount) {
    this.restoreInteractions();
    final boolean noResults = resultsCount == 0;
    this.slideshowMenuItem.setDisable(noResults);
    this.slideshowButton.setDisable(noResults);
    if (this.selectedTags.isEmpty()) {
      this.editMenuItem.setDisable(true);
      this.editButton.setDisable(true);
      this.deleteMenuItem.setDisable(true);
      this.deleteButton.setDisable(true);
    }
  }

  @Override
  public void onSearchFail() {
    this.restoreInteractions();
  }

  private void disableInteractions() {
    this.stage.getScene().getRoot().setDisable(true);
    // Record the disable-state of all menu items and disable them
    this.menuItemStates.replaceAll((menuItem, b) -> {
      final boolean disabled = menuItem.isDisable();
      menuItem.setDisable(true);
      return disabled;
    });
  }

  private void restoreInteractions() {
    this.stage.getScene().getRoot().setDisable(false);
    // Restore menu items’ states
    this.menuItemStates.forEach(MenuItem::setDisable);
  }

  private void onImportImages() {
    // TODO
    System.out.println("import images");
  }

  private void onImportDirectories() {
    // TODO
    System.out.println("import directories");
  }

  /**
   * Open settings dialog.
   */
  private void onSettings() {
    this.settingsDialog.resetLocalConfig();
    this.settingsDialog.showAndWait();
  }

  /**
   * Release all resources and close the app.
   */
  private void onQuit() {
    try {
      this.db.close();
    } catch (DatabaseOperationException e) {
      throw new RuntimeException(e);
    }
    Platform.exit();
  }

  /**
   * Open the dialog to edit selected tags/images.
   */
  private void onEdit() {
    if (!this.selectedPictures.isEmpty())
      this.editSelectedPictures();
    else if (this.selectedTags.size() == 1)
      this.editTag(this.selectedTags.get(0));
  }

  private void editSelectedPictures() {
    // TODO
    System.out.println("edit images");
  }

  private void editTag(Tag tag) {
    this.editTagDialog.setTag(tag);
    this.editTagDialog.showAndWait().ifPresent(t -> {
      this.tagsView.refresh();
      this.tagsView.selectTagType(t.type().orElse(null));
    });
  }

  /**
   * Open the dialog to confirm the deletion of selected tags/images
   * then delete them if the user confirms.
   */
  private void onDelete() {
    if (!this.selectedPictures.isEmpty())
      this.deleteSelectedPictures();
    else if (!this.selectedTags.isEmpty())
      this.deleteSelectedTags();
  }

  private void deleteSelectedPictures() {
    final var fromDisk = Alerts.confirmCheckbox(
        this.config,
        "alert.delete_images.header",
        "alert.delete_images.label",
        null,
        false,
        new FormatArg("count", this.selectedPictures.size())
    );
    if (fromDisk.isEmpty())
      return;

    final List<Picture> notDeleted = new LinkedList<>();
    for (final var picture : this.selectedPictures) {
      try {
        this.db.deletePicture(picture, fromDisk.get());
      } catch (DatabaseOperationException e) {
        notDeleted.add(picture);
      }
    }
    if (!notDeleted.isEmpty()) {
      Alerts.error(this.config, "alert.deletion_error.header", null, null);
      this.resultsView.listImages(notDeleted);
    } else
      this.resultsView.refresh();
  }

  private void deleteSelectedTags() {
    // TODO
    System.out.println("delete tags");
  }

  /**
   * Open the dialog to rename selected images.
   */
  private void onRenameSelectedImages() {
    // TODO
    System.out.println("rename images");
  }

  /**
   * Open the dialog to move selected images.
   */
  private void onMoveSelectedImages() {
    // TODO
    System.out.println("move images");
  }

  /**
   * Open the slideshow dialog for the current query results.
   */
  private void onSlideshow(boolean onlySelected) {
    // TODO
    System.out.println("slideshow " + onlySelected);
  }

  /**
   * Launch a search for images with no tags.
   */
  private void onShowImagesWithNoTags() {
    this.resultsView.searchImagesWithNoTags();
  }

  /**
   * Launch a search for images with no file.
   */
  private void onShowImagesWithNoFile() {
    this.resultsView.searchImagesWithNoFile();
  }

  /**
   * Open the dialog to convert a Python database file.
   */
  private void onConvertPythonDbMenuItem() {
    final var path = FileChoosers.showDatabaseFileChooser(this.config, this.stage, null);
    if (path.isEmpty())
      return;
    this.disableInteractions();
    new Thread(() -> {
      final Path newPath;
      try {
        newPath = DatabaseConnection.convertPythonDatabase(path.get());
      } catch (DatabaseOperationException e) {
        Platform.runLater(() -> {
          App.logger().error("Unable to convert database file", e);
          this.restoreInteractions();
          Alerts.databaseError(this.config, e.errorCode());
        });
        return;
      }
      Platform.runLater(() -> {
        this.restoreInteractions();
        final boolean proceed = Alerts.confirmation(
            this.config,
            "alert.conversion_done.header",
            "alert.conversion_done.content",
            null,
            new FormatArg("path", newPath)
        );
        if (proceed) {
          try {
            this.config.withDatabaseFile(newPath).save();
          } catch (IOException e) {
            App.logger().error("Unable to save config", e);
            Alerts.error(this.config, "dialog.settings.alert.save_error.header", null, null);
          }
        }
      });
    }).start();
  }

  /**
   * Focus the search bar in ResultView.
   */
  private void onFocusSearchBar() {
    this.resultsView.focusSearchBar();
  }

  /**
   * Open help dialog.
   */
  private void onHelp() {
    // TODO
    System.out.println("help");
  }

  /**
   * Open about dialog.
   */
  private void onAbout() {
    this.aboutDialog.showAndWait();
  }
}
