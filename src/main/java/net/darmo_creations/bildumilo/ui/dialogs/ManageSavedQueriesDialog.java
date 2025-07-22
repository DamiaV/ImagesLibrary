package net.darmo_creations.bildumilo.ui.dialogs;

import javafx.beans.property.*;
import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.converter.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.ui.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This dialog manages saved queries (creation, update, deletion).
 */
public class ManageSavedQueriesDialog extends DialogBase<Void> {
  private final Button moveUpButton = new Button();
  private final Button moveDownButton = new Button();
  private final Button deleteButton = new Button();
  private final TableView<QueryEntry> tableView = new TableView<>();
  private final Button applyButton;

  private final SavedQueriesManager queriesManager;
  private boolean changes;

  public ManageSavedQueriesDialog(final @NotNull Config config, @NotNull SavedQueriesManager queriesManager) {
    super(config, "manage_saved_queries", true, ButtonTypes.CANCEL, ButtonTypes.APPLY, ButtonTypes.OK);
    this.queriesManager = queriesManager;
    this.removeGlobalEventFilter();

    final Language language = config.language();
    final Theme theme = config.theme();

    this.moveUpButton.setGraphic(theme.getIcon(Icon.MOVE_UP, Icon.Size.SMALL));
    this.moveUpButton.setTooltip(new Tooltip(language.translate("dialog.manage_saved_queries.move_up")));
    this.moveUpButton.setOnAction(event -> this.moveUpSelectedItem());

    this.moveDownButton.setGraphic(theme.getIcon(Icon.MOVE_DOWN, Icon.Size.SMALL));
    this.moveDownButton.setTooltip(new Tooltip(language.translate("dialog.manage_saved_queries.move_down")));
    this.moveDownButton.setOnAction(event -> this.moveDownSelectedItem());

    this.deleteButton.setGraphic(theme.getIcon(Icon.DELETE_QUERIES, Icon.Size.SMALL));
    this.deleteButton.setTooltip(new Tooltip(language.translate("dialog.manage_saved_queries.delete")));
    this.deleteButton.setOnAction(event -> this.deletedSelectedItems());

    this.tableView.setEditable(true);
    this.tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    final TableColumn<QueryEntry, String> nameCol = new NonSortableTableColumn<>(
        language.translate("dialog.manage_saved_queries.query_name"));
    nameCol.setEditable(true);
    nameCol.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
    nameCol.setCellValueFactory(param -> param.getValue().nameProperty());
    this.tableView.getColumns().add(nameCol);

    final TableColumn<QueryEntry, String> queryCol = new NonSortableTableColumn<>(
        language.translate("dialog.manage_saved_queries.query"));
    queryCol.setEditable(true);
    queryCol.setCellFactory(param -> new TextFieldTableCell<>(new DefaultStringConverter()));
    queryCol.setCellValueFactory(param -> param.getValue().queryProperty());
    this.tableView.getColumns().add(queryCol);

    this.tableView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> this.updateButtons());

    VBox.setVgrow(this.tableView, Priority.ALWAYS);

    this.getDialogPane().setContent(new VBox(
        5,
        new HBox(5, new HorizontalSpacer(), this.moveUpButton, this.moveDownButton, this.deleteButton),
        this.tableView
    ));

    this.applyButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.APPLY);
    this.applyButton.addEventFilter(ActionEvent.ACTION, event -> {
      this.applyChanges();
      event.consume();
    });

    final Stage stage = this.stage();
    stage.setMinWidth(500);
    stage.setMinHeight(400);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton())
        this.applyChanges();
      return null;
    });

    this.setOnShowing(event -> {
      final var items = this.tableView.getItems();
      items.clear();
      queriesManager.entries()
          .forEach(e -> items.add(new QueryEntry(e.name(), e.query())));
      this.changes = false;
      this.updateButtons();
    });

    this.updateButtons();
  }

  private void moveUpSelectedItem() {
    this.changes = true;
    final QueryEntry selectedItem = this.tableView.getSelectionModel().getSelectedItem();
    final int i = this.tableView.getItems().indexOf(selectedItem);
    this.tableView.getItems().remove(selectedItem);
    this.tableView.getItems().add(i - 1, selectedItem);
    this.tableView.getSelectionModel().clearSelection();
    this.tableView.getSelectionModel().select(selectedItem);
    this.tableView.requestFocus();
  }

  private void moveDownSelectedItem() {
    this.changes = true;
    final QueryEntry selectedItem = this.tableView.getSelectionModel().getSelectedItem();
    final int i = this.tableView.getItems().indexOf(selectedItem);
    this.tableView.getItems().remove(selectedItem);
    this.tableView.getItems().add(i + 1, selectedItem);
    this.tableView.getSelectionModel().clearSelection();
    this.tableView.getSelectionModel().select(selectedItem);
    this.tableView.requestFocus();
  }

  private void deletedSelectedItems() {
    final boolean proceed = Alerts.confirmation(
        this.config,
        "alert.delete_queries.header",
        null,
        null
    );
    if (!proceed)
      return;
    this.changes = true;
    this.tableView.getItems().removeAll(this.tableView.getSelectionModel().getSelectedItems());
  }

  private void updateButtons() {
    final var selectedItems = this.tableView.getSelectionModel().getSelectedItems();
    final boolean noSelection = selectedItems.isEmpty();
    final boolean notSingleSelection = selectedItems.size() != 1;
    final boolean invalid = this.isDataInvalid();
    this.moveUpButton.setDisable(
        notSingleSelection || this.tableView.getItems().indexOf(selectedItems.get(0)) == 0);
    this.moveDownButton.setDisable(
        notSingleSelection || this.tableView.getItems().indexOf(selectedItems.get(0)) == this.tableView.getItems().size() - 1);
    this.deleteButton.setDisable(noSelection);
    this.applyButton.setDisable(invalid || !this.changes);
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(invalid);
  }

  private boolean isDataInvalid() {
    final Set<String> names = new HashSet<>();
    final Set<String> queries = new HashSet<>();
    boolean invalid = false;

    for (final QueryEntry item : this.tableView.getItems()) {
      final var name = item.getName();
      final var query = item.getQuery();
      if (name.isPresent()) {
        if (names.contains(name.get())) {
          invalid = true;
          break;
        } else
          names.add(name.get());
      } else {
        invalid = true;
        break;
      }
      if (query.isPresent()) {
        if (queries.contains(query.get())) {
          invalid = true;
          break;
        } else
          queries.add(query.get());
      } else {
        invalid = true;
        break;
      }
    }
    return invalid;
  }

  private void applyChanges() {
    this.queriesManager.startTransaction();
    try {
      this.queriesManager.clear();
      for (final QueryEntry query : this.tableView.getItems())
        //noinspection OptionalGetWithoutIsPresent
        this.queriesManager.saveQuery(query.getName().get(), query.getQuery().get());
      this.queriesManager.commit();
      this.changes = false;
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      this.queriesManager.rollback();
    }
    this.updateButtons();
  }

  private class QueryEntry {
    private final StringProperty nameProperty = new SimpleStringProperty();
    private final StringProperty queryProperty = new SimpleStringProperty();

    public QueryEntry(String name, String query) {
      this.nameProperty.set(name);
      this.nameProperty.addListener((observable, oldValue, newValue) -> {
        ManageSavedQueriesDialog.this.changes = true;
        ManageSavedQueriesDialog.this.updateButtons();
      });
      this.queryProperty.set(query);
      this.queryProperty.addListener((observable, oldValue, newValue) -> {
        ManageSavedQueriesDialog.this.changes = true;
        ManageSavedQueriesDialog.this.updateButtons();
      });
    }

    public Optional<String> getName() {
      return StringUtils.stripNullable(this.nameProperty.get());
    }

    public StringProperty nameProperty() {
      return this.nameProperty;
    }

    public Optional<String> getQuery() {
      return StringUtils.stripNullable(this.queryProperty.get());
    }

    public StringProperty queryProperty() {
      return this.queryProperty;
    }
  }

  private static class NonSortableTableColumn<S, T> extends TableColumn<S, T> {
    public NonSortableTableColumn(String text) {
      super(text);
      this.setSortable(false);
      this.setReorderable(false);
    }
  }
}
