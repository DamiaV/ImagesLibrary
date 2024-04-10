package net.darmo_creations.imageslibrary.ui;

import javafx.beans.binding.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;

import java.util.*;

public class TagsView extends VBox { // TODO option to create/edit/delete tags and tag types from here
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final ObservableSet<TreeItem<TagsTreeEntry>> searchMatches = FXCollections.observableSet(new HashSet<>());

  private final Set<Tag> tags;
  private final Map<Integer, Integer> tagsCounts;
  private final Set<TagType> tagTypes;
  private final Map<Integer, Integer> tagTypesCounts;

  private final TreeView<TagsTreeEntry> tagsTree = new TreeView<>();
  private final TextField searchField = new TextField();
  private final Button clearSearchButton = new Button();

  /**
   * Create a new tag tree view.
   *
   * @param tags       A view to the available tags.
   * @param tagsCounts A view to the existing tags use counts.
   * @param tagTypes   A view to the available tag types.
   */
  public TagsView(
      final Set<Tag> tags,
      final Map<Integer, Integer> tagsCounts,
      final Set<TagType> tagTypes,
      final Map<Integer, Integer> tagTypesCounts
  ) {
    this.tags = Objects.requireNonNull(tags);
    this.tagsCounts = Objects.requireNonNull(tagsCounts);
    this.tagTypes = Objects.requireNonNull(tagTypes);
    this.tagTypesCounts = Objects.requireNonNull(tagTypesCounts);

    final Config config = App.config();
    final Language language = config.language();
    final Theme theme = config.theme();

    VBox.setVgrow(this.tagsTree, Priority.ALWAYS);
    HBox.setHgrow(this.searchField, Priority.ALWAYS);
    final HBox searchBox = new HBox(
        5,
        this.searchField,
        this.clearSearchButton
    );
    searchBox.setPadding(new Insets(2));
    this.getChildren().addAll(searchBox, this.tagsTree);

    this.searchField.setPromptText(language.translate("tag_search_field.search"));
    this.searchField.textProperty().addListener((observable, oldValue, newValue) -> this.onSearchFilterChange(newValue));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText(null);
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.SMALL));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);

    this.refresh();
  }

  /**
   * Called whenever the search filter changes.
   *
   * @param text The filter text.
   */
  private void onSearchFilterChange(String text) {
    this.searchMatches.clear();
    final var filter = StringUtils.stripNullable(text);
    this.clearSearchButton.setDisable(filter.isEmpty());
    if (filter.isEmpty())
      return;
    final Set<TreeItem<TagsTreeEntry>> matches = new HashSet<>();
    for (final var item : this.tagsTree.getRoot().getChildren())
      this.searchMatchingItems(item, matches, filter.get());
    this.searchMatches.addAll(matches);
  }

  /**
   * Search for tree items matching the given search query.
   *
   * @param searchNode   Tree item to search children of.
   * @param matches      Set to populate with matches.
   * @param searchFilter Search filter.
   */
  private void searchMatchingItems(
      final TreeItem<TagsTreeEntry> searchNode,
      final Set<TreeItem<TagsTreeEntry>> matches,
      String searchFilter
  ) {
    for (final var item : searchNode.getChildren())
      if (item.getValue().toString().toLowerCase().contains(searchFilter.toLowerCase()))
        matches.add(item);
  }

  public void refresh() {
    final Language language = App.config().language();

    this.tagsTree.setRoot(new TreeItem<>());
    this.tagsTree.setShowRoot(false);
    this.tagsTree.setCellFactory(tree -> new SearchHighlightingTreeCell());
    final Map<TagType, TreeItem<TagsTreeEntry>> tagTypeItems = new HashMap<>();
    final TreeItem<TagsTreeEntry> defaultItem = new TreeItem<>(new DummyEntry(language.translate("tags_tree.no_type")));
    tagTypeItems.put(null, defaultItem);
    this.tagsTree.getRoot().getChildren().add(defaultItem);
    this.tagTypes.stream().sorted(Comparator.comparing(TagType::label)).forEach(tagType -> {
      final TreeItem<TagsTreeEntry> item = new TreeItem<>(new TagTypeEntry(tagType));
      tagTypeItems.put(tagType, item);
      this.tagsTree.getRoot().getChildren().add(item);
    });
    this.tags.forEach(tag -> {
      final var entry = tagTypeItems.get(tag.type().orElse(null));
      entry.getChildren().add(new TreeItem<>(new TagEntry(tag)));
    });
    this.tagsTree.getRoot().getChildren()
        .forEach(entry -> entry.getChildren().sort(Comparator.comparing(TreeItem::toString)));
  }

  public void addTagClickListener(TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public interface TagClickListener {
    void onTagClicked(Tag tag);
  }

  private sealed interface TagsTreeEntry permits DummyEntry, TagTypeEntry, TagEntry {
  }

  private record DummyEntry(String text) implements TagsTreeEntry {
    @Override
    public String toString() {
      return this.text;
    }
  }

  private final class TagTypeEntry implements TagsTreeEntry {
    private final TagType tagType;

    private TagTypeEntry(TagType tagType) {
      this.tagType = tagType;
    }

    public TagType tagType() {
      return this.tagType;
    }

    @Override
    public String toString() {
      return "%s %s (%d)".formatted(
          this.tagType.symbol(),
          this.tagType.label(),
          TagsView.this.tagTypesCounts.get(this.tagType.id())
      );
    }
  }

  private final class TagEntry implements TagsTreeEntry {
    private final Tag tag;

    private TagEntry(Tag tag) {
      this.tag = tag;
    }

    public Tag tag() {
      return this.tag;
    }

    @Override
    public String toString() {
      if (this.tag.definition().isEmpty())
        return "%s (%d)".formatted(
            this.tag.label(),
            TagsView.this.tagsCounts.get(this.tag.id())
        );
      return this.tag.label();
    }
  }

  /**
   * Tree cell class that allows highlighting of tree items matching a query filter.
   * <p>
   * From https://stackoverflow.com/a/34914538/3779986
   */
  private final class SearchHighlightingTreeCell extends TreeCell<TagsTreeEntry> {
    // Cannot be local or else it would be garbage-collected
    @SuppressWarnings("FieldCanBeLocal")
    private final BooleanBinding matchesSearch;

    public SearchHighlightingTreeCell() {
      this.matchesSearch = Bindings.createBooleanBinding(
          () -> TagsView.this.searchMatches.contains(this.getTreeItem()),
          this.treeItemProperty(),
          TagsView.this.searchMatches
      );
      this.matchesSearch.addListener((obs, didMatchSearch, nowMatchesSearch) -> {
        if (nowMatchesSearch)
          this.getStyleClass().add("search-match");
        else if (didMatchSearch)
          this.getStyleClass().remove("search-match");
      });
    }

    @Override
    protected void updateItem(TagsTreeEntry item, boolean empty) {
      // Update the text when the displayed item changes
      super.updateItem(item, empty);
      this.setText(empty ? null : item.toString());
      this.getStyleClass().removeAll("tag-type");
      this.setGraphic(null);
      this.setStyle(null);
      this.setTooltip(null);
      if (item instanceof TagEntry tagEntry && tagEntry.tag().definition().isPresent()) {
        this.setGraphic(App.config().theme().getIcon(Icon.COMPOUND_TAG, Icon.Size.SMALL));
        this.setTooltip(new Tooltip(tagEntry.tag().definition().get()));
      } else if (item instanceof TagTypeEntry tagTypeEntry) {
        this.getStyleClass().add("tag-type");
        this.setStyle("-fx-text-fill: %s;".formatted(StringUtils.colorToCss(tagTypeEntry.tagType().color())));
      } else if (item instanceof DummyEntry)
        this.getStyleClass().add("tag-type");
    }
  }
}
