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
 * A dialog that allows merging the tags of two media files, discarding one of the two afterwards.
 */
public class MergeMediaTagsDialog extends DialogBase<Boolean> {
  private final HBox mediaViewerBox1;
  private final HBox mediaViewerBox2;
  private final MediaViewer mediaViewer1;
  private final MediaViewer mediaViewer2;
  private final MediaMetadataView mediaMetadataView1;
  private final MediaMetadataView mediaMetadataView2;
  private final Button swapButton = new Button();
  private final CheckBox deleteFromDiskCheckBox = new CheckBox();

  private final DatabaseConnection db;
  private MediaFile mediaFile1, mediaFile2;
  private final BooleanProperty leftToRight = new SimpleBooleanProperty(this, "leftToRight", true);
  private boolean preventClosing;

  public MergeMediaTagsDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "merge_images_tags", true, ButtonTypes.CANCEL, ButtonTypes.OK);
    this.db = Objects.requireNonNull(db);

    this.mediaViewer1 = new MediaViewer(config);
    this.mediaViewer2 = new MediaViewer(config);

    this.mediaViewerBox1 = new HBox(this.mediaViewer1);
    this.mediaViewerBox2 = new HBox(this.mediaViewer2);

    this.mediaMetadataView1 = new MediaMetadataView(this.mediaViewer1);
    this.mediaMetadataView2 = new MediaMetadataView(this.mediaViewer2);

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
    this.mediaViewerBox1.heightProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewersSizes());
    this.mediaViewerBox2.setAlignment(Pos.CENTER);
    this.mediaViewerBox2.setMinHeight(200);
    this.mediaViewerBox2.heightProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewersSizes());
    this.stage().widthProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewersSizes());

    HBox.setHgrow(this.mediaMetadataView1, Priority.ALWAYS);
    HBox.setHgrow(this.mediaMetadataView2, Priority.ALWAYS);
    this.mediaMetadataView1.prefWidthProperty().bindBidirectional(this.mediaMetadataView2.prefWidthProperty());

    this.swapButton.setTooltip(new Tooltip(this.config.language().translate("dialog.merge_images_tags.swap_button.tooltip")));
    this.swapButton.setOnAction(event -> this.leftToRight.set(!this.leftToRight.get()));

    this.deleteFromDiskCheckBox.setText(this.config.language().translate("dialog.merge_images_tags.delete_from_disk_button"));

    final HBox metadataBox = new HBox(
        5,
        this.mediaMetadataView1,
        new VBox(new VerticalSpacer(), this.swapButton, new VerticalSpacer()),
        this.mediaMetadataView2
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

  private void updateMediaViewersSizes() {
    this.resizeMediaViewer(this.mediaViewerBox1);
    this.resizeMediaViewer(this.mediaViewerBox2);
  }

  private void resizeMediaViewer(@NotNull HBox mediaViewerBox) {
    if (this.getDialogPane().getScene() == null)
      return;

    final double halfStageW = this.stage().getWidth() / 2;
    final MediaViewer mediaViewer = (MediaViewer) mediaViewerBox.getChildren().get(0);
    mediaViewerBox.setPrefWidth(halfStageW - 10);
    mediaViewer.updateSize(halfStageW - 20, mediaViewerBox.getHeight() - 10);
  }

  /**
   * Set the medias to merge.
   *
   * @param mediaFile1 A media.
   * @param tags1      The first media’s tags.
   * @param mediaFile2 Another media.
   * @param tags2      The second media’s tags.
   */
  public void setMedias(
      @NotNull MediaFile mediaFile1, @NotNull Set<Tag> tags1,
      @NotNull MediaFile mediaFile2, @NotNull Set<Tag> tags2
  ) {
    this.leftToRight.set(true);
    this.mediaFile1 = Objects.requireNonNull(mediaFile1);
    this.mediaFile2 = Objects.requireNonNull(mediaFile2);
    this.mediaMetadataView1.setMedia(mediaFile1, tags1);
    this.mediaMetadataView2.setMedia(mediaFile2, tags2);

    this.updateMediaViewersSizes();
  }

  private void updateSwapButton() {
    final Icon icon = this.leftToRight.get() ? Icon.MERGE_TO_RIGHT : Icon.MERGE_TO_LEFT;
    this.swapButton.setGraphic(this.config.theme().getIcon(icon, Icon.Size.BIG));
  }

  private boolean applyChanges() {
    final MediaFile source, dest;
    if (this.leftToRight.get()) {
      source = this.mediaFile1;
      dest = this.mediaFile2;
    } else {
      source = this.mediaFile2;
      dest = this.mediaFile1;
    }

    try {
      this.db.mergeMedias(source, dest, this.deleteFromDiskCheckBox.isSelected());
    } catch (final DatabaseOperationException e) {
      Alerts.databaseError(this.config, e.errorCode());
      return false;
    }
    return true;
  }

  private class MediaMetadataView extends VBox {
    private final Button openInExplorerButton = new Button();
    private final ListView<TagView> tagsList = new ListView<>();
    private final MediaViewer mediaViewer;

    private MediaFile mediaFile;

    public MediaMetadataView(@NotNull MediaViewer mediaViewer) {
      super(5);
      this.mediaViewer = Objects.requireNonNull(mediaViewer);
      this.mediaViewer.setOnLoadedCallback(ignored -> this.onMediaLoaded());
      this.mediaViewer.setOnLoadErrorCallback(this::onFileLoadingError);

      final Config config = MergeMediaTagsDialog.this.config;
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

    public void setMedia(@NotNull MediaFile mediaFile, @NotNull Set<Tag> tags) {
      this.mediaFile = Objects.requireNonNull(mediaFile);

      this.mediaViewer.setMedia(this.mediaFile);

      this.tagsList.getItems().clear();
      final var tagsEntries = tags.stream()
          .sorted()
          .map(TagView::new)
          .toList();
      this.tagsList.getItems().addAll(tagsEntries);
    }

    private void onMediaLoaded() {
      this.openInExplorerButton.setDisable(false);
      MergeMediaTagsDialog.this.updateMediaViewersSizes();
    }

    private void onFileLoadingError(Exception error) {
      this.openInExplorerButton.setDisable(true);
    }

    private void onOpenFile() {
      if (this.mediaFile != null)
        FileUtils.openInFileExplorer(this.mediaFile.path().toString());
    }
  }
}
