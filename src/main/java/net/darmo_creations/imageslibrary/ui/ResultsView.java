package net.darmo_creations.imageslibrary.ui;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ResultsView extends VBox implements ClickableListCellFactory.ClickListener<ResultsView.PictureEntry> {
  private static final int MAX_HISTORY_SIZE = 20;
  private static int globalId = 0;

  private final Set<ImageClickListener> imageClickListeners = new HashSet<>();
  private final Set<ImageSelectionListener> imageSelectionListeners = new HashSet<>();
  private final Set<SimilarImagesActionListener> similarImagesActionListeners = new HashSet<>();
  private final Set<SearchListener> searchListeners = new HashSet<>();

  private final Config config;
  private final DatabaseConnection db;
  private final SavedQueriesManager queriesManager;
  private final int id = ++globalId;

  private final Button saveQueryButton = new Button();
  private final MenuButton historyButton = new MenuButton();
  private final AutoCompleteField<Tag, Collection<String>> searchField;
  private final Button clearSearchButton = new Button();
  private final Label resultsLabel = new Label();
  private final ListView<PictureEntry> imagesList = new ListView<>();
  private final ImagePreviewPane imagePreviewPane;
  private final TextPopOver popup;

  public ResultsView(
      @NotNull Config config,
      final @NotNull DatabaseConnection db,
      @NotNull SavedQueriesManager queriesManager
  ) {
    super(5);
    this.config = config;
    this.db = db;
    this.queriesManager = queriesManager;
    queriesManager.addQueriesUpdateListener(this::updateSearchButtons);

    final Language language = config.language();
    final Theme theme = config.theme();

    this.imagePreviewPane = new ImagePreviewPane(config);
    this.imagePreviewPane.addEditTagsListener(picture -> this.onItemDoubleClick(new PictureEntry(picture, Set.of(), config, this.id)));
    this.imagePreviewPane.addSimilarImagesListeners(this::onSimilarImages);

    this.popup = new TextPopOver(PopOver.ArrowLocation.RIGHT_CENTER, config);

    this.saveQueryButton.setTooltip(new Tooltip(language.translate("image_search_field.save_query")));
    this.saveQueryButton.setGraphic(theme.getIcon(Icon.SAVE_QUERY, Icon.Size.BIG));
    this.saveQueryButton.setOnAction(event -> this.onSaveQuery());
    this.saveQueryButton.setDisable(true);

    this.historyButton.setTooltip(new Tooltip(language.translate("image_search_field.history")));
    this.historyButton.setGraphic(theme.getIcon(Icon.SEARCH_HISTORY, Icon.Size.BIG));
    this.historyButton.setDisable(true);

    final var textField = new StyleClassedTextField();
    this.searchField = new AutoCompleteField<>(
        textField,
        db.getAllTags(),
        t -> true,
        Tag::label,
        config.isQuerySyntaxHighlightingEnabled() ? new TagQuerySyntaxHighlighter() : null,
        List::of,
        config
    );
    textField.setPromptText(new Text(language.translate("image_search_field.search")));
    textField.setStyle("-fx-font-size: 2em");
    textField.setOnAction(e -> this.search(null));
    this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (this.popup.isShowing())
        this.popup.hide();
      this.updateSearchButtons();
    });

    final Button searchButton = new Button();
    searchButton.setOnAction(e -> this.search(null));
    searchButton.setGraphic(theme.getIcon(Icon.SEARCH, Icon.Size.BIG));
    searchButton.setTooltip(new Tooltip(language.translate("image_search_field.go")));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText("");
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.BIG));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);

    final ToggleButton syntaxHighlightingButton = new ToggleButton();
    config.querySyntaxHighlightingProperty().addListener((observable, oldValue, enabled) -> {
      syntaxHighlightingButton.setTooltip(new Tooltip(language.translate(
          "image_search_field.syntax_highlighting." + enabled)));
      this.searchField.setSyntaxHighlighter(enabled ? new TagQuerySyntaxHighlighter() : null);
    });
    syntaxHighlightingButton.setSelected(config.isQuerySyntaxHighlightingEnabled());
    syntaxHighlightingButton.setTooltip(new Tooltip(language.translate(
        "image_search_field.syntax_highlighting." + config.isQuerySyntaxHighlightingEnabled())));
    syntaxHighlightingButton.setOnAction(e -> {
      config.setQuerySyntaxHighlightingEnabled(!config.isQuerySyntaxHighlightingEnabled());
      try {
        config.save();
      } catch (final IOException ex) {
        App.logger().error("Unable to save config", ex);
      }
    });
    syntaxHighlightingButton.setGraphic(theme.getIcon(Icon.SYNTAX_HIGHLIGHTING, Icon.Size.BIG));

    final ToggleButton caseSensitivityButton = new ToggleButton();
    config.caseSensitiveQueriesByDefaultProperty().addListener((observable, oldValue, enabled) -> {
      caseSensitivityButton.setTooltip(new Tooltip(language.translate(
          "image_search_field.case_sensitivity." + enabled)));
      this.search(null);
    });
    caseSensitivityButton.setSelected(config.caseSensitiveQueriesByDefault());
    caseSensitivityButton.setTooltip(new Tooltip(language.translate(
        "image_search_field.case_sensitivity." + config.caseSensitiveQueriesByDefault())));
    caseSensitivityButton.setOnAction(e -> {
      config.setCaseSensitiveQueriesByDefault(!config.caseSensitiveQueriesByDefault());
      try {
        config.save();
      } catch (final IOException ex) {
        App.logger().error("Unable to save config", ex);
      }
    });
    caseSensitivityButton.setGraphic(theme.getIcon(Icon.CASE_SENSITIVITY, Icon.Size.BIG));

    this.resultsLabel.setText(language.translate("images_view.suggestion"));
    this.resultsLabel.getStyleClass().add("results-label");
    this.resultsLabel.setPadding(new Insets(0, 2, 0, 2));

    this.imagesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.imagesList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.C && event.isShortcutDown())
        this.copySelectedPaths();
    });
    this.imagesList.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onSelectionChange());
    this.imagesList.setCellFactory(item -> ClickableListCellFactory.forListener(this));

    HBox.setHgrow(this.searchField, Priority.ALWAYS);
    final HBox searchBox = new HBox(
        5,
        this.saveQueryButton,
        this.historyButton,
        this.searchField,
        searchButton,
        this.clearSearchButton,
        caseSensitivityButton,
        syntaxHighlightingButton
    );
    searchBox.setPadding(new Insets(2, 2, 0, 2));
    final HBox resultsLabelBox = new HBox(this.resultsLabel);
    resultsLabelBox.setAlignment(Pos.CENTER);
    final SplitPane splitPane = new SplitPane(this.imagesList, this.imagePreviewPane);
    splitPane.setDividerPositions(0.95);
    this.imagePreviewPane.addTagClickListener(this::searchTag);
    VBox.setVgrow(splitPane, Priority.ALWAYS);
    this.getChildren().addAll(searchBox, resultsLabelBox, splitPane);
  }

  /**
   * Return a list of the images listed in this view.
   */
  @Unmodifiable
  public List<Picture> pictures() {
    return this.imagesList.getItems().stream().map(PictureEntry::picture).toList();
  }

  /**
   * Return a list of the images that are currently selected in this view.
   */
  @Unmodifiable
  public List<Picture> getSelectedImages() {
    return this.imagesList.getSelectionModel().getSelectedItems().stream()
        .map(PictureEntry::picture)
        .toList();
  }

  /**
   * Focus the search bar.
   */
  public void focusSearchBar() {
    this.searchField.requestFocus();
  }

  /**
   * Replace the current query by the given tag and run a search.
   *
   * @param tag The tag to insert.
   */
  public void searchTag(@NotNull Tag tag) {
    this.searchField.setText(tag.label());
    this.searchField.requestFocus();
    this.search(null);
  }

  /**
   * Add a listener that will be notified whenever an image item is double-clicked.
   */
  public void addImageDoubleClickListener(@NotNull ImageClickListener listener) {
    this.imageClickListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever the list’s selection changes.
   */
  public void addImageSelectionListener(@NotNull ImageSelectionListener listener) {
    this.imageSelectionListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever a request to show similar images of a given picture is made.
   */
  public void addSimilarImagesListener(@NotNull SimilarImagesActionListener listener) {
    this.similarImagesActionListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever a tag search starts, ends or fails.
   */
  public void addSearchListener(@NotNull SearchListener listener) {
    this.searchListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Search for all images that match the given flag.
   */
  public void searchImagesWithFlag(@NotNull String flag) {
    this.searchField.setText(flag);
    this.searchField.requestFocus();
    this.search(null);
  }

  public void searchQuery(@NotNull String query) {
    this.searchField.setText(query);
    this.searchField.requestFocus();
    this.search(null);
  }

  /**
   * Refresh this view by re-running the current tag query.
   */
  public void refresh() {
    final var selection = this.imagesList.getSelectionModel().getSelectedItems();
    final PictureEntry entry = !selection.isEmpty() ? selection.get(0) : null;
    this.search(() -> {
      if (entry != null) {
        final Picture picture = entry.picture();
        try {
          if (this.imagesList.getItems().contains(entry)) {
            this.imagesList.getSelectionModel().select(entry);
            this.imagesList.requestFocus();
          } else if (this.db.pictureExists(picture.id())) {
            this.imagePreviewPane.setImage(picture, this.db.getImageTags(picture), this.hasSimilarImages(picture));
          } else
            this.imagePreviewPane.setImage(null, null, false);
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
        }
        this.imagesList.requestFocus();
      }
    });
  }

  /**
   * Show the given images in the list view.
   *
   * @param pictures The images to show.
   */
  public void listImages(final @NotNull Collection<Picture> pictures) {
    final Language language = this.config.language();
    final int count = pictures.size();

    if (pictures.isEmpty())
      this.resultsLabel.setText(language.translate("images_view.no_results"));
    else
      this.resultsLabel.setText(language.translate("images_view.results", count,
          new FormatArg("count", language.formatNumber(count))));

    final var listViewItems = this.imagesList.getItems();
    listViewItems.clear();
    pictures.stream()
        .sorted()
        .forEach(picture -> {
          Set<Tag> imageTags;
          try {
            imageTags = this.db.getImageTags(picture);
          } catch (final DatabaseOperationException e) {
            App.logger().error("Error getting tags for image {}", picture, e);
            imageTags = Set.of();
          }
          listViewItems.add(new PictureEntry(picture, imageTags, this.config, this.id));
        });
  }

  /**
   * Search for all images that match the current tag query.
   *
   * @param onSuccess An optional callback to run when the search succeeds.
   */
  private void search(Runnable onSuccess) {
    final var queryString = StringUtils.stripNullable(this.searchField.getText());
    if (queryString.isEmpty())
      return;

    final String query = queryString.get();
    final Language language = this.config.language();
    final TagQuery tagQuery;
    try {
      tagQuery = TagQueryParser.parse(query, this.db.getTagDefinitions(), DatabaseConnection.PSEUDO_TAGS, this.config);
    } catch (final TagQueryTooLargeException e) {
      this.showPopup(language.translate("image_search_field.recursive_loop_error"));
      return;
    } catch (final TagQuerySyntaxErrorException e) {
      this.showPopup(language.translate("image_search_field.query_syntax_error"));
      return;
    } catch (final InvalidPseudoTagException e) {
      this.showPopup(language.translate("image_search_field.invalid_pseudo_tag", new FormatArg("tag", e.pseudoTag())));
      return;
    }

    final var history = this.historyButton.getItems();
    final var matchingItem = history.stream().filter(t -> t.getText().equals(query)).findFirst();
    if (matchingItem.isEmpty()) {
      final MenuItem menuItem = new MenuItem(query);
      menuItem.setMnemonicParsing(false);
      menuItem.setOnAction(e -> {
        this.searchField.setText(query);
        this.searchField.requestFocus();
      });
      history.add(0, menuItem);
      if (history.size() > MAX_HISTORY_SIZE)
        history.remove(history.size() - 1);
      this.historyButton.setDisable(false);
    } else {
      // Put matching item on top
      final MenuItem item = matchingItem.get();
      history.remove(item);
      history.add(0, item);
    }

    this.performSearch(query, () -> this.db.queryPictures(tagQuery), onSuccess);
  }

  private void performSearch(@NotNull String query, @NotNull Search search, Runnable onSuccess) {
    this.searchListeners.forEach(l -> l.onSearchStart(query, this));
    new Thread(() -> {
      final Set<Picture> pictures;
      try {
        pictures = search.run();
      } catch (final DatabaseOperationException e) {
        Platform.runLater(() -> {
          Alerts.databaseError(this.config, e.errorCode());
          this.onSearchError();
        });
        return;
      }
      Platform.runLater(() -> {
        this.onSearchEnd(pictures);
        if (onSuccess != null)
          onSuccess.run();
      });
    }).start();
  }

  private void onSaveQuery() {
    final Optional<String> query = StringUtils.stripNullable(this.searchField.getText());
    if (query.isPresent() && !this.queriesManager.isQuerySaved(query.get())) {
      final Optional<String> nameOpt = Alerts.textInput(
          this.config,
          "alert.set_query_name.header",
          "alert.set_query_name.name_label",
          "alert.set_query_name.title",
          this.config.language().translate("alert.set_query_name.default_name"),
          null
      );
      nameOpt.ifPresent(name -> {
        boolean proceed = true;
        if (this.queriesManager.isNameSaved(name))
          proceed = Alerts.confirmation(
              this.config,
              "alert.duplicate_query_name.header",
              "alert.duplicate_query_name.content",
              null,
              new FormatArg("name", name),
              new FormatArg("query", this.queriesManager.getQuery(name))
          );
        if (proceed)
          try {
            this.queriesManager.saveQuery(name, query.get());
          } catch (final DatabaseOperationException e) {
            Alerts.databaseError(this.config, e.errorCode());
          }
      });
    }
  }

  private interface Search {
    Set<Picture> run() throws DatabaseOperationException;
  }

  private void showPopup(@NotNull String text) {
    this.popup.setText(text);
    this.popup.show(this.searchField);
  }

  private void onSearchEnd(final @NotNull Set<Picture> pictures) {
    this.listImages(pictures);
    this.searchListeners.forEach(listener -> listener.onSearchEnd(pictures.size()));
    this.searchField.requestFocus();
  }

  private void onSearchError() {
    this.searchListeners.forEach(SearchListener::onSearchFail);
    this.searchField.requestFocus();
  }

  private void updateSearchButtons() {
    final Optional<String> query = StringUtils.stripNullable(this.searchField.getText());
    final boolean noQuery = query.isEmpty();
    this.saveQueryButton.setDisable(noQuery || this.queriesManager.isQuerySaved(query.get()));
    this.clearSearchButton.setDisable(noQuery);
  }

  @Override
  public void onItemClick(@NotNull PictureEntry item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull PictureEntry pictureEntry) {
    this.imageClickListeners.forEach(listener -> listener.onImageClick(pictureEntry.picture()));
  }

  public void onSimilarImages(@NotNull Picture picture) {
    this.similarImagesActionListeners.forEach(l -> l.onSimilarImage(picture));
  }

  private void copySelectedPaths() {
    final var selectedItems = this.imagesList.getSelectionModel().getSelectedItems();
    final var joiner = new StringJoiner("\n");
    selectedItems.forEach(e -> joiner.add(e.picture().path().toString()));
    final var clipboardContent = new ClipboardContent();
    clipboardContent.putString(joiner.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  private void onSelectionChange() {
    final var selectedItems = this.imagesList.getSelectionModel().getSelectedItems();
    final var selection = selectedItems.stream()
        .filter(Objects::nonNull) // Selection model may return null values in the list
        .map(PictureEntry::picture)
        .toList();
    if (selection.size() == 1) {
      final PictureEntry entry = selectedItems.get(0);
      final Picture picture = entry.picture();
      this.imagePreviewPane.setImage(picture, entry.tags(), this.hasSimilarImages(picture));
    }
    this.imageSelectionListeners.forEach(listener -> listener.onSelectionChange(selection));
  }

  private boolean hasSimilarImages(@NotNull Picture picture) {
    if (picture.hash().isPresent()) {
      try {
        return !this.db.getSimilarImages(picture.hash().get(), picture).isEmpty();
      } catch (final DatabaseOperationException e) {
        App.logger().error("Failed to get similar images of {}", picture.path(), e);
      }
    }
    return false;
  }

  public static final class PictureEntry extends HBox implements Comparable<PictureEntry> {
    private final Picture picture;
    private final Set<Tag> tags;
    private final int viewId; // Prevents error if the same Picture is listed in two different ResultsView’s

    public PictureEntry(@NotNull Picture picture, final @NotNull Set<Tag> tags, final @NotNull Config config, int viewId) {
      super(5);
      this.picture = Objects.requireNonNull(picture);
      this.tags = Objects.requireNonNull(tags);
      this.viewId = viewId;
      final Language language = config.language();
      final Theme theme = config.theme();
      boolean exists;
      try {
        exists = Files.exists(picture.path());
      } catch (final SecurityException e) {
        exists = false;
      }
      if (!exists) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_FILE, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.missing_file")));
        this.getChildren().add(label);
      }
      if (tags.isEmpty()) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_TAGS, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.no_tags")));
        this.getChildren().add(label);
      }
      if (picture.hash().isEmpty()) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_HASH, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.no_hash")));
        this.getChildren().add(label);
      }
      this.getChildren().add(new Label(picture.path().toString()));
    }

    public Picture picture() {
      return this.picture;
    }

    public Set<Tag> tags() {
      return this.tags;
    }

    @Override
    public int compareTo(ResultsView.PictureEntry o) {
      return this.picture.path().compareTo(o.picture().path());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || this.getClass() != o.getClass())
        return false;
      final PictureEntry that = (PictureEntry) o;
      return Objects.equals(this.picture, that.picture) && this.viewId == that.viewId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.picture, this.viewId);
    }

    @Override
    public String toString() {
      return this.picture.path().toString();
    }
  }

  @FunctionalInterface
  public interface ImageClickListener {
    /**
     * Called when an item of the list is double-clicked.
     *
     * @param picture The clicked image.
     */
    void onImageClick(@NotNull Picture picture);
  }

  @FunctionalInterface
  public interface ImageSelectionListener {
    /**
     * Called when the image list’s selection changes or the list regains focus.
     *
     * @param pictures The selected pictures.
     */
    void onSelectionChange(@NotNull List<Picture> pictures);
  }

  @FunctionalInterface
  public interface SimilarImagesActionListener {
    /**
     * Called when a request for the images similar to the passed one is made.
     *
     * @param picture The picture to get the similar pictures of.
     */
    void onSimilarImage(@NotNull Picture picture);
  }

  public interface SearchListener {
    /**
     * Called right before the search starts.
     *
     * @param query The query being run.
     * @param view  The view that initiated the search.
     */
    void onSearchStart(@NotNull String query, @NotNull ResultsView view);

    /**
     * Called right after the search ended with no errors.
     *
     * @param resultsCount The number of images that matched the search query.
     */
    void onSearchEnd(int resultsCount);

    /**
     * Called when the search failed with an error.
     */
    void onSearchFail();
  }
}