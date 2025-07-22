package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A dialog that allows merging the tags of two images, discarding one of the two afterwards.
 */
public class MergeImagesTagsDialog extends DialogBase<Boolean> {
  private final HBox mediaViewerBox1;
  private final HBox mediaViewerBox2;
  private final MediaViewer mediaViewer1;
  private final MediaViewer mediaViewer2;
  private final PictureMetadataView pictureMetadataView1;
  private final PictureMetadataView pictureMetadataView2;
  private final Button swapButton = new Button();
  private final CheckBox deleteFromDiskCheckBox = new CheckBox();

  private final DatabaseConnection db;
  private Picture picture1, picture2;
  private final BooleanProperty leftToRight = new SimpleBooleanProperty(this, "leftToRight", true);
  private boolean preventClosing;

  public MergeImagesTagsDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "merge_images_tags", true, ButtonTypes.CANCEL, ButtonTypes.OK);
    this.db = Objects.requireNonNull(db);

    this.mediaViewer1 = new MediaViewer(config);
    this.mediaViewer2 = new MediaViewer(config);

    this.mediaViewerBox1 = new HBox(this.mediaViewer1);
    this.mediaViewerBox2 = new HBox(this.mediaViewer2);

    this.pictureMetadataView1 = new PictureMetadataView(this.mediaViewer1);
    this.pictureMetadataView2 = new PictureMetadataView(this.mediaViewer2);

    this.getDialogPane().setContent(this.createContent());

    this.leftToRight.addListener((observable, oldValue, newValue) -> this.updateSwapButton());

    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);

    this.setResultConverter(buttonType -> {
      final boolean apply = !buttonType.getButtonData().isCancelButton();
      if (apply)
        this.preventClosing = !this.applyChanges();
      return apply;
    });
    this.setOnCloseRequest(event -> {
      if (this.preventClosing) {
        event.consume();
        this.preventClosing = false;
      } else {
        // Dispose of any loaded MediaPlayer
        this.mediaViewer1.setMedia(null);
        this.mediaViewer2.setMedia(null);
      }
    });

    this.updateSwapButton();
  }

  private Node createContent() {
    this.mediaViewerBox1.setAlignment(Pos.CENTER);
    this.mediaViewerBox1.setMinHeight(200);
    this.mediaViewerBox1.heightProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewsSizes());
    this.mediaViewerBox2.setAlignment(Pos.CENTER);
    this.mediaViewerBox2.setMinHeight(200);
    this.mediaViewerBox2.heightProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewsSizes());
    this.stage().widthProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewsSizes());

    HBox.setHgrow(this.pictureMetadataView1, Priority.ALWAYS);
    HBox.setHgrow(this.pictureMetadataView2, Priority.ALWAYS);
    this.pictureMetadataView1.prefWidthProperty().bindBidirectional(this.pictureMetadataView2.prefWidthProperty());

    this.swapButton.setTooltip(new Tooltip(this.config.language().translate("dialog.merge_images_tags.swap_button.tooltip")));
    this.swapButton.setOnAction(event -> this.leftToRight.set(!this.leftToRight.get()));

    this.deleteFromDiskCheckBox.setText(this.config.language().translate("dialog.merge_images_tags.delete_from_disk_button"));

    final HBox metadataBox = new HBox(
        5,
        this.pictureMetadataView1,
        new VBox(new VerticalSpacer(), this.swapButton, new VerticalSpacer()),
        this.pictureMetadataView2
    );
    VBox.setVgrow(metadataBox, Priority.ALWAYS);

    final HBox checkBoxBox = new HBox(this.deleteFromDiskCheckBox);
    checkBoxBox.setAlignment(Pos.CENTER);

    final Label label = new Label(
        this.config.language().translate("dialog.merge_images_tags.info"),
        this.config.theme().getIcon(Icon.INFO, Icon.Size.SMALL)
    );
    label.setWrapText(true);
    label.setPrefHeight(40);
    label.setMinHeight(40);

    final SplitPane splitPane = new SplitPane(
        new HBox(5, this.mediaViewerBox1, this.mediaViewerBox2),
        new VBox(
            5,
            metadataBox,
            checkBoxBox,
            label
        )
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    splitPane.setPrefWidth(800);
    splitPane.setPrefHeight(600);
    return splitPane;
  }

  private void updateImageViewsSizes() {
    this.resizeImageView(this.mediaViewerBox1);
    this.resizeImageView(this.mediaViewerBox2);
  }

  private void resizeImageView(@NotNull HBox imageViewBox) {
    if (this.getDialogPane().getScene() == null)
      return;

    final double halfStageW = this.stage().getWidth() / 2;
    final MediaViewer mediaViewer = (MediaViewer) imageViewBox.getChildren().get(0);
    imageViewBox.setPrefWidth(halfStageW - 10);
    mediaViewer.updateImageViewSize(halfStageW - 20, imageViewBox.getHeight() - 10);
  }

  /**
   * Set the pictures to merge.
   *
   * @param picture1 A picture.
   * @param tags1    The first picture’s tags.
   * @param picture2 Another picture.
   * @param tags2    The second picture’s tags.
   */
  public void setPictures(
      @NotNull Picture picture1, @NotNull Set<Tag> tags1,
      @NotNull Picture picture2, @NotNull Set<Tag> tags2
  ) {
    this.leftToRight.set(true);
    this.picture1 = Objects.requireNonNull(picture1);
    this.picture2 = Objects.requireNonNull(picture2);
    this.pictureMetadataView1.setMedia(picture1, tags1);
    this.pictureMetadataView2.setMedia(picture2, tags2);

    this.updateImageViewsSizes();
  }

  private void updateSwapButton() {
    final Icon icon = this.leftToRight.get() ? Icon.MERGE_TO_RIGHT : Icon.MERGE_TO_LEFT;
    this.swapButton.setGraphic(this.config.theme().getIcon(icon, Icon.Size.BIG));
  }

  private boolean applyChanges() {
    final Picture source, dest;
    if (this.leftToRight.get()) {
      source = this.picture1;
      dest = this.picture2;
    } else {
      source = this.picture2;
      dest = this.picture1;
    }

    try {
      this.db.mergePictures(source, dest, this.deleteFromDiskCheckBox.isSelected());
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return false;
    }
    return true;
  }

  private class PictureMetadataView extends VBox {
    private final Button openInExplorerButton = new Button();
    private final ListView<TagView> tagsList = new ListView<>();
    private final MediaViewer mediaViewer;

    private Picture picture;

    public PictureMetadataView(@NotNull MediaViewer mediaViewer) {
      super(5);
      this.mediaViewer = Objects.requireNonNull(mediaViewer);
      this.mediaViewer.setOnLoadedCallback(ignored -> this.updateImageViewBoxContent());
      this.mediaViewer.setOnLoadErrorCallback(this::onFileLoadingError);

      final Config config = MergeImagesTagsDialog.this.config;
      final Language language = config.language();
      final Theme theme = config.theme();

      final HBox tagsLabelBox = new HBox(new Label(language.translate("image_preview.section.tags.title")));
      tagsLabelBox.getStyleClass().add("section-title");
      tagsLabelBox.setAlignment(Pos.CENTER);

      this.openInExplorerButton.setTooltip(new Tooltip(language.translate("image_preview.open_in_explorer_button.label")));
      this.openInExplorerButton.setGraphic(theme.getIcon(Icon.OPEN_FILE_IN_EXPLORER, Icon.Size.SMALL));
      this.openInExplorerButton.setOnAction(e -> this.onOpenFile());

      HBox.setHgrow(tagsLabelBox, Priority.ALWAYS);
      final HBox tagsTitleBox = new HBox(5, tagsLabelBox, this.openInExplorerButton);
      tagsTitleBox.setPadding(new Insets(0, 5, 0, 5));

      this.tagsList.setPrefHeight(150);
      this.tagsList.setMinHeight(30);
      VBox.setVgrow(this.tagsList, Priority.ALWAYS);

      this.getChildren().addAll(tagsTitleBox, this.tagsList);
    }

    public void setMedia(@NotNull Picture picture, @NotNull Set<Tag> tags) {
      this.picture = Objects.requireNonNull(picture);

      this.mediaViewer.setMedia(this.picture);

      this.tagsList.getItems().clear();
      final var tagsEntries = tags.stream()
          .sorted()
          .map(TagView::new)
          .toList();
      this.tagsList.getItems().addAll(tagsEntries);
    }

    private void updateImageViewBoxContent() {
      this.openInExplorerButton.setDisable(false);
      MergeImagesTagsDialog.this.updateImageViewsSizes();
    }

    private void onFileLoadingError(Exception error) {
      this.openInExplorerButton.setDisable(true);
    }

    private void onOpenFile() {
      if (this.picture != null)
        FileUtils.openInFileExplorer(this.picture.path().toString());
    }
  }
}
