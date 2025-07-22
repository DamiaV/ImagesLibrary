package net.darmo_creations.bildumilo.ui;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.query_parser.*;
import net.darmo_creations.bildumilo.query_parser.ex.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.ui.dialogs.*;
import net.darmo_creations.bildumilo.ui.syntax_highlighting.*;
import net.darmo_creations.bildumilo.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ResultsView extends VBox implements ClickableListCellFactory.ClickListener<ResultsView.MediaEntry> {
  private static final int MAX_HISTORY_SIZE = 20;
  private static int globalId = 0;

  private final Set<MediaItemClickListener> mediaItemClickListeners = new HashSet<>();
  private final Set<MediaItemSelectionListener> mediaItemSelectionListeners = new HashSet<>();
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
  private final ListView<MediaEntry> mediasList = new ListView<>();
  private final MediaPreviewPane mediaPreviewPane;
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

    this.mediaPreviewPane = new MediaPreviewPane(config);
    this.mediaPreviewPane.addEditTagsListener(media -> this.onItemDoubleClick(new MediaEntry(media, Set.of(), config, this.id)));
    this.mediaPreviewPane.addSimilarImagesListeners(this::onSimilarImages);

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

    this.mediasList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.mediasList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.C && event.isShortcutDown())
        this.copySelectedPaths();
    });
    this.mediasList.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onSelectionChange());
    this.mediasList.setCellFactory(item -> ClickableListCellFactory.forListener(this));
    this.mediasList.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
      if (isFocused)
        this.onSelectionChange();
    });

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
    final SplitPane splitPane = new SplitPane(this.mediasList, this.mediaPreviewPane);
    splitPane.setDividerPositions(0.95);
    this.mediaPreviewPane.addTagClickListener(this::searchTag);
    VBox.setVgrow(splitPane, Priority.ALWAYS);
    this.getChildren().addAll(searchBox, resultsLabelBox, splitPane);
  }

  /**
   * Return a list of the medias listed in this view.
   */
  @Unmodifiable
  public List<MediaFile> mediasFiles() {
    return this.mediasList.getItems().stream().map(MediaEntry::mediaFile).toList();
  }

  /**
   * Return a list of the medias that are currently selected in this view.
   */
  @Unmodifiable
  public List<MediaFile> getSelectedMediaFiles() {
    return this.mediasList.getSelectionModel().getSelectedItems().stream()
        .map(MediaEntry::mediaFile)
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
   * Add a listener that will be notified whenever a media item is double-clicked.
   */
  public void addMediaItemDoubleClickListener(@NotNull ResultsView.MediaItemClickListener listener) {
    this.mediaItemClickListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever the list’s selection changes.
   */
  public void addMediaItemSelectionListener(@NotNull ResultsView.MediaItemSelectionListener listener) {
    this.mediaItemSelectionListeners.add(Objects.requireNonNull(listener));
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
   * Search for all medias that match the given flag.
   *
   * @param flag The name of the flag without the "#".
   */
  public void searchMediasWithFlag(@NotNull String flag) {
    this.searchField.setText("#" + flag);
    this.searchField.requestFocus();
    this.search(null);
  }

  /**
   * Set the search bar’s query string.
   *
   * @param query A query string.
   */
  public void searchQuery(@NotNull String query) {
    this.searchField.setText(query);
    this.searchField.requestFocus();
    this.search(null);
  }

  /**
   * Refresh this view by re-running the current tag query.
   */
  public void refresh() {
    final var selection = this.mediasList.getSelectionModel().getSelectedItems();
    final MediaEntry entry = !selection.isEmpty() ? selection.get(0) : null;
    this.search(() -> {
      if (entry != null) {
        final MediaFile mediaFile = entry.mediaFile();
        try {
          if (this.mediasList.getItems().contains(entry)) {
            this.mediasList.getSelectionModel().select(entry);
            this.mediasList.requestFocus();
          } else if (this.db.mediaExists(mediaFile.id())) {
            this.mediaPreviewPane.setMedia(mediaFile, this.db.getMediaTags(mediaFile), this.hasSimilarImages(mediaFile));
          } else
            this.mediaPreviewPane.setMedia(null, null, false);
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
        }
        this.mediasList.requestFocus();
      }
    });
  }

  /**
   * Show the given medias in the list view.
   *
   * @param mediaFiles The medias to show.
   */
  public void listMedias(final @NotNull Collection<MediaFile> mediaFiles) {
    final Language language = this.config.language();
    final int count = mediaFiles.size();

    if (mediaFiles.isEmpty())
      this.resultsLabel.setText(language.translate("images_view.no_results"));
    else
      this.resultsLabel.setText(language.translate("images_view.results", count,
          new FormatArg("count", language.formatNumber(count))));

    final var listViewItems = this.mediasList.getItems();
    listViewItems.clear();
    mediaFiles.stream()
        .sorted()
        .forEach(media -> {
          Set<Tag> mediaTags;
          try {
            mediaTags = this.db.getMediaTags(media);
          } catch (final DatabaseOperationException e) {
            App.logger().error("Error getting tags for media file {}", media, e);
            mediaTags = Set.of();
          }
          listViewItems.add(new MediaEntry(media, mediaTags, this.config, this.id));
        });
  }

  /**
   * Search for all medias that match the current tag query.
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

    this.performSearch(query, () -> this.db.queryMedias(tagQuery), onSuccess);
  }

  private void performSearch(@NotNull String query, @NotNull Search search, Runnable onSuccess) {
    this.searchListeners.forEach(l -> l.onSearchStart(query, this));
    new Thread(() -> {
      final Set<MediaFile> mediaFiles;
      try {
        mediaFiles = search.run();
      } catch (final DatabaseOperationException e) {
        Platform.runLater(() -> {
          Alerts.databaseError(this.config, e.errorCode());
          this.onSearchError();
        });
        return;
      }
      Platform.runLater(() -> {
        this.onSearchEnd(mediaFiles);
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
    Set<MediaFile> run() throws DatabaseOperationException;
  }

  private void showPopup(@NotNull String text) {
    this.popup.setText(text);
    this.popup.show(this.searchField);
  }

  private void onSearchEnd(final @NotNull Set<MediaFile> mediaFiles) {
    this.listMedias(mediaFiles);
    this.searchListeners.forEach(listener -> listener.onSearchEnd(mediaFiles.size()));
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
  public void onItemClick(@NotNull ResultsView.MediaEntry item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull ResultsView.MediaEntry mediaEntry) {
    this.mediaItemClickListeners.forEach(listener -> listener.onMediaClick(mediaEntry.mediaFile()));
  }

  public void onSimilarImages(@NotNull MediaFile mediaFile) {
    this.similarImagesActionListeners.forEach(l -> l.onSimilarImage(mediaFile));
  }

  private void copySelectedPaths() {
    final var selectedItems = this.mediasList.getSelectionModel().getSelectedItems();
    final var joiner = new StringJoiner("\n");
    selectedItems.forEach(e -> joiner.add(e.mediaFile().path().toString()));
    final var clipboardContent = new ClipboardContent();
    clipboardContent.putString(joiner.toString());
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  private void onSelectionChange() {
    final var selectedItems = this.mediasList.getSelectionModel().getSelectedItems();
    final var selection = selectedItems.stream()
        .filter(Objects::nonNull) // Selection model may return null values in the list
        .map(MediaEntry::mediaFile)
        .toList();
    if (selection.size() == 1) {
      final MediaEntry entry = selectedItems.get(0);
      final MediaFile mediaFile = entry.mediaFile();
      final var previewedMediaFile = this.mediaPreviewPane.getMediaFile();
      if (previewedMediaFile.isEmpty() || previewedMediaFile.get() != mediaFile)
        this.mediaPreviewPane.setMedia(mediaFile, entry.tags(), this.hasSimilarImages(mediaFile));
    }
    this.mediaItemSelectionListeners.forEach(listener -> listener.onSelectionChange(selection));
  }

  private boolean hasSimilarImages(@NotNull MediaFile mediaFile) {
    if (mediaFile.hash().isPresent()) {
      try {
        return !this.db.getSimilarImages(mediaFile.hash().get(), mediaFile).isEmpty();
      } catch (final DatabaseOperationException e) {
        App.logger().error("Failed to get similar images of {}", mediaFile.path(), e);
      }
    }
    return false;
  }

  public static final class MediaEntry extends HBox implements Comparable<MediaEntry> {
    private final MediaFile mediaFile;
    private final Set<Tag> tags;
    private final int viewId; // Prevents error if the same media is listed in two different ResultsView’s

    public MediaEntry(@NotNull MediaFile mediaFile, final @NotNull Set<Tag> tags, final @NotNull Config config, int viewId) {
      super(5);
      this.mediaFile = Objects.requireNonNull(mediaFile);
      this.tags = Objects.requireNonNull(tags);
      this.viewId = viewId;
      final Language language = config.language();
      final Theme theme = config.theme();
      boolean exists;
      try {
        exists = Files.exists(mediaFile.path());
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
      if (!mediaFile.isVideo() && mediaFile.hash().isEmpty()) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_HASH, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.no_hash")));
        this.getChildren().add(label);
      }
      this.getChildren().add(new Label(mediaFile.path().toString()));
    }

    public MediaFile mediaFile() {
      return this.mediaFile;
    }

    public Set<Tag> tags() {
      return this.tags;
    }

    @Override
    public int compareTo(MediaEntry o) {
      return this.mediaFile.path().compareTo(o.mediaFile().path());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || this.getClass() != o.getClass())
        return false;
      final MediaEntry that = (MediaEntry) o;
      return Objects.equals(this.mediaFile, that.mediaFile) && this.viewId == that.viewId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.mediaFile, this.viewId);
    }

    @Override
    public String toString() {
      return this.mediaFile.path().toString();
    }
  }

  @FunctionalInterface
  public interface MediaItemClickListener {
    /**
     * Called when an item of the media result list is double-clicked.
     *
     * @param mediaFile The clicked media.
     */
    void onMediaClick(@NotNull MediaFile mediaFile);
  }

  @FunctionalInterface
  public interface MediaItemSelectionListener {
    /**
     * Called when the media list’s selection changes or the list regains focus.
     *
     * @param mediaFiles The selected medias.
     */
    void onSelectionChange(@NotNull List<MediaFile> mediaFiles);
  }

  @FunctionalInterface
  public interface SimilarImagesActionListener {
    /**
     * Called when a request for the images similar to the passed one is made.
     *
     * @param mediaFile The image to get the similar images of.
     */
    void onSimilarImage(@NotNull MediaFile mediaFile);
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
     * @param resultsCount The number of medias that matched the search query.
     */
    void onSearchEnd(int resultsCount);

    /**
     * Called when the search failed with an error.
     */
    void onSearchFail();
  }
}