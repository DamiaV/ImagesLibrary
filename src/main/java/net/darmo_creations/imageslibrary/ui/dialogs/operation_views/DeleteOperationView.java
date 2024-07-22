package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This view provides a form to edit a {@link DeleteOperation}.
 */
public class DeleteOperationView extends OperationView<DeleteOperation> {
  private CheckBox deleteFromDiskCheckBox;
  private CheckBox deleteEmptySourceDirectoryCheckBox;

  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public DeleteOperationView(
      DeleteOperation operation,
      boolean expanded,
      @NotNull DatabaseConnection db,
      @NotNull Config config
  ) {
    super(operation, expanded, db, config);
    this.deleteFromDiskCheckBox.setSelected(operation != null && operation.deleteFromDisk());
    this.deleteEmptySourceDirectoryCheckBox.setSelected(operation != null && operation.deleteEmptySourceDirectory());
  }

  @Override
  protected String getTitleKey() {
    return DeleteOperation.KEY;
  }

  @Override
  protected Optional<Node> setupContent(@NotNull Config config) {
    final Language language = config.language();

    this.deleteFromDiskCheckBox = new CheckBox();
    this.deleteFromDiskCheckBox.setText(language.translate("operation_view.delete.from_disk"));
    this.deleteFromDiskCheckBox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());
    this.deleteFromDiskCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      this.deleteEmptySourceDirectoryCheckBox.setDisable(newValue);
      if (newValue)
        this.deleteEmptySourceDirectoryCheckBox.setSelected(false);
    });

    this.deleteEmptySourceDirectoryCheckBox = new CheckBox(
        language.translate("operation_view.move.delete_source_directory_if_empty")
    );
    this.deleteEmptySourceDirectoryCheckBox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());

    return Optional.of(new VBox(
        5,
        this.deleteFromDiskCheckBox,
        this.deleteEmptySourceDirectoryCheckBox
    ));
  }

  @Override
  protected boolean isOperationValid() {
    return true;
  }

  @Override
  public DeleteOperation getOperation() {
    return new DeleteOperation(
        this.deleteFromDiskCheckBox.isSelected(),
        this.deleteEmptySourceDirectoryCheckBox.isSelected(),
        this.getCondition().orElse(null)
    );
  }
}
