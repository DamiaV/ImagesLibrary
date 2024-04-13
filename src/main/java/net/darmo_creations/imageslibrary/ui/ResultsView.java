package net.darmo_creations.imageslibrary.ui;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.util.*;
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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

// TODO move case sensitive setting from dialog to toggle button next to search bar
// TODO add toggle button to toggle preview sidebar
//  update and save config when any of these buttons is clicked
public class ResultsView extends VBox implements ClickableListCellFactory.ClickListener<ResultsView.PictureEntry> {
  private final List<ImageClickListener> imageClickListeners = new ArrayList<>();
  private final List<ImageSelectionListener> imageSelectionListeners = new ArrayList<>();
  private final List<SearchListener> searchListeners = new ArrayList<>();

  private final DatabaseConnection db;

  private final MenuButton historyButton = new MenuButton();
  private final AutoCompleteTextField<Tag> searchField;
  private final Button searchButton = new Button();
  private final Button clearSearchButton = new Button();
  private final Label resultsLabel = new Label();
  private final ListView<PictureEntry> imagesList = new ListView<>();
  private final ImagePreviewPane imagePreviewPane = new ImagePreviewPane();
  private final PopOver popup;

  private boolean popupInitialized = false;

  public ResultsView(final DatabaseConnection db) {
    super(5);
    this.db = db;

    final Config config = App.config();
    final Language language = config.language();
    final Theme theme = config.theme();

    final Label content = new Label();
    this.popup = new PopOver(content);
    content.setPadding(new Insets(5));
    this.popup.setArrowLocation(PopOver.ArrowLocation.RIGHT_CENTER);
    this.popup.setFadeInDuration(new Duration(100));
    this.popup.setFadeOutDuration(new Duration(100));

    this.historyButton.setTooltip(new Tooltip(language.translate("image_search_field.history")));
    this.historyButton.setGraphic(theme.getIcon(Icon.SEARCH_HISTORY, Icon.Size.BIG));
    this.historyButton.setDisable(true);

    this.searchField = new AutoCompleteTextField<>(
        db.getAllTags(),
        Tag::label,
        config.isQuerySyntaxHighlightingEnabled() ? new TagQuerySyntaxHighlighter() : null
    );
    this.searchField.setPromptText(new Text(language.translate("image_search_field.search")));
    this.searchField.setStyle("-fx-font-size: 2em");
    this.searchField.setOnAction(e -> this.search());
    this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (this.popup.isShowing())
        this.popup.hide();
      this.clearSearchButton.setDisable(StringUtils.stripNullable(newValue).isEmpty());
    });

    this.searchButton.setOnAction(e -> this.search());
    this.searchButton.setGraphic(theme.getIcon(Icon.SEARCH, Icon.Size.BIG));
    this.searchButton.setTooltip(new Tooltip(language.translate("image_search_field.go")));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText("");
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.BIG));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);

    final ToggleButton syntaxHighlightingButton = new ToggleButton();
    syntaxHighlightingButton.setSelected(config.isQuerySyntaxHighlightingEnabled());
    syntaxHighlightingButton.setOnAction(e -> {
      config.setQuerySyntaxHighlightingEnabled(!config.isQuerySyntaxHighlightingEnabled());
      try {
        config.save();
      } catch (IOException ex) {
        App.LOGGER.error("Unable to save config", ex);
      }
      this.searchField.setSyntaxHighlighter(config.isQuerySyntaxHighlightingEnabled() ? new TagQuerySyntaxHighlighter() : null);
    });
    syntaxHighlightingButton.setGraphic(theme.getIcon(Icon.SYNTAX_HIGHLIGHTING, Icon.Size.BIG));
    syntaxHighlightingButton.setTooltip(new Tooltip(language.translate("image_search_field.syntax_highlighting")));

    this.resultsLabel.setText(language.translate("images_view.suggestion"));
    this.resultsLabel.getStyleClass().add("results-label");
    this.resultsLabel.setPadding(new Insets(0, 2, 0, 2));

    this.imagesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.imagesList.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onSelectionChange());
    this.imagesList.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue)
        this.onSelectionChange();
    });
    this.imagesList.setCellFactory(item -> ClickableListCellFactory.forListener(this));

    HBox.setHgrow(this.searchField, Priority.ALWAYS);
    final HBox searchBox = new HBox(
        5,
        this.historyButton,
        this.searchField,
        this.searchButton,
        this.clearSearchButton,
        syntaxHighlightingButton
    );
    searchBox.setPadding(new Insets(2, 2, 0, 2));
    final HBox resultsLabelBox = new HBox(this.resultsLabel);
    resultsLabelBox.setAlignment(Pos.CENTER);
    final SplitPane splitPane = new SplitPane(this.imagesList, this.imagePreviewPane);
    splitPane.setDividerPositions(0.95);
    this.imagePreviewPane.setSplitPane(splitPane, false);
    this.imagePreviewPane.addTagClickListener(this::searchTag);
    VBox.setVgrow(splitPane, Priority.ALWAYS);
    this.getChildren().addAll(searchBox, resultsLabelBox, splitPane);
  }

  /**
   * Refresh this view’s result list.
   */
  public void refresh() {
    this.search();
  }

  /**
   * Replace the current query by the given tag and run a search.
   *
   * @param tag The tag to insert.
   */
  public void searchTag(Tag tag) {
    this.searchField.setText(tag.label());
    this.searchField.requestFocus();
    this.search();
  }

  /**
   * Add a listener that will be notified whenever an image item is double-clicked.
   */
  public void addImageClickListener(ImageClickListener listener) {
    this.imageClickListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever the list’s selection changes.
   */
  public void addImageSelectionListener(ImageSelectionListener listener) {
    this.imageSelectionListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Add a listener that will be notified whenever a tag search starts, ends or fails.
   */
  public void addSearchListener(SearchListener listener) {
    this.searchListeners.add(Objects.requireNonNull(listener));
  }

  private void search() {
    final var queryString = StringUtils.stripNullable(this.searchField.getText());
    if (queryString.isEmpty())
      return;

    final String query = queryString.get();
    final var tagDefinitions = this.db.getAllTags().stream()
        .filter(tag -> tag.definition().isPresent())
        .collect(Collectors.toMap(Tag::label, tag -> tag.definition().get()));
    final Language language = App.config().language();
    final TagQuery tagQuery;
    try {
      tagQuery = TagQueryParser.parse(query, tagDefinitions, DatabaseConnection.PSEUDO_TAGS);
    } catch (TagQueryTooLargeException e) {
      this.showPopup(language.translate("image_search_field.recursive_loop_error"));
      return;
    } catch (TagQuerySyntaxErrorException e) {
      this.showPopup(language.translate("image_search_field.query_syntax_error"));
      return;
    } catch (InvalidPseudoTagException e) {
      this.showPopup(language.translate("image_search_field.invalid_pseudo_tag", new FormatArg("tag", e.pseudoTag())));
      return;
    }

    this.imagesList.setDisable(true);
    this.searchField.setDisable(true);
    this.searchButton.setDisable(true);
    this.clearSearchButton.setDisable(true);
    final var history = this.historyButton.getItems();
    if (history.isEmpty() || !history.get(history.size() - 1).getText().equals(query)) {
      final MenuItem menuItem = new MenuItem(query);
      menuItem.setOnAction(e -> {
        this.searchField.setText(query);
        this.searchField.requestFocus();
      });
      history.add(0, menuItem);
      this.historyButton.setDisable(false);
    }

    this.searchListeners.forEach(SearchListener::onSearchStart);
    new Thread(() -> {
      final Set<Picture> pictures;
      try {
        pictures = this.db.queryPictures(tagQuery);
      } catch (DatabaseOperationException e) {
        Platform.runLater(() -> {
          Alerts.databaseError(e.errorCode());
          this.onSearchError();
        });
        return;
      }
      Platform.runLater(() -> this.onSearchEnd(pictures));
    }).start();
  }

  private void showPopup(String text) {
    ((Label) this.popup.getContentNode()).setText(text);
    this.popup.show(this.searchField);
    if (!this.popupInitialized) {
      // From https://stackoverflow.com/a/36404968/3779986
      final var stylesheets = ((Parent) this.popup.getSkin().getNode()).getStylesheets();
      App.config().theme().getStyleSheets().forEach(path -> stylesheets.add(path.toExternalForm()));
      this.popupInitialized = true;
    }
  }

  private void onSearchEnd(final Set<Picture> pictures) {
    final Language language = App.config().language();
    final int count = pictures.size();
    if (pictures.isEmpty())
      this.resultsLabel.setText(language.translate("images_view.no_results"));
    else
      this.resultsLabel.setText(language.translate("images_view.results", count,
          new FormatArg("count", language.formatNumber(count))));
    this.imagesList.getItems().clear();
    for (final var picture : pictures) {
      Set<Tag> imageTags;
      try {
        imageTags = this.db.getImageTags(picture);
      } catch (DatabaseOperationException e) {
        App.LOGGER.error("Error getting tags for image {}", picture, e);
        imageTags = Set.of();
      }
      this.imagesList.getItems().add(new PictureEntry(picture, imageTags));
    }
    this.imagesList.getItems().sort(null);
    this.searchListeners.forEach(listener -> listener.onSearchEnd(count));
    this.resetFieldsStates();
  }

  private void onSearchError() {
    this.searchListeners.forEach(SearchListener::onSearchFail);
    this.resetFieldsStates();
  }

  private void resetFieldsStates() {
    this.imagesList.setDisable(false);
    this.searchField.setDisable(false);
    this.searchField.requestFocus();
    this.searchButton.setDisable(false);
    this.clearSearchButton.setDisable(false);
  }

  @Override
  public void onItemClick(PictureEntry item) {
  }

  @Override
  public void onItemDoubleClick(PictureEntry pictureEntry) {
    this.imageClickListeners.forEach(listener -> listener.onImageClick(pictureEntry.picture()));
  }

  private void onSelectionChange() {
    final var selectedItems = this.imagesList.getSelectionModel().getSelectedItems();
    final var selection = selectedItems.stream()
        .map(PictureEntry::picture)
        .toList();
    if (selection.size() == 1)
      this.imagePreviewPane.setImage(selection.get(0), selectedItems.get(0).tags());
    this.imageSelectionListeners.forEach(listener -> listener.onSelectionChange(selection));
  }

  public static final class PictureEntry extends HBox implements Comparable<PictureEntry> {
    private final Picture picture;
    private final Set<Tag> tags;

    public PictureEntry(Picture picture, final Set<Tag> tags) {
      super(5);
      this.picture = picture;
      this.tags = tags;
      final Config config = App.config();
      final Language language = config.language();
      final Theme theme = config.theme();
      if (!Files.exists(picture.path())) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_FILE, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.missing_file")));
        this.getChildren().add(label);
      }
      if (tags.isEmpty()) {
        final Label label = new Label(null, theme.getIcon(Icon.NO_TAGS, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(language.translate("images_view.result.no_tags")));
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
    void onImageClick(Picture picture);
  }

  @FunctionalInterface
  public interface ImageSelectionListener {
    /**
     * Called when the image list’s selection changes or the list regains focus.
     *
     * @param pictures The selected pictures.
     */
    void onSelectionChange(List<Picture> pictures);
  }

  public interface SearchListener {
    /**
     * Called right before the search starts.
     */
    void onSearchStart();

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