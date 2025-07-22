package net.darmo_creations.bildumilo;

import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.ui.*;
import net.darmo_creations.bildumilo.ui.dialogs.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static javafx.scene.control.TabPane.*;

public class AppController implements ResultsView.SearchListener {
  private final DatabaseConnection db;

  /**
   * The stage associated to this controller.
   */
  private final Stage stage;
  private final Config config;
  private final SavedQueriesManager queriesManager;

  private final EditMediasDialog editMediasDialog;
  private final CreateTagDialog createTagDialog;
  private final CreateTagTypeDialog createTagTypeDialog;
  private final EditTagTypeDialog editTagTypeDialog;
  private final EditTagDialog editTagDialog;
  private final SettingsDialog settingsDialog;
  private final AboutDialog aboutDialog;
  private final ProgressDialog progressDialog;
  private final MoveMediaFilesDialog moveMediaFilesDialog;
  private final ImageViewerDialog imageViewerDialog;
  private final ManageSavedQueriesDialog manageSavedQueriesDialog;
  private final BatchOperationsDialog batchOperationsDialog;
  private final MergeMediaTagsDialog mergeMediaTagsDialog;

  private final Map<MenuItem, Boolean> menuItemStates = new HashMap<>();
  private MenuItem moveMediaFilesMenuItem;
  private Button moveMediaFilesButton;
  private MenuItem editMenuItem;
  private Button editButton;
  private MenuItem deleteMenuItem;
  private Button deleteButton;
  private MenuItem mergeMediaFilesTagsMenuItem;
  private Button mergeMediaFilesTagsButton;
  private MenuItem slideshowMenuItem;
  private Button slideshowButton;
  private MenuItem slideshowSelectedMenuItem;
  private Button slideshowSelectedButton;
  private Menu savedQueriesMenu;
  private MenuItem closeResultsTabMenuItem;

  private final TagsView tagsView;
  private final TabPane resultsTabPane = new TabPane();

  private final List<MediaFile> selectedMediaFiles = new ArrayList<>();
  private final List<Tag> selectedTags = new ArrayList<>();
  // Counts how many blocking tasks are ongoing to properly handle disabling/re-enabling of interactions
  private int onGoingTasksCount = 0;

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
    this.queriesManager = SavedQueriesManager.load(db);
    this.queriesManager.addQueriesUpdateListener(this::updateSavedQueries);

    final Theme theme = config.theme();
    theme.getAppIcon().ifPresent(icon -> stage.getIcons().add(icon));
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    stage.setTitle(App.NAME + (config.isDebug() ? " [DEBUG]" : ""));
    stage.setMaximized(true);

    this.editMediasDialog = new EditMediasDialog(config, db);
    this.createTagDialog = new CreateTagDialog(config, db);
    this.createTagTypeDialog = new CreateTagTypeDialog(config, db);
    this.editTagTypeDialog = new EditTagTypeDialog(config, db);
    this.editTagDialog = new EditTagDialog(config, db);
    this.settingsDialog = new SettingsDialog(config);
    this.aboutDialog = new AboutDialog(config);
    this.progressDialog = new ProgressDialog(config, "converting_python_db");
    this.moveMediaFilesDialog = new MoveMediaFilesDialog(config, db);
    this.imageViewerDialog = new ImageViewerDialog(config);
    this.manageSavedQueriesDialog = new ManageSavedQueriesDialog(config, this.queriesManager);
    this.batchOperationsDialog = new BatchOperationsDialog(config, db, BatchOperationsManager.load(db));
    this.mergeMediaTagsDialog = new MergeMediaTagsDialog(config, db);

    this.tagsView = new TagsView(config, this.db);

    final Scene scene = new Scene(new VBox(this.createMenuBar(), this.createToolBar(), this.createContent()));
    stage.setScene(scene);
    theme.applyTo(scene);

    // Files/directories drag-and-drop
    scene.setOnDragOver(event -> {
      if (event.getGestureSource() == null // From another application
          && this.isDragAndDropValid(event.getDragboard()))
        event.acceptTransferModes(TransferMode.COPY);
      event.consume();
    });
    scene.setOnDragDropped(event -> {
      final Dragboard dragboard = event.getDragboard();
      final boolean success = this.isDragAndDropValid(dragboard);
      if (success)
        this.loadFiles(dragboard.getFiles().stream().map(File::toPath).toList());
      event.setDropCompleted(success);
      event.consume();
    });

    stage.setOnCloseRequest(event -> {
      JavaFxUtils.checkNoOngoingTask(config, event, this.progressDialog);
      if (!event.isConsumed())
        try {
          this.db.close();
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(config, e.errorCode());
        }
    });

    this.updateSavedQueries();
  }

  private boolean isDragAndDropValid(final @NotNull Dragboard dragboard) {
    return dragboard.hasFiles() && dragboard.getFiles().stream().allMatch(
        // Accept directories and files with valid extensions
        file -> file.isDirectory() || FileUtils.isValidFile(file.toPath()));
  }

  private MenuBar createMenuBar() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final Menu fileMenu = new Menu(language.translate("menu.file"));
    final MenuItem importFilesMenuItem = new MenuItem(
        language.translate("menu.file.import_images"),
        theme.getIcon(Icon.IMPORT_MEDIA_FILES, Icon.Size.SMALL)
    );
    importFilesMenuItem.setOnAction(e -> this.onImportMediaFiles());
    importFilesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(importFilesMenuItem, importFilesMenuItem.isDisable());
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
    quitMenuItem.setOnAction(e -> this.stage.close());
    quitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(quitMenuItem, quitMenuItem.isDisable());
    fileMenu.getItems().addAll(
        importFilesMenuItem,
        importDirectoriesMenuItem,
        new SeparatorMenuItem(),
        settingsMenuItem,
        new SeparatorMenuItem(),
        quitMenuItem
    );

    final Menu editMenu = new Menu(language.translate("menu.edit"));
    this.editMenuItem = new MenuItem(
        language.translate("menu.edit.edit_images"),
        theme.getIcon(Icon.EDIT_MEDIA_FILES, Icon.Size.SMALL)
    );
    this.editMenuItem.setOnAction(e -> this.onEdit());
    this.editMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN));
    this.editMenuItem.setDisable(true);
    this.menuItemStates.put(this.editMenuItem, this.editMenuItem.isDisable());
    this.deleteMenuItem = new MenuItem(
        language.translate("menu.edit.delete_images"),
        theme.getIcon(Icon.DELETE_MEDIA_FILES, Icon.Size.SMALL)
    );
    this.deleteMenuItem.setOnAction(e -> this.onDelete());
    this.deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
    this.deleteMenuItem.setDisable(true);
    this.menuItemStates.put(this.deleteMenuItem, this.deleteMenuItem.isDisable());
    this.moveMediaFilesMenuItem = new MenuItem(
        language.translate("menu.edit.move_images"),
        theme.getIcon(Icon.MOVE_MEDIAS, Icon.Size.SMALL)
    );
    this.moveMediaFilesMenuItem.setOnAction(e -> this.onBatchMoveSelectedMediaFiles());
    this.moveMediaFilesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));
    this.moveMediaFilesMenuItem.setDisable(true);
    this.menuItemStates.put(this.moveMediaFilesMenuItem, this.moveMediaFilesMenuItem.isDisable());
    this.mergeMediaFilesTagsMenuItem = new MenuItem(
        language.translate("menu.edit.merge_images_tags"),
        theme.getIcon(Icon.MERGE_MEDIA_FILES_TAGS, Icon.Size.SMALL)
    );
    this.mergeMediaFilesTagsMenuItem.setOnAction(e -> this.onMergeSelectedMediaFilesTags());
    this.mergeMediaFilesTagsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.mergeMediaFilesTagsMenuItem.setDisable(true);
    this.menuItemStates.put(this.mergeMediaFilesTagsMenuItem, this.mergeMediaFilesTagsMenuItem.isDisable());
    editMenu.getItems().addAll(
        this.editMenuItem,
        this.deleteMenuItem,
        this.moveMediaFilesMenuItem,
        this.mergeMediaFilesTagsMenuItem
    );

    final Menu viewMenu = new Menu(language.translate("menu.view"));
    final MenuItem addResultsTabMenuItem = new MenuItem(
        language.translate("menu.view.new_results_tab"),
        theme.getIcon(Icon.NEW_TAB, Icon.Size.SMALL)
    );
    addResultsTabMenuItem.setOnAction(e -> this.addResultsTab(null));
    addResultsTabMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
    this.closeResultsTabMenuItem = new MenuItem(
        language.translate("menu.view.close_results_tab"),
        theme.getIcon(Icon.CLOSE_TAB, Icon.Size.SMALL)
    );
    this.closeResultsTabMenuItem.setOnAction(e -> this.onCloseResultsTab());
    this.closeResultsTabMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
    this.closeResultsTabMenuItem.setDisable(true);
    this.menuItemStates.put(this.closeResultsTabMenuItem, this.closeResultsTabMenuItem.isDisable());
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
    viewMenu.getItems().addAll(
        addResultsTabMenuItem,
        this.closeResultsTabMenuItem,
        new SeparatorMenuItem(),
        this.slideshowMenuItem,
        this.slideshowSelectedMenuItem
    );

    final Menu queriesMenu = new Menu(language.translate("menu.queries"));
    final MenuItem showNoTagsMenuItem = new MenuItem(
        language.translate("menu.queries.show_images_with_no_tags"),
        theme.getIcon(Icon.SEARCH_NO_TAGS, Icon.Size.SMALL)
    );
    showNoTagsMenuItem.setOnAction(e -> this.onShowMediasWithNoTags());
    showNoTagsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.menuItemStates.put(showNoTagsMenuItem, showNoTagsMenuItem.isDisable());
    final MenuItem showNoFileMenuItem = new MenuItem(
        language.translate("menu.queries.show_images_with_no_file"),
        theme.getIcon(Icon.SEARCH_NO_FILE, Icon.Size.SMALL)
    );
    showNoFileMenuItem.setOnAction(e -> this.onShowMediasWithNoFile());
    showNoFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.menuItemStates.put(showNoFileMenuItem, showNoFileMenuItem.isDisable());
    final MenuItem showNoHashMenuItem = new MenuItem(
        language.translate("menu.queries.show_images_with_no_hash"),
        theme.getIcon(Icon.SEARCH_NO_HASH, Icon.Size.SMALL)
    );
    showNoHashMenuItem.setOnAction(e -> this.onShowMediasWithNoHash());
    showNoHashMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.menuItemStates.put(showNoHashMenuItem, showNoHashMenuItem.isDisable());
    final MenuItem showVideosMenuItem = new MenuItem(
        language.translate("menu.queries.show_videos"),
        theme.getIcon(Icon.SEARCH_VIDEOS, Icon.Size.SMALL)
    );
    showVideosMenuItem.setOnAction(e -> this.onShowVideos());
    showVideosMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
    this.menuItemStates.put(showVideosMenuItem, showVideosMenuItem.isDisable());
    this.savedQueriesMenu = new Menu(
        language.translate("menu.queries.saved_queries"),
        theme.getIcon(Icon.SAVED_QUERIES, Icon.Size.SMALL)
    );
    final MenuItem manageSavedQueriesMenuItem = new MenuItem(
        language.translate("menu.queries.saved_queries.manage"),
        theme.getIcon(Icon.MANAGE_SAVED_QUERIES, Icon.Size.SMALL)
    );
    manageSavedQueriesMenuItem.setOnAction(event -> this.manageSavedQueriesDialog.showAndWait());
    this.savedQueriesMenu.getItems().addAll(new SeparatorMenuItem(), manageSavedQueriesMenuItem);
    final MenuItem focusSearchBarMenuItem = new MenuItem(
        language.translate("menu.queries.focus_search_bar"),
        theme.getIcon(Icon.FOCUS_SEARCH_BAR, Icon.Size.SMALL)
    );
    focusSearchBarMenuItem.setOnAction(e -> this.onFocusSearchBar());
    focusSearchBarMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(focusSearchBarMenuItem, focusSearchBarMenuItem.isDisable());
    queriesMenu.getItems().addAll(
        showNoTagsMenuItem,
        showNoFileMenuItem,
        showNoHashMenuItem,
        showVideosMenuItem,
        new SeparatorMenuItem(),
        this.savedQueriesMenu,
        new SeparatorMenuItem(),
        focusSearchBarMenuItem
    );

    final Menu toolsMenu = new Menu(language.translate("menu.tools"));
    final MenuItem operationsMenuItem = new MenuItem(
        language.translate("menu.tools.batch_operations"),
        theme.getIcon(Icon.BATCH_OPERATIONS, Icon.Size.SMALL)
    );
    operationsMenuItem.setOnAction(e -> this.onOperationsAction());
    operationsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
    this.menuItemStates.put(operationsMenuItem, operationsMenuItem.isDisable());
    final MenuItem convertPythonDbMenuItem = new MenuItem(
        language.translate("menu.tools.convert_python_db"),
        theme.getIcon(Icon.CONVERT_PYTHON_DB, Icon.Size.SMALL)
    );
    convertPythonDbMenuItem.setOnAction(e -> this.onConvertPythonDbMenuItem());
    toolsMenu.getItems().addAll(
        operationsMenuItem,
        new SeparatorMenuItem(),
        convertPythonDbMenuItem
    );

    final Menu helpMenu = new Menu(language.translate("menu.help"));
    final MenuItem aboutMenuItem = new MenuItem(
        language.translate("menu.help.about"),
        theme.getIcon(Icon.ABOUT, Icon.Size.SMALL)
    );
    aboutMenuItem.setOnAction(e -> this.onAbout());
    final MenuItem helpMenuItem = new MenuItem(
        language.translate("menu.help.help"),
        theme.getIcon(Icon.HELP, Icon.Size.SMALL)
    );
    helpMenuItem.setOnAction(e -> this.onHelp());
    helpMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN));
    helpMenuItem.setDisable(true); // TEMP until help is done
    this.menuItemStates.put(helpMenuItem, helpMenuItem.isDisable());
    helpMenu.getItems().addAll(aboutMenuItem, helpMenuItem);

    return new MenuBar(fileMenu, editMenu, viewMenu, queriesMenu, toolsMenu, helpMenu);
  }

  private ToolBar createToolBar() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final Button importMediaFilesButton = new Button(null, theme.getIcon(Icon.IMPORT_MEDIA_FILES, Icon.Size.BIG));
    importMediaFilesButton.setOnAction(e -> this.onImportMediaFiles());
    importMediaFilesButton.setTooltip(new Tooltip(language.translate("toolbar.file.import_images")));
    final Button importDirectoriesButton = new Button(null, theme.getIcon(Icon.IMPORT_DIRECTORIES, Icon.Size.BIG));
    importDirectoriesButton.setOnAction(e -> this.onImportDirectories());
    importDirectoriesButton.setTooltip(new Tooltip(language.translate("toolbar.file.import_directories")));

    this.editButton = new Button(null, theme.getIcon(Icon.EDIT_MEDIA_FILES, Icon.Size.BIG));
    this.editButton.setOnAction(e -> this.onEdit());
    this.editButton.setTooltip(new Tooltip(language.translate("toolbar.edit.edit_images")));
    this.editButton.setDisable(true);
    this.deleteButton = new Button(null, theme.getIcon(Icon.DELETE_MEDIA_FILES, Icon.Size.BIG));
    this.deleteButton.setOnAction(e -> this.onDelete());
    this.deleteButton.setTooltip(new Tooltip(language.translate("toolbar.edit.delete_images")));
    this.deleteButton.setDisable(true);
    this.moveMediaFilesButton = new Button(null, theme.getIcon(Icon.MOVE_MEDIAS, Icon.Size.BIG));
    this.moveMediaFilesButton.setOnAction(e -> this.onBatchMoveSelectedMediaFiles());
    this.moveMediaFilesButton.setTooltip(new Tooltip(language.translate("toolbar.edit.move_images")));
    this.moveMediaFilesButton.setDisable(true);
    this.mergeMediaFilesTagsButton = new Button(null, theme.getIcon(Icon.MERGE_MEDIA_FILES_TAGS, Icon.Size.BIG));
    this.mergeMediaFilesTagsButton.setOnAction(e -> this.onMergeSelectedMediaFilesTags());
    this.mergeMediaFilesTagsButton.setTooltip(new Tooltip(language.translate("toolbar.edit.merge_images_tags")));
    this.mergeMediaFilesTagsButton.setDisable(true);

    final Button operationsButton = new Button(null, theme.getIcon(Icon.BATCH_OPERATIONS, Icon.Size.BIG));
    operationsButton.setOnAction(e -> this.onOperationsAction());
    operationsButton.setTooltip(new Tooltip(language.translate("toolbar.tools.batch_operations")));

    this.slideshowButton = new Button(null, theme.getIcon(Icon.SLIDESHOW, Icon.Size.BIG));
    this.slideshowButton.setOnAction(e -> this.onSlideshow(false));
    this.slideshowButton.setTooltip(new Tooltip(language.translate("toolbar.view.slideshow")));
    this.slideshowButton.setDisable(true);
    this.slideshowSelectedButton = new Button(null, theme.getIcon(Icon.SLIDESHOW_SELECTED, Icon.Size.BIG));
    this.slideshowSelectedButton.setOnAction(e -> this.onSlideshow(true));
    this.slideshowSelectedButton.setTooltip(new Tooltip(language.translate("toolbar.view.slideshow_selected")));
    this.slideshowSelectedButton.setDisable(true);

    final Button showNoTagsButton = new Button(null, theme.getIcon(Icon.SEARCH_NO_TAGS, Icon.Size.BIG));
    showNoTagsButton.setOnAction(e -> this.onShowMediasWithNoTags());
    showNoTagsButton.setTooltip(new Tooltip(language.translate("toolbar.queries.show_images_with_no_tags")));
    final Button showNoFileButton = new Button(null, theme.getIcon(Icon.SEARCH_NO_FILE, Icon.Size.BIG));
    showNoFileButton.setOnAction(e -> this.onShowMediasWithNoFile());
    showNoFileButton.setTooltip(new Tooltip(language.translate("toolbar.queries.show_images_with_no_file")));
    final Button showNoHashButton = new Button(null, theme.getIcon(Icon.SEARCH_NO_HASH, Icon.Size.BIG));
    showNoHashButton.setOnAction(e -> this.onShowMediasWithNoHash());
    showNoHashButton.setTooltip(new Tooltip(language.translate("toolbar.queries.show_images_with_no_hash")));
    final Button showVideosButton = new Button(null, theme.getIcon(Icon.SEARCH_VIDEOS, Icon.Size.BIG));
    showVideosButton.setOnAction(e -> this.onShowVideos());
    showVideosButton.setTooltip(new Tooltip(language.translate("toolbar.queries.show_videos")));

    final Button helpButton = new Button(null, theme.getIcon(Icon.HELP, Icon.Size.BIG));
    helpButton.setOnAction(e -> this.onHelp());
    helpButton.setTooltip(new Tooltip(language.translate("toolbar.help.help")));
    helpButton.setDisable(true); // TEMP until help is done

    return new ToolBar(
        importMediaFilesButton,
        importDirectoriesButton,
        new Separator(),
        this.editButton,
        this.deleteButton,
        this.moveMediaFilesButton,
        this.mergeMediaFilesTagsButton,
        new Separator(),
        operationsButton,
        new Separator(),
        showNoTagsButton,
        showNoFileButton,
        showNoHashButton,
        showVideosButton,
        new Separator(),
        this.slideshowButton,
        this.slideshowSelectedButton,
        new Separator(),
        helpButton
    );
  }

  private SplitPane createContent() {
    this.tagsView.addTagClickListener(this::onTagDoubleClick);
    this.tagsView.addTagSelectionListener(this::onTagSelectionChange);
    this.tagsView.addCreateTagListener(this::onCreateTag);
    this.tagsView.addEditTagTypeListener(this::onEditTagType);
    this.tagsView.addDeleteTagTypeListener(this::onDeleteTagType);
    this.tagsView.addCreateTagTypeListener(this::onCreateTagType);
    this.tagsView.addEditTagsTypeListener(this::onEditTagsType);

    this.resultsTabPane.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
      if (isFocused)
        this.onResultsTabSelectionChanged(this.resultsTabPane.getSelectionModel().getSelectedItem());
    });
    this.resultsTabPane.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onResultsTabSelectionChanged(newValue));
    this.resultsTabPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      final int size = this.resultsTabPane.getTabs().size();
      if (event.getCode() == KeyCode.TAB && event.isShortcutDown()) {
        if (size > 1) {
          int i = this.resultsTabPane.getSelectionModel().getSelectedIndex();
          if (event.isShiftDown())
            i = i == 0 ? size - 1 : i - 1;
          else
            i = (i + 1) % size;
          this.resultsTabPane.getSelectionModel().select(i);
          this.getSelectedResultsView().focusSearchBar();
        }
        event.consume();
      }
    });
    this.resultsTabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> this.onResultsTabsUpdate());
    this.resultsTabPane.setTabDragPolicy(TabDragPolicy.REORDER);
    this.addResultsTab(null);

    final SplitPane splitPane = new SplitPane();
    splitPane.getItems().addAll(this.tagsView, this.resultsTabPane);
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
  }

  private void onResultsTabSelectionChanged(@NotNull Tab tab) {
    this.onMediaSelectionChange(((ResultsView) tab.getContent()).getSelectedMediaFiles());
    this.onResultsTabsUpdate();
  }

  private void onResultsTabsUpdate() {
    final boolean oneTab = this.resultsTabPane.getTabs().size() == 1;
    this.closeResultsTabMenuItem.setDisable(oneTab);
    this.resultsTabPane.setTabClosingPolicy(oneTab ? TabClosingPolicy.UNAVAILABLE : TabClosingPolicy.ALL_TABS);
    this.getSelectedResultsView().focusSearchBar();
  }

  private void addResultsTab(String title) {
    if (title == null)
      title = this.config.language().translate("results_tabs.new_tab.title");
    final ResultsView view = new ResultsView(this.config, this.db, this.queriesManager);
    view.addMediaItemDoubleClickListener(this::onMediaDoubleClick);
    view.addMediaItemSelectionListener(this::onMediaSelectionChange);
    view.addSimilarImagesListener(this::onSimilarImages);
    view.addSearchListener(this);
    final Tab tab = new Tab(title, view);
    this.resultsTabPane.getTabs().add(tab);
    this.resultsTabPane.getSelectionModel().select(tab);
    view.focusSearchBar();
  }

  private ResultsView getSelectedResultsView() {
    final Tab tab = this.resultsTabPane.getSelectionModel().getSelectedItem();
    if (tab == null)
      throw new IllegalStateException("No results tab selected");
    return (ResultsView) tab.getContent();
  }

  private Stream<ResultsView> getResultsViews() {
    return this.resultsTabPane.getTabs()
        .stream()
        .map(t -> (ResultsView) t.getContent());
  }

  private void loadFiles(final @NotNull List<Path> filesOrDirs) {
    if (filesOrDirs.isEmpty())
      return;

    final LoadingResult res = this.loadMediaFiles(filesOrDirs);
    final List<MediaFile> mediaFiles = res.mediaFiles();
    mediaFiles.sort(null);
    final List<Path> skipped = res.skipped();
    skipped.sort(null);
    final List<Path> errors = res.errors();
    errors.sort(null);

    if (!errors.isEmpty() || !skipped.isEmpty()) {
      final String headerKey;
      if (!errors.isEmpty() && skipped.isEmpty())
        headerKey = "alert.loading_error.header";
      else if (errors.isEmpty())
        headerKey = "alert.skipped_files.header";
      else
        headerKey = "alert.skipped_files_and_error.header";
      Alerts.info(
          this.config,
          headerKey,
          null,
          null,
          new FormatArg("skipped_nb", skipped.size()),
          new FormatArg("errors_nb", errors.size())
      );
    }

    if (mediaFiles.isEmpty()) {
      Alerts.info(
          this.config,
          "alert.no_files.header",
          null,
          null
      );
      return;
    }

    this.editMediasDialog.setMedias(mediaFiles, true);
    this.editMediasDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private LoadingResult loadMediaFiles(final @NotNull List<Path> filesOrDirs) {
    final List<MediaFile> mediaFiles = new LinkedList<>();
    final List<Path> errors = new LinkedList<>();
    final List<Path> skipped = new LinkedList<>();

    for (final Path fileOrDir : filesOrDirs)
      if (Files.isDirectory(fileOrDir))
        try (final var tree = Files.newDirectoryStream(fileOrDir)) {
          final List<Path> files = new LinkedList<>();
          tree.iterator().forEachRemaining(files::add);
          final var res = this.loadMediaFiles(files);
          mediaFiles.addAll(res.mediaFiles());
          skipped.addAll(res.skipped());
          errors.addAll(res.errors());
        } catch (final IOException | SecurityException e) {
          errors.add(fileOrDir);
        }
      else if (FileUtils.isValidFile(fileOrDir))
        try {
          if (this.db.isFileRegistered(fileOrDir))
            skipped.add(fileOrDir);
          else // Gain time by not computing hashes now as the EditMediasDialog will do when needed
            mediaFiles.add(new MediaFile(0, fileOrDir, null));
        } catch (final Exception e) {
          errors.add(fileOrDir);
        }

    return new LoadingResult(mediaFiles, skipped, errors);
  }

  private record LoadingResult(
      @NotNull List<MediaFile> mediaFiles,
      @NotNull List<Path> skipped,
      @NotNull List<Path> errors
  ) {
    private LoadingResult {
      Objects.requireNonNull(mediaFiles);
      Objects.requireNonNull(skipped);
      Objects.requireNonNull(errors);
    }
  }

  private void onTagDoubleClick(@NotNull Tag tag) {
    this.getSelectedResultsView().searchTag(tag);
  }

  private void onTagSelectionChange(final @NotNull List<Tag> tags) {
    this.selectedMediaFiles.clear();
    this.selectedTags.clear();
    this.selectedTags.addAll(tags);

    this.moveMediaFilesMenuItem.setDisable(true);
    this.moveMediaFilesButton.setDisable(true);
    this.mergeMediaFilesTagsMenuItem.setDisable(true);
    this.mergeMediaFilesTagsButton.setDisable(true);
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

  private void onCreateTag(TagType tagType) {
    this.createTagDialog.reset(tagType);
    this.createTagDialog.showAndWait().ifPresent(tag -> this.tagsView.refresh());
  }

  private void onEditTagType(@NotNull TagType tagType) {
    this.editTagTypeDialog.setTagType(tagType);
    this.editTagTypeDialog.showAndWait().ifPresent(type -> {
      this.tagsView.refresh();
      this.getResultsViews().forEach(ResultsView::refresh);
    });
  }

  private void onDeleteTagType(@NotNull TagType tagType) {
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
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return;
    }
    this.tagsView.refresh();
    this.getResultsViews().forEach(ResultsView::refresh);
  }

  private void onCreateTagType() {
    this.createTagTypeDialog.reset();
    this.createTagTypeDialog.showAndWait().ifPresent(type -> {
      this.tagsView.refresh();
      this.tagsView.selectTagType(type);
    });
  }

  private void onEditTagsType(final @NotNull List<Tag> tags, TagType type) {
    final Set<TagUpdate> updates = tags.stream()
        .map(tag -> new TagUpdate(tag.id(), tag.label(), type, tag.definition().orElse(null)))
        .collect(Collectors.toSet());
    try {
      this.db.updateTags(updates);
      this.tagsView.refresh();
      this.getResultsViews().forEach(ResultsView::refresh);
    } catch (final DatabaseOperationException e) {
      App.logger().error("Error updating tags", e);
    }
  }

  private void onMediaDoubleClick(@NotNull MediaFile mediaFile) {
    this.editMediasDialog.setMedias(List.of(mediaFile), false);
    this.editMediasDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private void onMediaSelectionChange(final @NotNull List<MediaFile> mediaFiles) {
    this.selectedTags.clear();
    this.selectedMediaFiles.clear();
    this.selectedMediaFiles.addAll(mediaFiles);

    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    final boolean empty = mediaFiles.isEmpty();
    final boolean notTwoSelected = mediaFiles.size() != 2;
    this.editMenuItem.setDisable(empty);
    this.editMenuItem.setGraphic(theme.getIcon(Icon.EDIT_MEDIA_FILES, Icon.Size.SMALL));
    this.editMenuItem.setText(language.translate("menu.edit.edit_images"));
    this.editButton.setDisable(empty);
    this.editButton.setGraphic(theme.getIcon(Icon.EDIT_MEDIA_FILES, Icon.Size.BIG));
    this.editButton.setTooltip(new Tooltip(language.translate("toolbar.edit.edit_images")));
    this.deleteMenuItem.setDisable(empty);
    this.deleteMenuItem.setGraphic(theme.getIcon(Icon.DELETE_MEDIA_FILES, Icon.Size.SMALL));
    this.deleteMenuItem.setText(language.translate("menu.edit.delete_images"));
    this.deleteButton.setDisable(empty);
    this.deleteButton.setGraphic(theme.getIcon(Icon.DELETE_MEDIA_FILES, Icon.Size.BIG));
    this.deleteButton.setTooltip(new Tooltip(language.translate("toolbar.edit.delete_images")));
    this.moveMediaFilesMenuItem.setDisable(empty);
    this.moveMediaFilesButton.setDisable(empty);
    this.mergeMediaFilesTagsMenuItem.setDisable(notTwoSelected);
    this.mergeMediaFilesTagsButton.setDisable(notTwoSelected);
    this.slideshowSelectedMenuItem.setDisable(empty);
    this.slideshowSelectedButton.setDisable(empty);
  }

  private void onSimilarImages(@NotNull MediaFile mediaFile) {
    final String escapedPath = mediaFile.path().toString()
        .replace("\"", "\\\"")
        .replace("\\", "\\\\");
    final String query = "similar_to=\"%s\"".formatted(escapedPath);
    this.addResultsTab(query);
    this.getSelectedResultsView().searchQuery(query);
  }

  @Override
  public void onSearchStart(@NotNull String query, final @NotNull ResultsView view) {
    this.resultsTabPane.getTabs().stream()
        .filter(tab -> tab.getContent() == view)
        .findFirst()
        .ifPresent(tab -> tab.setText(query));
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
    this.onGoingTasksCount++;
    if (this.onGoingTasksCount > 1)
      return;
    this.stage.getScene().getRoot().setDisable(true);
    // Record the disable-state of all menu items and disable them
    this.menuItemStates.replaceAll((menuItem, b) -> {
      final boolean disabled = menuItem.isDisable();
      menuItem.setDisable(true);
      return disabled;
    });
  }

  private void restoreInteractions() {
    this.onGoingTasksCount--;
    if (this.onGoingTasksCount != 0)
      return;
    this.stage.getScene().getRoot().setDisable(false);
    // Restore menu items’ states
    this.menuItemStates.forEach(MenuItem::setDisable);
  }

  private void onImportMediaFiles() {
    this.loadFiles(FileChoosers.showMediaFilesChooser(this.config, this.stage));
  }

  private void onImportDirectories() {
    FileChoosers.showDirectoryChooser(this.config, this.stage)
        .ifPresent(value -> this.loadFiles(List.of(value)));
  }

  /**
   * Open settings dialog.
   */
  private void onSettings() {
    this.settingsDialog.resetLocalConfig();
    this.settingsDialog.showAndWait();
  }

  /**
   * Open the dialog to edit selected tags/medias.
   */
  private void onEdit() {
    if (!this.selectedMediaFiles.isEmpty())
      this.editSelectedMedias();
    else if (this.selectedTags.size() == 1)
      this.editTag(this.selectedTags.get(0));
  }

  private void editSelectedMedias() {
    if (this.selectedMediaFiles.isEmpty())
      return;
    this.editMediasDialog.setMedias(this.selectedMediaFiles, false);
    this.editMediasDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private void editTag(@NotNull Tag tag) {
    this.editTagDialog.setTag(tag);
    this.editTagDialog.showAndWait().ifPresent(t -> {
      this.tagsView.refresh();
      this.getResultsViews().forEach(ResultsView::refresh);
    });
  }

  /**
   * Open the dialog to confirm the deletion of selected tags/medias
   * then delete them if the user confirms.
   */
  private void onDelete() {
    if (!this.selectedMediaFiles.isEmpty())
      this.deleteSelectedMedias();
    else if (!this.selectedTags.isEmpty())
      this.deleteSelectedTags();
  }

  private void deleteSelectedMedias() {
    final var fromDisk = Alerts.confirmCheckbox(
        this.config,
        "alert.delete_images.header",
        "alert.delete_images.label",
        null,
        false,
        new FormatArg("count", this.selectedMediaFiles.size())
    );
    if (fromDisk.isEmpty())
      return;

    final List<MediaFile> notDeleted = new LinkedList<>();
    for (final var mediaFile : this.selectedMediaFiles) {
      try {
        this.db.deleteMedia(mediaFile, fromDisk.get());
      } catch (final DatabaseOperationException e) {
        notDeleted.add(mediaFile);
      }
    }
    if (!notDeleted.isEmpty()) {
      Alerts.error(this.config, "alert.deletion_error.header", null, null);
      this.addResultsTab(this.config.language().translate("results_tabs.not_deleted.title"));
      this.getSelectedResultsView().listMedias(notDeleted);
    } else {
      this.getResultsViews().forEach(ResultsView::refresh);
      this.tagsView.refresh();
    }
  }

  private void deleteSelectedTags() {
    if (!this.selectedTags.isEmpty() && Alerts.confirmation(
        this.config,
        "alert.delete_tags.header",
        null,
        null))
      try {
        this.db.deleteTags(new HashSet<>(this.selectedTags));
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
      }
  }

  /**
   * Open the dialog to move the selected media files in batch.
   */
  private void onBatchMoveSelectedMediaFiles() {
    if (this.selectedMediaFiles.isEmpty())
      return;
    this.moveMediaFilesDialog.setMedias(this.selectedMediaFiles);
    this.moveMediaFilesDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private void onMergeSelectedMediaFilesTags() {
    if (this.selectedMediaFiles.size() != 2)
      return;
    final MediaFile mediaFile1 = this.selectedMediaFiles.get(0);
    final MediaFile mediaFile2 = this.selectedMediaFiles.get(1);
    final Set<Tag> mediaTags1, mediaTags2;
    try {
      mediaTags1 = this.db.getMediaTags(mediaFile1);
      mediaTags2 = this.db.getMediaTags(mediaFile2);
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return;
    }
    this.mergeMediaTagsDialog.setMedias(mediaFile1, mediaTags1, mediaFile2, mediaTags2);
    this.mergeMediaTagsDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private void onOperationsAction() {
    this.batchOperationsDialog.setMedias(this.getSelectedResultsView().mediasFiles(), this.selectedMediaFiles);
    this.batchOperationsDialog.showAndWait().ifPresent(anyUpdate -> {
      if (anyUpdate) {
        this.getResultsViews().forEach(ResultsView::refresh);
        this.tagsView.refresh();
      }
    });
  }

  private void onCloseResultsTab() {
    this.resultsTabPane.getTabs().remove(this.resultsTabPane.getSelectionModel().getSelectedItem());
  }

  /**
   * Open the slideshow dialog for the current query results or selected medias.
   */
  private void onSlideshow(boolean onlySelected) {
    final List<MediaFile> mediaFiles = new LinkedList<>();
    if (onlySelected)
      mediaFiles.addAll(this.selectedMediaFiles);
    else
      mediaFiles.addAll(this.getSelectedResultsView().mediasFiles());
    this.imageViewerDialog.setImages(mediaFiles);
    this.imageViewerDialog.showAndWait();
  }

  /**
   * Launch a search for medias with no tags.
   */
  private void onShowMediasWithNoTags() {
    this.searchFlag("no_tags");
  }

  /**
   * Launch a search for medias with no file.
   */
  private void onShowMediasWithNoFile() {
    this.searchFlag("no_file");
  }

  /**
   * Launch a search for medias with no hash.
   */
  private void onShowMediasWithNoHash() {
    this.searchFlag("no_hash");
  }

  /**
   * Launch a search for video files.
   */
  private void onShowVideos() {
    this.searchFlag("video");
  }

  private void searchFlag(String flag) {
    this.addResultsTab(flag);
    this.getSelectedResultsView().searchMediasWithFlag(flag);
  }

  /**
   * Open the dialog to convert a Python database file.
   */
  private void onConvertPythonDbMenuItem() {
    final var path = FileChoosers.showDatabaseFileChooser(this.config, this.stage);
    if (path.isEmpty())
      return;
    this.disableInteractions();
    this.progressDialog.show();
    DatabaseConnection.convertPythonDatabase(
        path.get(),
        newPath -> {
          this.progressDialog.hide();
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
            } catch (final IOException e) {
              App.logger().error("Unable to save config", e);
              Alerts.error(this.config, "dialog.settings.alert.save_error.header", null, null);
            }
          }
          this.restoreInteractions();
        },
        () -> {
          this.progressDialog.hide();
          this.restoreInteractions();
        },
        e -> {
          App.logger().error("Unable to convert database file", e);
          this.progressDialog.hide();
          Alerts.databaseError(this.config, e.errorCode());
          this.restoreInteractions();
        },
        this.progressDialog
    );
  }

  /**
   * Focus the search bar in ResultView.
   */
  private void onFocusSearchBar() {
    this.getSelectedResultsView().focusSearchBar();
  }

  private static final KeyCode[] KEYCODES = {
      KeyCode.NUMPAD1,
      KeyCode.NUMPAD2,
      KeyCode.NUMPAD3,
      KeyCode.NUMPAD4,
      KeyCode.NUMPAD5,
      KeyCode.NUMPAD6,
      KeyCode.NUMPAD7,
      KeyCode.NUMPAD8,
      KeyCode.NUMPAD9,
      KeyCode.NUMPAD0,
  };

  private void updateSavedQueries() {
    final ObservableList<MenuItem> menuItems = this.savedQueriesMenu.getItems();
    menuItems.subList(0, menuItems.size() - 2).clear(); // Remove all items except separator and manage item
    final List<SavedQuery> sortedQueries = this.queriesManager.entries();
    for (int i = 0; i < sortedQueries.size(); i++) {
      final SavedQuery savedQuery = sortedQueries.get(i);
      final MenuItem item = new MenuItem(savedQuery.name());
      item.setOnAction(event -> this.getSelectedResultsView().searchQuery(savedQuery.query()));
      if (i < KEYCODES.length)
        item.setAccelerator(new KeyCodeCombination(KEYCODES[i], KeyCombination.CONTROL_DOWN));
      menuItems.add(menuItems.size() - 2, item);
    }
    this.savedQueriesMenu.setDisable(menuItems.size() == 2);
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
