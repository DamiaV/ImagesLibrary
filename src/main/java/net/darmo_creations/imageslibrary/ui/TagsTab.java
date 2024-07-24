package net.darmo_creations.imageslibrary.ui;

import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A tab that displays a list of tags.
 */
public final class TagsTab extends Tab implements ClickableListCellFactory.ClickListener<TagsTab.TagEntry> {
  public static final DataFormat DRAG_DATA_FORMAT = new DataFormat("application/tag_ids");

  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<TagSelectionListener> tagSelectionListeners = new HashSet<>();
  private final Set<CreateTagListener> createTagListeners = new HashSet<>();
  private final Set<EditTagTypeListener> editTagTypeListeners = new HashSet<>();
  private final Set<DeleteTagTypeListener> deleteTagTypeListeners = new HashSet<>();
  private final Set<CreateTagTypeListener> createTagTypeListeners = new HashSet<>();

  private final Config config;
  @Nullable
  private final TagType tagType;

  private final Label label;
  private final ObservableList<TagEntry> tagEntries = FXCollections.observableArrayList();
  private final FilteredList<TagEntry> filteredList = new FilteredList<>(this.tagEntries, item -> true);
  private final ListView<TagEntry> tagsList = new ListView<>(this.filteredList);

  /**
   * Create a new tab for the given tag type.
   *
   * @param tagType A tag type. May be null.
   */
  public TagsTab(final @NotNull Config config, TagType tagType) {
    super(getTitle(tagType, 0, config.language()));
    this.config = config;
    this.tagType = tagType;

    this.label = new Label(getTitle(tagType, 0, config.language()));
    this.label.getStyleClass().add("tags-tab-title");

    this.getStyleClass().add("tags-tab");
    if (this.tagType != null) {
      final String color = StringUtils.colorToCss(this.tagType.color());
      this.setStyle("-fx-text-base-color: %s;".formatted(color));
      this.label.setStyle("-fx-text-fill: %s;".formatted(color));
    }

    final Language language = config.language();
    final Theme theme = config.theme();

    final HBox top = new HBox(5, this.label);
    top.setPadding(new Insets(5, 2, 5, 5));

    final Button createTagButton = new Button(null, theme.getIcon(Icon.CREATE_TAG, Icon.Size.SMALL));
    createTagButton.setOnAction(event -> this.createTagListeners.forEach(l -> l.onCreateTag(tagType)));
    createTagButton.setTooltip(new Tooltip(language.translate("tags_tab.create_tag")));
    top.getChildren().addAll(new HorizontalSpacer(), createTagButton);

    if (tagType != null) {
      final Button editTagTypeButton = new Button(null, theme.getIcon(Icon.EDIT_TAG_TYPE, Icon.Size.SMALL));
      editTagTypeButton.setOnAction(e -> this.editTagTypeListeners.forEach(l -> l.onEditTagType(tagType)));
      editTagTypeButton.setTooltip(new Tooltip(language.translate("tags_tab.edit")));
      final Button deleteTagTypeButton = new Button(null, theme.getIcon(Icon.DELETE_TAG_TYPE, Icon.Size.SMALL));
      deleteTagTypeButton.setOnAction(e -> this.deleteTagTypeListeners.forEach(l -> l.onDeleteTagType(tagType)));
      deleteTagTypeButton.setTooltip(new Tooltip(language.translate("tags_tab.delete")));
      top.getChildren().addAll(editTagTypeButton, deleteTagTypeButton);
    }

    final Button createTagTypeButton = new Button(null, theme.getIcon(Icon.CREATE_TAG_TYPE, Icon.Size.SMALL));
    createTagTypeButton.setOnAction(e -> this.createTagTypeListeners.forEach(CreateTagTypeListener::onCreateTagType));
    createTagTypeButton.setTooltip(new Tooltip(language.translate("tags_tab.create")));
    top.getChildren().add(createTagTypeButton);

    VBox.setVgrow(this.tagsList, Priority.ALWAYS);
    this.tagsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.tagsList.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onSelectionChange());
    this.tagsList.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue)
        this.onSelectionChange();
    });
    this.tagsList.setCellFactory(param -> ClickableListCellFactory.forListener(this));
    this.filteredList.predicateProperty().addListener((observable, oldValue, newValue) -> this.updateTitle());
    // Allow drag-and-drop of tag items
    this.tagsList.setOnDragDetected(event -> {
      final Dragboard dragboard = this.tagsList.startDragAndDrop(TransferMode.MOVE);
      final var selectedItems = this.tagsList.getSelectionModel().getSelectedItems().stream()
          .map(e -> e.tag().id())
          .toList();
      dragboard.setContent(Map.of(DRAG_DATA_FORMAT, selectedItems));
      event.consume();
    });

    this.setContent(new VBox(top, this.tagsList));
  }

  private void updateTitle() {
    this.setText(getTitle(this.tagType, this.filteredList.size(), this.config.language()));
    this.label.setText(getTitle(this.tagType, this.filteredList.size(), this.config.language()));
  }

  public Optional<TagType> tagType() {
    return Optional.ofNullable(this.tagType);
  }

  /**
   * Set the tag entries this tab should display.
   *
   * @param tags The tag entries to display.
   */
  public void setTags(final @NotNull Set<TagEntry> tags) {
    this.tagEntries.clear();
    this.tagEntries.addAll(tags);
    this.tagEntries.sort(Comparator.comparing(e -> e.tag().label()));
    this.updateTitle();
  }

  /**
   * Set the filter for this list: only tags whose label is matching the filter will be displayed.
   *
   * @param filter A filter.
   */
  public void setFilter(String filter) {
    this.filteredList.setPredicate(item ->
        StringUtils.stripNullable(filter)
            .map(s -> item.tag().label().toLowerCase().contains(s.toLowerCase()))
            .orElse(true));
  }

  /**
   * Deselect all currently selected items.
   */
  public void deselectAll() {
    this.tagsList.getSelectionModel().clearSelection();
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

  @Override
  public void onItemClick(@NotNull TagEntry item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull TagEntry tagEntry) {
    this.tagClickListeners.forEach(listener -> listener.onTagClick(tagEntry.tag()));
  }

  private void onSelectionChange() {
    final var tags = this.tagsList.getSelectionModel()
        .getSelectedItems()
        .stream()
        .map(TagEntry::tag)
        .toList();
    this.tagSelectionListeners.forEach(listener -> listener.onSelectionChanged(tags));
  }

  private static String getTitle(TagType tagType, int tagsCount, final @NotNull Language language) {
    final String title;
    if (tagType == null)
      title = language.translate("tags_tab.no_type");
    else
      title = "%s %s".formatted(tagType.symbol(), tagType.label());
    return "%s (%s)".formatted(title, language.formatNumber(tagsCount));
  }

  public static final class TagEntry extends HBox {
    private final Tag tag;

    public TagEntry(@NotNull Tag tag, int useCount, final @NotNull Config config) {
      this.tag = tag;
      final Label label = new Label();
      if (this.tag.definition().isPresent()) {
        label.setGraphic(config.theme().getIcon(Icon.COMPOUND_TAG, Icon.Size.SMALL));
        label.setTooltip(new Tooltip(this.tag.definition().get()));
        label.setText(this.tag.label());
      } else
        label.setText("%s (%d)".formatted(this.tag.label(), useCount));
      this.getChildren().add(label);
    }

    public Tag tag() {
      return this.tag;
    }
  }
}
