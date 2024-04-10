package net.darmo_creations.imageslibrary.ui;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

public class ResultsView extends VBox {
  private final List<ImageListSelectionListener> imageListSelectionListeners = new ArrayList<>();
  private final List<SearchListener> searchListeners = new ArrayList<>();
  private final DatabaseConnection db;

  // TODO edit image when double-clicked
  private final ListView<PictureEntry> imagesList = new ListView<>();
  private final TextField searchField = new TextField();
  private final Button searchButton = new Button();
  private final Button clearSearchButton = new Button();
  private final Label errorLabel = new Label();

  public ResultsView(final DatabaseConnection db) {
    this.db = db;

    final Config config = App.config();
    final Language language = config.language();
    final Theme theme = config.theme();

    VBox.setVgrow(this.imagesList, Priority.ALWAYS);
    HBox.setHgrow(this.searchField, Priority.ALWAYS);
    final HBox searchBox = new HBox(
        5,
        this.searchField,
        this.searchButton,
        this.clearSearchButton
    );
    searchBox.setPadding(new Insets(2));
    this.getChildren().addAll(searchBox, this.errorLabel, this.imagesList);

    this.imagesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.imagesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        this.imageListSelectionListeners.forEach(listener -> {
          final var selection = this.imagesList.getSelectionModel().getSelectedItems().stream()
              .map(PictureEntry::picture)
              .toList();
          listener.onSelectionChanged(selection);
        })
    );

    this.searchField.setPromptText(language.translate("image_search_field.search"));
    this.searchField.setOnAction(e -> this.search());
    this.searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      this.errorLabel.setText(null);
      this.clearSearchButton.setDisable(StringUtils.stripNullable(newValue).isEmpty());
    });

    this.searchButton.setOnAction(e -> this.search());
    this.searchButton.setGraphic(theme.getIcon(Icon.SEARCH, Icon.Size.SMALL));
    this.searchButton.setTooltip(new Tooltip(language.translate("image_search_field.go")));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText(null);
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.SMALL));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);
  }

  /**
   * Refresh this view’s result list.
   */
  public void refresh() {
    this.search();
  }

  public void insertTagInSearchBar(Tag tag) {
    this.searchField.appendText(" " + tag.label());
  }

  public void addImageListSelectionListener(ImageListSelectionListener listener) {
    this.imageListSelectionListeners.add(listener);
  }

  public void addSearchListener(SearchListener listener) {
    this.searchListeners.add(listener);
  }

  private void search() {
    final var queryString = StringUtils.stripNullable(this.searchField.getText());
    if (queryString.isEmpty())
      return;

    this.imagesList.setDisable(true);
    this.searchField.setDisable(true);
    this.searchButton.setDisable(true);
    this.clearSearchButton.setDisable(true);
    this.searchListeners.forEach(SearchListener::onSearchStart);
    // TODO show spinning gif?

    final Language language = App.config().language();

    new Thread(() -> {
      final var tagDefinitions = this.db.getAllTags().stream()
          .filter(tag -> tag.definition().isPresent())
          .collect(Collectors.toMap(Tag::label, tag -> tag.definition().get()));
      final TagQuery query;
      try {
        query = TagQueryParser.parse(queryString.get(), tagDefinitions, DatabaseConnection.PSEUDO_TAGS);
      } catch (TagQueryTooLargeException e) {
        Platform.runLater(() -> {
          this.errorLabel.setText(
              language.translate("image_search_field.recursive_loop_error"));
          this.onSearchError();
        });
        return;
      } catch (TagQuerySyntaxErrorException e) {
        Platform.runLater(() -> {
          this.errorLabel.setText(
              language.translate("image_search_field.query_syntax_error"));
          this.onSearchError();
        });
        return;
      } catch (InvalidPseudoTagException e) {
        Platform.runLater(() -> {
          this.errorLabel.setText(
              language.translate("image_search_field.invalid_pseudo_tag", new FormatArg("tag", e.pseudoTag())));
          this.onSearchError();
        });
        return;
      }
      final Set<Picture> pictures;
      try {
        pictures = this.db.queryPictures(query);
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

  private void onSearchEnd(final Set<Picture> pictures) {
    // TODO show number of results above list
    // TODO show images somewhere
    this.resetFieldsStates();
    this.imagesList.getItems().clear();
    pictures.forEach(picture -> this.imagesList.getItems().add(new PictureEntry(picture)));
    this.imagesList.getItems().sort(null);
    this.searchListeners.forEach(listener -> listener.onSearchEnd(pictures.size()));
  }

  private void onSearchError() {
    this.resetFieldsStates();
    this.searchListeners.forEach(SearchListener::onSearchError);
  }

  private void resetFieldsStates() {
    this.imagesList.setDisable(false);
    this.searchField.setDisable(false);
    this.searchField.requestFocus();
    this.searchButton.setDisable(false);
    this.clearSearchButton.setDisable(false);
  }

  private record PictureEntry(Picture picture) implements Comparable<PictureEntry> {
    @Override
    public String toString() {
      return this.picture.path().toString();
    }

    @Override
    public int compareTo(@NotNull ResultsView.PictureEntry o) {
      return this.picture.path().compareTo(o.picture().path());
    }
  }

  public interface ImageListSelectionListener {
    void onSelectionChanged(List<Picture> pictures);
  }

  public interface SearchListener {
    void onSearchStart();

    void onSearchEnd(int resultsCount);

    void onSearchError();
  }
}