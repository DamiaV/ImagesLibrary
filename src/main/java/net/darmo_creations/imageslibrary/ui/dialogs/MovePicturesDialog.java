package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.application.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This dialog allows moving several images at once.
 */
public class MovePicturesDialog extends DialogBase<Boolean> {
  private final Label warningLabel = new Label();
  private final Label destDirLabel = new Label();
  private final CheckBox deleteEmptyDirsCheckBox = new CheckBox();
  private final CheckBox overwriteTargetFilesCheckBox = new CheckBox();
  private final ListView<String> errorsListView = new ListView<>();
  private final Button applyButton;

  private final ProgressDialog progressDialog;

  private final DatabaseConnection db;
  private final List<Picture> pictures = new LinkedList<>();
  private final ObjectProperty<Path> destDir = new SimpleObjectProperty<>();
  private boolean anyUpdate;

  /**
   * Create a new dialog.
   *
   * @param config The app’s config.
   * @param db     The database.
   */
  public MovePicturesDialog(final @NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "move_pictures", true, ButtonTypes.CLOSE, ButtonTypes.APPLY);
    this.db = db;

    this.applyButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.APPLY);
    this.applyButton.addEventFilter(ActionEvent.ACTION, event -> {
      this.moveFiles();
      event.consume();
    });
    this.destDir.addListener((observable, oldValue, newValue) -> this.updateButtons());

    this.progressDialog = new ProgressDialog(config, "moving_files");

    final Language language = config.language();

    this.warningLabel.setText(language.translate("dialog.move_pictures.warning_duplicate_names"));
    this.warningLabel.setGraphic(config.theme().getIcon(Icon.WARNING, Icon.Size.SMALL));
    this.warningLabel.managedProperty().bind(this.warningLabel.visibleProperty());

    final Button destDirButton = new Button();
    destDirButton.setText(language.translate("dialog.move_pictures.dest_dir_button"));
    destDirButton.setGraphic(config.theme().getIcon(Icon.SELECT_DIRECTORY, Icon.Size.SMALL));
    destDirButton.setOnAction(event -> this.onDestDirAction());

    final HBox destBox = new HBox(5, new Label(language.translate("dialog.move_pictures.dest_dir")), this.destDirLabel, destDirButton);
    destBox.setAlignment(Pos.CENTER_LEFT);

    this.deleteEmptyDirsCheckBox.setText(language.translate("dialog.move_pictures.delete_empty_dirs"));
    this.overwriteTargetFilesCheckBox.setText(language.translate("dialog.move_pictures.overwrite_target_files"));

    VBox.setVgrow(this.errorsListView, Priority.ALWAYS);

    this.getDialogPane().setContent(new VBox(
        5,
        this.warningLabel,
        destBox,
        this.deleteEmptyDirsCheckBox,
        this.overwriteTargetFilesCheckBox,
        new Label(language.translate("dialog.move_pictures.errors")),
        this.errorsListView
    ));

    final Stage stage = this.stage();
    stage.setMinWidth(500);
    stage.setMinHeight(300);

    this.setResultConverter(buttonType -> this.anyUpdate);

    this.updateButtons();
  }

  /**
   * Set the pictures to move.
   *
   * @param pictures The pictures.
   */
  public void setPictures(final @NotNull Collection<Picture> pictures) {
    this.pictures.clear();
    this.pictures.addAll(pictures);
    this.errorsListView.getItems().clear();
    final Set<String> names = new HashSet<>();
    boolean showWarning = false;
    for (final Picture picture : pictures) {
      final String fileName = picture.path().getFileName().toString();
      if (names.contains(fileName)) {
        showWarning = true;
        break;
      }
      names.add(fileName);
    }
    this.warningLabel.setVisible(showWarning);
    this.updateButtons();
  }

  private void onDestDirAction() {
    FileChoosers.showDirectoryChooser(this.config, this.stage()).ifPresent(path -> {
      final String s = path.toString();
      this.destDirLabel.setText(s);
      this.destDirLabel.setTooltip(new Tooltip(s));
      this.destDir.set(path);
    });
  }

  private void moveFiles() {
    if (this.destDir == null)
      return;

    final boolean overwriteTarget = this.overwriteTargetFilesCheckBox.isSelected();
    new Thread(() -> {
      final List<Picture> errors = new LinkedList<>();
      final int total = this.pictures.size();
      int counter = 0;
      this.notifyProgress(total, counter);
      for (final Picture picture : this.pictures) {
        if (this.progressDialog.isCancelled()) {
          App.logger().info("File moving cancelled.");
          return;
        }
        final Path newPath = this.destDir.get().resolve(picture.path().getFileName());
        try {
          this.db.moveOrRenamePicture(picture, newPath, overwriteTarget);
        } catch (final DatabaseOperationException e) {
          errors.add(picture);
        }
        counter++;
        this.notifyProgress(total, counter);
      }

      Platform.runLater(() -> this.onMoveDone(errors));
    }, "Pictures Mover Thread").start();
  }

  private void notifyProgress(int total, int counter) {
    Platform.runLater(() -> this.progressDialog.notifyProgress("progress.moving_files", total, counter));
  }

  private void onMoveDone(final @NotNull List<Picture> errors) {
    if (!errors.isEmpty()) {
      Alerts.warning(this.config, "alert.file_moving_errors.header", null, null);
      this.errorsListView.getItems().clear();
      errors.stream().map(p -> p.path().toString()).forEach(this.errorsListView.getItems()::add);
      this.errorsListView.getItems().sort(null);
    } else // No need to show if there are errors
      Alerts.info(this.config, "alert.file_moving_done.header", null, null);

    final Set<Path> paths = new HashSet<>();
    if (this.deleteEmptyDirsCheckBox.isSelected())
      // Go through the direct parent folder of each moved picture and delete any that are empty
      for (final Picture picture : this.pictures) {
        if (errors.contains(picture))
          continue;
        final Path directory = picture.path().getParent();
        if (!paths.contains(directory)) {
          paths.add(directory);
          if (Files.isDirectory(directory))
            try (final var stream = Files.newDirectoryStream(directory)) {
              if (!stream.iterator().hasNext())
                Files.delete(directory);
            } catch (final IOException | SecurityException e) {
              App.logger().error("Could not delete directory {}", directory, e);
            }
        }
      }
    this.pictures.retainAll(errors);
    this.anyUpdate = true;
  }

  private void updateButtons() {
    this.applyButton.setDisable(this.destDir.get() == null || this.pictures.isEmpty());
  }
}
