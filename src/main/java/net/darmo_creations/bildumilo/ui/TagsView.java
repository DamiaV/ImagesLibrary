package net.darmo_creations.bildumilo.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;

public class TagsView extends VBox {
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<TagSelectionListener> tagSelectionListeners = new HashSet<>();
  private final Set<CreateTagListener> createTagListeners = new HashSet<>();
  private final Set<EditTagTypeListener> editTagTypeListeners = new HashSet<>();
  private final Set<DeleteTagTypeListener> deleteTagTypeListeners = new HashSet<>();
  private final Set<CreateTagTypeListener> createTagTypeListeners = new HashSet<>();
  private final Set<EditTagsTypeListener> editTagsTypeListeners = new HashSet<>();

  private final Config config;
  private final DatabaseConnection db;

  private final TabPane tabPane = new TabPane();
  private final TextField searchField = new TextField();
  private final Button clearSearchButton = new Button();

  /**
   * Create a new tag tree view.
   *
   * @param config The app’s config.
   * @param db     The database to pull tags from.
   */
  public TagsView(final @NotNull Config config, @NotNull final DatabaseConnection db) {
    this.config = config;
    this.db = db;

    this.setMinWidth(250);

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
    this.tabPane.skinProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null)
        this.hookDragAndDrop();
    });
    this.tabPane.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
      if (isFocused)
        this.onTabSelectionChange();
    });

    this.searchField.setPromptText(language.translate("tag_search_field.search"));
    this.searchField.setStyle("-fx-font-size: 1.5em");
    this.searchField.textProperty().addListener((observable, oldValue, newValue) -> this.onSearchFilterChange(newValue));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText(null);
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.BIG));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);

    this.refresh();
  }

  /**
   * Refresh this view from the internal tags and tag types views.
   */
  public void refresh() {
    final Tab selectedTab = this.tabPane.getSelectionModel().getSelectedItem();
    final Optional<Integer> selectedTagType;
    if (selectedTab != null)
      selectedTagType = ((TagsTab) selectedTab).tagType().map(TagType::id);
    else
      selectedTagType = Optional.empty();

    this.tabPane.getTabs().clear();

    final Map<TagType, TagsTab> tagTypeTabs = new HashMap<>();
    final Map<TagsTab, Set<TagsTab.TagEntry>> tabTags = new HashMap<>();

    // Create a new tab for the given tag type
    final Consumer<TagType> createTab = tagType -> {
      final TagsTab tab = new TagsTab(this.config, tagType);
      this.tabPane.getTabs().add(tab);
      tagTypeTabs.put(tagType, tab);
      tabTags.put(tab, new HashSet<>());
      tab.addTagClickListener(this::onTagClick);
      tab.addTagSelectionListener(this::onTagSelectionChange);
      tab.addCreateTagListener(this::onCreateTag);
      tab.addEditTagTypeListener(this::onEditTagType);
      tab.addDeleteTagTypeListener(this::onDeleteTagType);
      tab.addCreateTagTypeListener(this::onCreateTagType);
      if (selectedTagType.equals(Optional.ofNullable(tagType).map(TagType::id)))
        this.tabPane.getSelectionModel().select(tab);
    };

    createTab.accept(null);
    this.db.getAllTagTypes().stream()
        .sorted()
        .forEach(createTab);
    this.db.getAllTags().forEach(tag -> {
      final var tab = tagTypeTabs.get(tag.type().orElse(null));
      tabTags.get(tab).add(new TagsTab.TagEntry(tag, this.db.getAllTagsCounts().get(tag.id()), this.config));
    });
    tabTags.forEach(TagsTab::setTags);
    this.hookDragAndDrop();
    this.onSearchFilterChange(this.searchField.getText());
  }

  /**
   * Add tag drag-and-drop capabilities to tab headers.
   */
  private void hookDragAndDrop() {
    if (this.tabPane.getSkin() == null)
      return;
    final Pane headerRegion = (Pane) this.tabPane.getSkin().getNode().lookup(".headers-region");
    headerRegion.lookupAll(".tab").forEach(tabHeader -> {
      tabHeader.setOnDragOver(event -> {
        if (event.getDragboard().hasContent(TagsTab.DRAG_DATA_FORMAT))
          event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
      });
      tabHeader.setOnDragDropped(event -> {
        if (event.getDragboard().hasContent(TagsTab.DRAG_DATA_FORMAT)) {
          final TagsTab targetTab = (TagsTab) tabHeader.getProperties().get(Tab.class);
          final Optional<TagType> targetTagType = targetTab.tagType();
          //noinspection unchecked
          final var tags = ((List<Integer>) event.getDragboard().getContent(TagsTab.DRAG_DATA_FORMAT))
              .stream()
              // Remove tags whose type is already the target one
              .flatMap(id -> this.db.getAllTags().stream()
                  .filter(tag -> tag.id() == id && !tag.type().equals(targetTagType))
                  .findFirst()
                  .stream())
              .toList();
          if (!tags.isEmpty())
            this.editTagsTypeListeners.forEach(l -> l.onEditTagsType(tags, targetTagType.orElse(null)));
          event.setDropCompleted(true);
        }
        event.consume();
      });
    });
  }

  /**
   * Select the tab that corresponds to the given tag type, based on its ID.
   *
   * @param tagType A tag type. May be null.
   */
  public void selectTagType(TagType tagType) {
    this.tabPane.getTabs().stream()
        .filter(tab -> ((TagsTab) tab).tagType().map(tt -> tagType != null && tt.id() == tagType.id()).orElse(tagType == null))
        .findFirst()
        .ifPresent(tab -> this.tabPane.getSelectionModel().select(tab));
  }

  public void addTagClickListener(@NotNull TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public void addTagSelectionListener(@NotNull TagSelectionListener listener) {
    this.tagSelectionListeners.add(Objects.requireNonNull(listener));
  }

  public void addCreateTagListener(@NotNull CreateTagListener listener) {
    this.createTagListeners.add(Objects.requireNonNull(listener));
  }

  public void addEditTagTypeListener(@NotNull EditTagTypeListener listener) {
    this.editTagTypeListeners.add(Objects.requireNonNull(listener));
  }

  public void addDeleteTagTypeListener(@NotNull DeleteTagTypeListener listener) {
    this.deleteTagTypeListeners.add(Objects.requireNonNull(listener));
  }

  public void addCreateTagTypeListener(@NotNull CreateTagTypeListener listener) {
    this.createTagTypeListeners.add(Objects.requireNonNull(listener));
  }

  public void addEditTagsTypeListener(@NotNull EditTagsTypeListener listener) {
    this.editTagsTypeListeners.add(Objects.requireNonNull(listener));
  }

  /**
   * Called whenever the search filter changes.
   *
   * @param text The filter text.
   */
  private void onSearchFilterChange(@NotNull String text) {
    final var filter = StringUtils.stripNullable(text);
    this.clearSearchButton.setDisable(filter.isEmpty());
    this.tabPane.getTabs()
        .forEach(tab -> ((TagsTab) tab).setFilter(filter.orElse(null)));
  }

  private void onTabSelectionChange() {
    this.tabPane.getTabs().forEach(tab -> ((TagsTab) tab).deselectAll());
  }

  private void onTagClick(@NotNull Tag tag) {
    this.tagClickListeners.forEach(listener -> listener.onTagClick(tag));
  }

  private void onTagSelectionChange(final @NotNull List<Tag> tags) {
    this.tagSelectionListeners.forEach(listener -> listener.onSelectionChanged(tags));
  }

  private void onCreateTag(TagType tagType) {
    this.createTagListeners.forEach(l -> l.onCreateTag(tagType));
  }

  private void onEditTagType(@NotNull TagType tagType) {
    this.editTagTypeListeners.forEach(listener -> listener.onEditTagType(tagType));
  }

  private void onDeleteTagType(@NotNull TagType tagType) {
    this.deleteTagTypeListeners.forEach(listener -> listener.onDeleteTagType(tagType));
  }

  private void onCreateTagType() {
    this.createTagTypeListeners.forEach(CreateTagTypeListener::onCreateTagType);
  }
}
