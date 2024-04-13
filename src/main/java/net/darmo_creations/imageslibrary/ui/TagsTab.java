package net.darmo_creations.imageslibrary.ui;

import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.geometry.*;
import javafx.scene.control.*;
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
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<TagSelectionListener> tagSelectionListeners = new HashSet<>();
  private final Set<EditTagTypeListener> editTagTypeListeners = new HashSet<>();
  private final Set<DeleteTagTypeListener> deleteTagTypeListeners = new HashSet<>();

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
  public TagsTab(final Config config, @Nullable TagType tagType) {
    super(getTitle(tagType, 0, config.language()));
    this.config = config;
    this.tagType = tagType;

    this.label = new Label(getTitle(tagType, 0, config.language()));
    this.label.getStyleClass().add("tags-tab-title");

    this.getStyleClass().add("tags-tab");
    if (this.tagType != null) {
      String color = StringUtils.colorToCss(this.tagType.color());
      this.setStyle("-fx-text-base-color: %s;".formatted(color));
      this.label.setStyle("-fx-text-fill: %s;".formatted(color));
    }

    final HBox top = new HBox(5, this.label);
    top.setPadding(new Insets(5, 2, 5, 5));
    if (tagType != null) {
      final Language language = config.language();
      final Theme theme = config.theme();
      final Button editTagTypeButton = new Button(null, theme.getIcon(Icon.EDIT_TAG_TYPE, Icon.Size.SMALL));
      editTagTypeButton.setOnAction(e -> this.editTagTypeListeners.forEach(l -> l.onEditTagType(tagType)));
      editTagTypeButton.setTooltip(new Tooltip(language.translate("tags_tab.edit")));
      final Button deleteTagTypeButton = new Button(null, theme.getIcon(Icon.DELETE_TAG_TYPE, Icon.Size.SMALL));
      deleteTagTypeButton.setOnAction(e -> this.deleteTagTypeListeners.forEach(l -> l.onDeleteTagType(tagType)));
      deleteTagTypeButton.setTooltip(new Tooltip(language.translate("tags_tab.delete")));
      final Pane spacer = new Pane();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      top.getChildren().addAll(spacer, editTagTypeButton, deleteTagTypeButton);
    }

    VBox.setVgrow(this.tagsList, Priority.ALWAYS);
    this.tagsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    this.tagsList.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.onSelectionChange());
    this.tagsList.focusedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue)
        this.onSelectionChange();
    });
    this.tagsList.setCellFactory(param -> ClickableListCellFactory.forListener(this));
    this.filteredList.predicateProperty().addListener((observable, oldValue, newValue) -> {
      this.setText(getTitle(tagType, this.filteredList.size(), config.language()));
      this.label.setText(getTitle(tagType, this.filteredList.size(), config.language()));
    });

    this.setContent(new VBox(top, this.tagsList));
  }

  /**
   * Set the tag entries this tab should display.
   *
   * @param tags The tag entries to display.
   */
  public void setTags(final Set<TagEntry> tags) {
    this.tagEntries.clear();
    this.tagEntries.addAll(tags);
    this.tagEntries.sort(Comparator.comparing(e -> e.tag().label()));
    final String title = getTitle(this.tagType, this.tagEntries.size(), this.config.language());
    this.setText(title);
    this.label.setText(title);
  }

  /**
   * Set the filter for this list: only tags whose label is matching the filter will be displayed.
   *
   * @param filter A filter.
   */
  public void setFilter(@Nullable String filter) {
    this.filteredList.setPredicate(item ->
        StringUtils.stripNullable(filter)
            .map(s -> item.tag().label().contains(s.toLowerCase()))
            .orElse(true));
  }

  /**
   * Deselect all currently selected items.
   */
  public void deselectAll() {
    this.tagsList.getSelectionModel().clearSelection();
  }

  public void addTagClickListener(TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public void addTagSelectionListener(TagSelectionListener listener) {
    this.tagSelectionListeners.add(Objects.requireNonNull(listener));
  }

  public void addEditTagTypeListener(EditTagTypeListener listener) {
    this.editTagTypeListeners.add(Objects.requireNonNull(listener));
  }

  public void addDeleteTagTypeListener(DeleteTagTypeListener listener) {
    this.deleteTagTypeListeners.add(Objects.requireNonNull(listener));
  }

  @Override
  public void onItemClick(TagEntry item) {
  }

  @Override
  public void onItemDoubleClick(TagEntry tagEntry) {
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

  private static String getTitle(@Nullable TagType tagType, int tagsCount, final Language language) {
    final String title;
    if (tagType == null)
      title = language.translate("tags_tab.no_type");
    else
      title = "%s %s".formatted(tagType.symbol(), tagType.label());
    return "%s (%s)".formatted(title, language.formatNumber(tagsCount));
  }

  public static final class TagEntry extends HBox {
    private final Tag tag;

    public TagEntry(Tag tag, int useCount, final Config config) {
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
