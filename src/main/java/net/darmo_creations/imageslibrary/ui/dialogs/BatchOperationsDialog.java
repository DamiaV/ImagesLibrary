package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.application.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.dialogs.operation_views.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

/**
 * This dialog allows applying batches of operations to many media files at once.
 */
public class BatchOperationsDialog extends DialogBase<Boolean>
    implements OperationView.OperationUpdateListener, ClickableListCellFactory.ClickListener<String> {
  private final RadioButton applyToSelectedRadio = new RadioButton();
  private final RadioButton applyToResultsRadio = new RadioButton();
  private final RadioButton applyToAllRadio = new RadioButton();
  private final Label operationsLabel = new Label();
  private final Button newOperationBatchButton = new Button();
  private final Button saveOperationBatchButton = new Button();
  private final Button loadOperationBatchButton = new Button();
  private final Button deleteOperationBatchButton = new Button();
  private final Button renameOperationBatchButton = new Button();
  private final Button applyButton;
  private final ListView<OperationView<? extends Operation>> operationBatchList = new ListView<>();
  private final ListView<String> savedOperationBatchesList = new ListView<>();

  private final ProgressDialog progressDialog;

  private final DatabaseConnection db;
  private final List<MediaFile> resultMediaFiles = new ArrayList<>();
  private final List<MediaFile> selectedMediaFiles = new ArrayList<>();
  private boolean anyMediaUpdate;
  private boolean unsavedChanges;
  private String loadedOperationBatchName;

  private final BatchOperationsManager batchOperationsManager;
  // Avoids creating multiple view instances
  @UnmodifiableView
  private final Map<String, List<? extends Operation>> operationBatchesCache;

  public BatchOperationsDialog(
      @NotNull Config config,
      @NotNull DatabaseConnection db,
      @NotNull BatchOperationsManager batchOperationsManager
  ) {
    super(config, "batch_operations", true, ButtonTypes.CLOSE, ButtonTypes.APPLY);
    this.db = Objects.requireNonNull(db);
    this.batchOperationsManager = Objects.requireNonNull(batchOperationsManager);
    this.operationBatchesCache = batchOperationsManager.entries();

    this.progressDialog = new ProgressDialog(config, "executing_operations");

    this.applyButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.APPLY);
    this.applyButton.addEventFilter(ActionEvent.ACTION, event -> {
      this.runOperationBatch();
      event.consume();
    });

    final Language language = config.language();
    final Theme theme = config.theme();

    final ToggleGroup toggleGroup = new ToggleGroup();
    this.applyToSelectedRadio.setToggleGroup(toggleGroup);
    this.applyToResultsRadio.setToggleGroup(toggleGroup);
    this.applyToAllRadio.setToggleGroup(toggleGroup);

    this.applyToSelectedRadio.setText(language.translate("dialog.batch_operations.apply_to_selected"));
    this.applyToResultsRadio.setText(language.translate("dialog.batch_operations.apply_to_results"));
    this.applyToAllRadio.setText(language.translate("dialog.batch_operations.apply_to_all"));

    final MenuButton addOperationButton = new MenuButton();
    addOperationButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.add_operation.tooltip")));
    addOperationButton.setGraphic(theme.getIcon(Icon.ADD_OPERATION, Icon.Size.SMALL));
    for (final OperationType operationType : OperationType.values()) {
      final MenuItem item = new MenuItem(
          language.translate("operation_view." + operationType.name().toLowerCase())
      );
      item.setOnAction(event -> this.addOperation(operationType));
      addOperationButton.getItems().add(item);
    }

    this.newOperationBatchButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.new_operation_batch.tooltip")));
    this.newOperationBatchButton.setGraphic(theme.getIcon(Icon.NEW_OPERATION_BATCH, Icon.Size.SMALL));
    this.newOperationBatchButton.setOnAction(event -> this.newOperationBatch());

    this.saveOperationBatchButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.save_operation_batch.tooltip")));
    this.saveOperationBatchButton.setGraphic(theme.getIcon(Icon.SAVE_OPERATION_BATCH, Icon.Size.SMALL));
    this.saveOperationBatchButton.setOnAction(event -> this.saveOperationBatch());

    this.loadOperationBatchButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.load_operation_batch.tooltip")));
    this.loadOperationBatchButton.setGraphic(theme.getIcon(Icon.LOAD_OPERATION_BATCH, Icon.Size.SMALL));
    this.loadOperationBatchButton.setOnAction(event -> this.loadSelectedOperationBatch());

    this.deleteOperationBatchButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.delete_operation_batch.tooltip")));
    this.deleteOperationBatchButton.setGraphic(theme.getIcon(Icon.DELETE_OPERATION_BATCH, Icon.Size.SMALL));
    this.deleteOperationBatchButton.setOnAction(event -> this.onDeleteOperationBatch());

    this.renameOperationBatchButton.setTooltip(new Tooltip(language.translate("dialog.batch_operations.rename_operation_batch.tooltip")));
    this.renameOperationBatchButton.setGraphic(theme.getIcon(Icon.RENAME_OPERATION_BATCH, Icon.Size.SMALL));
    this.renameOperationBatchButton.setOnAction(event -> this.onRenameOperationBatch());

    this.operationBatchList.setPrefHeight(200);
    VBox.setVgrow(this.operationBatchList, Priority.ALWAYS);
    this.savedOperationBatchesList.setCellFactory(param -> ClickableListCellFactory.forListener(this));
    this.savedOperationBatchesList.setPrefHeight(200);
    this.savedOperationBatchesList.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> this.updateButtons());
    VBox.setVgrow(this.savedOperationBatchesList, Priority.ALWAYS);

    final HBox operationsBox = new HBox(
        5,
        this.operationsLabel,
        new HorizontalSpacer(),
        this.newOperationBatchButton,
        this.saveOperationBatchButton,
        addOperationButton
    );
    operationsBox.setAlignment(Pos.CENTER_LEFT);

    final HBox savedOperationsBox = new HBox(
        5,
        new Label(language.translate("dialog.batch_operations.saved_operation_batches")),
        new HorizontalSpacer(),
        this.deleteOperationBatchButton,
        this.renameOperationBatchButton,
        this.loadOperationBatchButton
    );
    savedOperationsBox.setAlignment(Pos.CENTER_LEFT);

    final SplitPane splitPane = new SplitPane(
        new VBox(
            5,
            operationsBox,
            this.operationBatchList
        ),
        new VBox(
            5,
            savedOperationsBox,
            this.savedOperationBatchesList
        )
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    VBox.setVgrow(splitPane, Priority.ALWAYS);

    this.getDialogPane().setContent(new VBox(
        5,
        new Label(language.translate("dialog.batch_operations.select_target_pictures")),
        this.applyToSelectedRadio,
        this.applyToResultsRadio,
        this.applyToAllRadio,
        splitPane
    ));

    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);

    this.setResultConverter(buttonType -> this.anyMediaUpdate);

    this.setOnCloseRequest(event -> {
      JavaFxUtils.checkNoOngoingTask(config, event, this.progressDialog);
      if (!event.isConsumed() && this.saveFailedOrIgnored())
        event.consume();
    });

    this.setCurrentBatchName(null);
    this.updateButtons();
  }

  /**
   * Set medias to potentially apply operations on.
   *
   * @param resultMediaFiles   Medias the current query returned.
   * @param selectedMediaFiles Medias currently selected.
   */
  public void setMedias(@NotNull List<MediaFile> resultMediaFiles, @NotNull List<MediaFile> selectedMediaFiles) {
    this.resultMediaFiles.clear();
    this.resultMediaFiles.addAll(resultMediaFiles);
    this.selectedMediaFiles.clear();
    this.selectedMediaFiles.addAll(selectedMediaFiles);

    this.applyToSelectedRadio.setDisable(selectedMediaFiles.isEmpty());
    this.applyToResultsRadio.setDisable(resultMediaFiles.isEmpty());
    if (!this.applyToSelectedRadio.isDisabled())
      this.applyToSelectedRadio.setSelected(true);
    else if (!this.applyToResultsRadio.isDisabled())
      this.applyToResultsRadio.setSelected(true);
    else
      this.applyToAllRadio.setSelected(true);

    this.operationBatchList.getItems().clear();
    this.savedOperationBatchesList.getItems().clear();
    this.operationBatchesCache.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().toLowerCase()))
        .forEach(e -> {
          e.getValue().forEach(op -> op.condition().ifPresent(Condition::purgeCaches));
          this.savedOperationBatchesList.getItems().add(e.getKey());
        });

    this.anyMediaUpdate = false;
    this.unsavedChanges = false;
    this.setCurrentBatchName(null);
    this.updateButtons();
  }

  private void newOperationBatch() {
    if (this.saveFailedOrIgnored())
      return;

    this.setCurrentBatchName(null);
    this.operationBatchList.getItems().clear();
    this.unsavedChanges = false;
    this.updateButtons();
  }

  private boolean saveOperationBatch() {
    final Optional<String> name;
    if (this.loadedOperationBatchName != null)
      name = Optional.of(this.loadedOperationBatchName);
    else
      name = this.promptBatchName();
    final boolean save = name.isPresent();
    if (save)
      try {
        this.saveBatch(name.get());
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
        return false;
      }
    return save;
  }

  private void saveBatch(@NotNull String name) throws DatabaseOperationException {
    final var operations = this.operationBatchList.getItems().stream()
        .map(OperationView::getOperation)
        .toList();
    this.batchOperationsManager.saveOperationBatch(name, operations);
    if (!this.savedOperationBatchesList.getItems().contains(name)) {
      this.savedOperationBatchesList.getItems().add(name);
      this.savedOperationBatchesList.scrollTo(this.savedOperationBatchesList.getItems().size() - 1);
    }
    if (!Objects.equals(this.loadedOperationBatchName, name))
      this.setCurrentBatchName(name);
    this.unsavedChanges = false;
    this.updateButtons();
  }

  private void loadSelectedOperationBatch() {
    if (this.saveFailedOrIgnored())
      return;

    final String selectedItem = this.savedOperationBatchesList.getSelectionModel().getSelectedItem();
    if (selectedItem == null)
      return;

    this.operationBatchList.getItems().clear();
    this.operationBatchesCache.get(selectedItem).forEach(operation -> {
      final OperationView<? extends Operation> operationView;
      if (operation instanceof UpdateTagsOperation o)
        operationView = new UpdateTagsOperationView(o, false, this.db, this.config);
      else if (operation instanceof MoveOperation o)
        operationView = new MoveOperationView(o, false, this.db, this.config);
      else if (operation instanceof DeleteOperation o)
        operationView = new DeleteOperationView(o, false, this.db, this.config);
      else if (operation instanceof RecomputeHashOperation o)
        operationView = new RecomputeHashOperationView(o, false, this.db, this.config);
      else if (operation instanceof TransformPathOperation o)
        operationView = new TransformPathOperationView(o, false, this.db, this.config);
      else
        throw new IllegalArgumentException("Unknown operation type: " + operation.getClass().getSimpleName());
      operationView.addUpdateListener(this);
      this.operationBatchList.getItems().add(operationView);
    });
    this.setCurrentBatchName(selectedItem);
    this.unsavedChanges = false;
    this.updateButtons();
  }

  private void onDeleteOperationBatch() {
    final String selectedItem = this.savedOperationBatchesList.getSelectionModel().getSelectedItem();
    if (selectedItem == null)
      return;

    final boolean proceed = Alerts.confirmation(
        this.config,
        "alert.confirm_delete_operation_batch.header",
        null,
        null,
        new FormatArg("name", selectedItem)
    );
    if (!proceed)
      return;

    try {
      this.batchOperationsManager.deleteOperationBatch(selectedItem);
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return;
    }

    this.savedOperationBatchesList.getItems().remove(selectedItem);
    if (Objects.equals(this.loadedOperationBatchName, selectedItem))
      this.unsavedChanges = true;
    this.updateButtons();
  }

  private void onRenameOperationBatch() {
    final String selectedItem = this.savedOperationBatchesList.getSelectionModel().getSelectedItem();
    if (selectedItem == null)
      return;

    Optional<String> newName = Optional.empty();
    boolean ok = false;
    do {
      newName = Alerts.textInput(
          this.config,
          "alert.rename_operation_batch.header",
          "alert.rename_operation_batch.label",
          "alert.rename_operation_batch.title",
          newName.orElse(selectedItem),
          null,
          new FormatArg("name", selectedItem)
      );
      if (newName.isEmpty())
        return;
      if (newName.get().equals(selectedItem))
        Alerts.warning(
            this.config,
            "alert.rename_operation_batch_conflict.header",
            null,
            null,
            new FormatArg("name", selectedItem)
        );
      else
        ok = true;
    } while (!ok);

    try {
      this.batchOperationsManager.renameOperationBatch(selectedItem, newName.get());
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return;
    }

    final int i = this.savedOperationBatchesList.getItems().indexOf(selectedItem);
    if (i != -1)
      this.savedOperationBatchesList.getItems().set(i, newName.get());
    if (Objects.equals(this.loadedOperationBatchName, selectedItem))
      this.setCurrentBatchName(newName.get());
    this.updateButtons();
  }

  private void addOperation(@NotNull OperationType operationType) {
    final OperationView<?> view = switch (operationType) {
      case UPDATE_TAGS -> new UpdateTagsOperationView(null, true, this.db, this.config);
      case MOVE -> new MoveOperationView(null, true, this.db, this.config);
      case DELETE -> new DeleteOperationView(null, true, this.db, this.config);
      case RECOMPUTE_HASH -> new RecomputeHashOperationView(null, true, this.db, this.config);
      case TRANSFORM_PATH -> new TransformPathOperationView(null, true, this.db, this.config);
    };
    view.addUpdateListener(this);
    this.operationBatchList.getItems().add(view);
    this.operationBatchList.scrollTo(view);
    this.unsavedChanges = true;
    this.updateButtons();
  }

  private void runOperationBatch() {
    this.progressDialog.show();
    this.disableInteractions();
    new Thread(new OperationsRunner(), "Batch Operation Thread").start();
  }

  @Override
  public void onOperationUpdate(@NotNull OperationView<?> source) {
    this.unsavedChanges = true;
    this.updateButtons();
  }

  @Override
  public void onOperationDelete(@NotNull OperationView<?> source) {
    this.operationBatchList.getItems().remove(source);
    this.unsavedChanges = true;
    this.updateButtons();
  }

  @Override
  public void onOperationMoveUp(@NotNull OperationView<?> source) {
    final int i = this.operationBatchList.getItems().indexOf(source);
    this.operationBatchList.getItems().remove(source);
    this.operationBatchList.getItems().add(i - 1, source);
    this.unsavedChanges = true;
    this.updateButtons();
  }

  @Override
  public void onOperationMoveDown(@NotNull OperationView<?> source) {
    final int i = this.operationBatchList.getItems().indexOf(source);
    this.operationBatchList.getItems().remove(source);
    this.operationBatchList.getItems().add(i + 1, source);
    this.unsavedChanges = true;
    this.updateButtons();
  }

  private void setCurrentBatchName(String name) {
    this.loadedOperationBatchName = name;
    if (name == null)
      this.operationsLabel.setText(this.config.language().translate("dialog.batch_operations.operations"));
    else
      this.operationsLabel.setText(this.config.language().translate(
          "dialog.batch_operations.operations_with_name",
          new FormatArg("name", name)
      ));
  }

  /**
   * Attempt to save the current operation batch and indicated whether the user cancelled it or it failed.
   *
   * @return True if the save failed or the user cancelled it;
   * false if the user decided not to save or there was nothing to save.
   */
  private boolean saveFailedOrIgnored() {
    if (this.unsavedChanges) {
      if (this.saveOperationBatchButton.isDisable()) {
        // Cannot save, just ask if we should proceed and lose all changes
        return !Alerts.confirmation(
            this.config,
            "alert.unsaved_operation_batch_no_save.header",
            null,
            null
        );
      } else {
        // Can save, ask if we should save, proceed without saving or cancel
        final Optional<Boolean> save = Alerts.confirmationWithCancel(
            this.config,
            "alert.unsaved_operation_batch.header",
            null,
            null
        );
        return save.isEmpty() || save.get() && !this.saveOperationBatch();
      }
    }
    return false;
  }

  /**
   * Prompt the user for the name of an operation batch.
   *
   * @return The new name or an empty {@link Optional} if the user dismissed the prompt.
   */
  private Optional<String> promptBatchName() {
    return Alerts.textInput(
        this.config,
        "alert.operation_name.header",
        "alert.operation_name.label",
        "alert.operation_name.title",
        this.config.language().translate("alert.operation_name.default_name"),
        null
    );
  }

  @Override
  public void onItemClick(@NotNull String item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull String item) {
    this.loadSelectedOperationBatch();
  }

  private void updateButtons() {
    final var items = this.operationBatchList.getItems();
    final boolean invalid = items.isEmpty() || items.stream().anyMatch(ov -> !ov.isValid());
    final boolean noSaveSelection = this.savedOperationBatchesList.getSelectionModel().isEmpty();
    this.newOperationBatchButton.setDisable(this.loadedOperationBatchName == null);
    this.saveOperationBatchButton.setDisable(invalid || !this.unsavedChanges);
    this.deleteOperationBatchButton.setDisable(noSaveSelection);
    this.renameOperationBatchButton.setDisable(noSaveSelection);
    this.loadOperationBatchButton.setDisable(noSaveSelection);
    this.applyButton.setDisable(invalid);
    for (int i = 0; i < items.size(); i++) {
      final var item = items.get(i);
      item.setCanMoveUp(i > 0);
      item.setCanMoveDown(i < items.size() - 1);
    }
  }

  private class OperationsRunner implements Runnable {
    @Override
    public void run() {
      final var dialog = this.dialog();
      final int total;
      final Stream<MediaFile> stream;
      if (dialog.applyToSelectedRadio.isSelected()) {
        total = dialog.selectedMediaFiles.size();
        stream = dialog.selectedMediaFiles.stream();
      } else if (dialog.applyToResultsRadio.isSelected()) {
        total = dialog.resultMediaFiles.size();
        stream = dialog.resultMediaFiles.stream();
      } else {
        total = -1;
        try {
          stream = dialog.db.getAllMedias();
        } catch (final DatabaseOperationException e) {
          this.onAbort(0, e.errorCode());
          return;
        }
      }

      int count = 0;
      dialog.anyMediaUpdate = true;
      try {
        final Iterator<MediaFile> iterator = stream.iterator();
        this.notifyProgress(total, count);
        while (iterator.hasNext()) {
          if (dialog.progressDialog.isCancelled()) {
            App.logger().info("Batch operation cancelled.");
            stream.close();
            this.onCancel(count);
            return;
          }
          MediaFile mediaFile = iterator.next();

          final List<? extends Operation> operations = dialog.operationBatchList.getItems()
              .stream()
              .map(OperationView::getOperation)
              .toList();

          boolean anyApplied = false;
          for (final Operation operation : operations)
            try {
              final var result = operation.apply(mediaFile, dialog.db, dialog.config);
              anyApplied |= result.getKey();
              mediaFile = result.getValue();
            } catch (final DatabaseOperationException | DatabaseOperationRuntimeException e) {
              App.logger().error("Batch operation failed.", e);
            }

          if (anyApplied)
            count++;
          this.notifyProgress(total, count);
        }
      } catch (final DatabaseOperationRuntimeException e) {
        stream.close();
        this.onAbort(count, e.errorCode());
        return;
      }
      this.onSuccess(count);
    }

    private void notifyProgress(int total, int counter) {
      Platform.runLater(() -> {
        if (total != -1)
          this.dialog().progressDialog.notifyProgress("progress.executing_operations", total, counter);
        else
          this.dialog().progressDialog.notifyIndeterminateProgress("progress.executing_operations");
      });
    }

    private void onSuccess(int count) {
      Platform.runLater(() -> {
        this.dialog().progressDialog.hide();
        Alerts.info(
            this.dialog().config,
            "alert.batch_operation_success.header",
            null,
            null,
            new FormatArg("count", count)
        );
        this.dialog().restoreInteractions();
      });
    }

    private void onCancel(int count) {
      Platform.runLater(() -> {
        this.dialog().progressDialog.hide();
        if (count > 0)
          Alerts.info(
              this.dialog().config,
              "alert.batch_operation_cancelled.header",
              null,
              null,
              new FormatArg("count", count)
          );
        this.dialog().restoreInteractions();
      });
    }

    private void onAbort(int count, @NotNull DatabaseErrorCode errorCode) {
      Platform.runLater(() -> {
        final var dialog = this.dialog();
        dialog.progressDialog.hide();
        Alerts.databaseError(
            dialog.config,
            errorCode,
            "alert.batch_operation_aborted.header",
            null,
            new FormatArg("count", count)
        );
        dialog.restoreInteractions();
      });
    }

    private BatchOperationsDialog dialog() {
      return BatchOperationsDialog.this;
    }
  }

  private enum OperationType {
    UPDATE_TAGS,
    MOVE,
    DELETE,
    RECOMPUTE_HASH,
    TRANSFORM_PATH,
  }
}
