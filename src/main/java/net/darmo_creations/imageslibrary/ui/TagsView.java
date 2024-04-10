package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;

import java.util.*;
import java.util.function.*;

public class TagsView extends VBox { // TODO option to create/edit/delete tags and tag types from here
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<TagSelectionListener> tagSelectionListeners = new HashSet<>();

  private final Set<Tag> tags;
  private final Map<Integer, Integer> tagsCounts;
  private final Set<TagType> tagTypes;

  private final TabPane tabPane = new TabPane();
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
      final Set<TagType> tagTypes
  ) {
    this.tags = Objects.requireNonNull(tags);
    this.tagsCounts = Objects.requireNonNull(tagsCounts);
    this.tagTypes = Objects.requireNonNull(tagTypes);

    final Config config = App.config();
    final Language language = config.language();
    final Theme theme = config.theme();

    VBox.setVgrow(this.tabPane, Priority.ALWAYS);
    HBox.setHgrow(this.searchField, Priority.ALWAYS);
    final HBox searchBox = new HBox(
        5,
        this.searchField,
        this.clearSearchButton
    );
    searchBox.setPadding(new Insets(2));
    this.getChildren().addAll(searchBox, this.tabPane);

    this.tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
    this.tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    this.tabPane.setSide(Side.LEFT);
    this.tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
        this.onTabSelectionChange());

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
   * Refresh this view from the internal tags and tag types views.
   */
  public void refresh() {
    // TODO allow drag and drop of tag items between tabs to change their type
    this.tabPane.getTabs().clear();

    final Map<TagType, TagsTab> tagTypeTabs = new HashMap<>();
    final Map<TagsTab, Set<TagsTab.TagEntry>> tabTags = new HashMap<>();

    // Create a new tab for the given tag type
    final Consumer<TagType> createTab = tagType -> {
      final TagsTab tab = new TagsTab(tagType);
      this.tabPane.getTabs().add(tab);
      tagTypeTabs.put(tagType, tab);
      tabTags.put(tab, new HashSet<>());
      tab.addTagClickListener(this::onTagClick);
      tab.addTagSelectionListener(this::onTagSelectionChange);
    };

    createTab.accept(null);
    this.tagTypes.stream()
        .sorted(Comparator.comparing(TagType::label))
        .forEach(createTab);
    this.tags.forEach(tag -> {
      final var tab = tagTypeTabs.get(tag.type().orElse(null));
      tabTags.get(tab).add(new TagsTab.TagEntry(tag, this.tagsCounts.get(tag.id())));
    });
    tabTags.forEach(TagsTab::setTags);
  }

  public void addTagClickListener(TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public void addTagSelectionListener(TagSelectionListener listener) {
    this.tagSelectionListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Called whenever the search filter changes.
   *
   * @param text The filter text.
   */
  private void onSearchFilterChange(String text) {
    final var filter = StringUtils.stripNullable(text);
    this.clearSearchButton.setDisable(filter.isEmpty());
    this.tabPane.getTabs()
        .forEach(tab -> ((TagsTab) tab).setFilter(filter.orElse(null)));
  }

  private void onTabSelectionChange() {
    this.tabPane.getTabs().forEach(tab -> ((TagsTab) tab).deselectAll());
  }

  private void onTagClick(Tag tag) {
    this.tagClickListeners.forEach(listener -> listener.onTagClicked(tag));
  }

  private void onTagSelectionChange(final List<Tag> tags) {
    this.tagSelectionListeners.forEach(listener -> listener.onSelectionChanged(tags));
  }
}
