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

public class TagsView extends VBox { // TODO button to open tags edit dialog from here?
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();

  private final Set<Tag> tags;
  private final Set<TagType> tagTypes;

  private final TreeView<TagsTreeEntry> tagsTree = new TreeView<>();
  private final TextField searchField = new TextField();
  private final Button clearSearchButton = new Button();

  /**
   * Create a new tag tree view.
   *
   * @param tags     A view to the available tags.
   * @param tagTypes A view to the available tag types.
   */
  public TagsView(final Set<Tag> tags, final Set<TagType> tagTypes) {
    this.tags = Objects.requireNonNull(tags);
    this.tagTypes = Objects.requireNonNull(tagTypes);

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
    this.searchField.textProperty().addListener((observable, oldValue, newValue) ->
        this.clearSearchButton.setDisable(StringUtils.stripNullable(newValue).isEmpty()));

    this.clearSearchButton.setOnAction(e -> {
      this.searchField.setText(null);
      this.searchField.requestFocus();
    });
    this.clearSearchButton.setGraphic(theme.getIcon(Icon.CLEAR_TEXT, Icon.Size.SMALL));
    this.clearSearchButton.setTooltip(new Tooltip(language.translate("search_field.erase_search")));
    this.clearSearchButton.setDisable(true);

    this.refresh();
  }

  public void refresh() {
    // TODO build tree
  }

  public void addTagClickListener(TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public interface TagClickListener {
    void onTagClicked(Tag tag);
  }

  private interface TagsTreeEntry {
  }

  private record TagTypeEntry(TagType tagType) implements TagsTreeEntry {
    @Override
    public String toString() {
      return "%s (%s)".formatted(this.tagType.label(), this.tagType.symbol());
    }
  }

  private record TagEntry(Tag tag) implements TagsTreeEntry {
    @Override
    public String toString() {
      return this.tag.label();
    }
  }

//  /**
//   * Tree cell class that allows highlighting of tree items matching a query filter.
//   * <p>
//   * From https://stackoverflow.com/a/34914538/3779986
//   */
//  private class SearchHighlightingTreeCell extends TreeCell<Object> {
//    // Cannot be local or else it would be garbage-collected
//    @SuppressWarnings("FieldCanBeLocal")
//    private final BooleanBinding matchesSearch;
//
//    public SearchHighlightingTreeCell() {
//      this.matchesSearch = Bindings.createBooleanBinding(
//          () -> FamilyTreeView.this.searchMatches.contains(this.getTreeItem()),
//          this.treeItemProperty(),
//          FamilyTreeView.this.searchMatches
//      );
//      this.matchesSearch.addListener((obs, didMatchSearch, nowMatchesSearch) ->
//          this.pseudoClassStateChanged(PseudoClasses.SEARCH_MATCH, nowMatchesSearch));
//    }
//
//    @Override
//    protected void updateItem(Object item, boolean empty) {
//      // Update the text when the displayed item changes
//      super.updateItem(item, empty);
//      this.setText(empty ? null : item.toString());
//      Optional<FamilyTree> familyTree = FamilyTreeView.this.familyTree();
//      if (familyTree.isPresent() && item instanceof Person p && familyTree.get().isRoot(p)) {
//        this.setGraphic(App.config().theme().getIcon(Icon.TREE_ROOT, Icon.Size.SMALL));
//      } else {
//        this.setGraphic(null);
//      }
//    }
//  }
}
