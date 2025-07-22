package net.darmo_creations.bildumilo.ui.dialogs.operation_views;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.data.batch_operations.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.ui.dialogs.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * This view provides a form to edit a {@link MoveOperation}.
 */
public class MoveOperationView extends OperationView<MoveOperation> {
  private Label pathLabel;
  private CheckBox deleteEmptySourceDirectoryCheckBox;
  private CheckBox overwriteTargetCheckBox;

  private Path path;

  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public MoveOperationView(
      MoveOperation operation,
      boolean expanded,
      @NotNull DatabaseConnection db,
      @NotNull Config config
  ) {
    super(operation, expanded, db, config);
    this.path = operation == null ? null : operation.targetPath();
    this.pathLabel.setText(this.path == null ? null : this.path.toString());
    this.deleteEmptySourceDirectoryCheckBox.setSelected(operation != null && operation.deleteEmptySourceDirectory());
    this.overwriteTargetCheckBox.setSelected(operation != null && operation.overwriteTarget());
  }

  @Override
  protected String getTitleKey() {
    return MoveOperation.KEY;
  }

  @Override
  protected Optional<Node> setupContent(@NotNull Config config) {
    final Language language = config.language();

    final Button selectDirButton = new Button(
        language.translate("operation_view.move.select_directory"),
        config.theme().getIcon(Icon.SELECT_DIRECTORY, Icon.Size.SMALL)
    );
    selectDirButton.setOnAction(event -> this.onSelectDir(config));

    this.pathLabel = new Label();

    this.deleteEmptySourceDirectoryCheckBox = new CheckBox(
        language.translate("operation_view.move.delete_source_directory_if_empty")
    );
    this.deleteEmptySourceDirectoryCheckBox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());

    this.overwriteTargetCheckBox = new CheckBox(
        language.translate("operation_view.move.overwrite_target")
    );
    this.overwriteTargetCheckBox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());

    final HBox pathBox = new HBox(5, selectDirButton, this.pathLabel);
    pathBox.setAlignment(Pos.CENTER_LEFT);
    return Optional.of(new VBox(5,
        pathBox,
        this.deleteEmptySourceDirectoryCheckBox,
        this.overwriteTargetCheckBox
    ));
  }

  @Override
  protected boolean isOperationValid() {
    return this.path != null;
  }

  @Override
  public MoveOperation getOperation() {
    if (this.path == null)
      throw new IllegalStateException("Target path is empty");
    return new MoveOperation(
        this.path,
        this.deleteEmptySourceDirectoryCheckBox.isSelected(),
        this.overwriteTargetCheckBox.isSelected(),
        this.getCondition().orElse(null)
    );
  }

  private void onSelectDir(@NotNull Config config) {
    FileChoosers.showDirectoryChooser(config, this.getScene().getWindow()).ifPresent(p -> {
      this.path = p;
      this.pathLabel.setText(p.toString());
      this.notifyUpdateListeners();
    });
  }
}
