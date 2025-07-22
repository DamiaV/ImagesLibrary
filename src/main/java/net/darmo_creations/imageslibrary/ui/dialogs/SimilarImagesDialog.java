package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Dialog that shows images that are similar to a given one.
 */
public class SimilarImagesDialog extends DialogBase<Set<Tag>> {
  private final HBox mediaViewerBox;
  private final MediaViewer mediaViewer;
  private final ListView<PictureView> picturesList = new ListView<>();
  private final ListView<TagView> tagsList = new ListView<>();
  private final Button copyTagsButton = new Button();

  private final DatabaseConnection db;
  private final Set<TagCopyListener> tagCopyListeners = new HashSet<>();

  public SimilarImagesDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "similar_images", true, false, ButtonTypes.CLOSE);
    this.db = db;
    this.mediaViewer = new MediaViewer(config);
    this.mediaViewer.setOnLoadedCallback(ignored -> this.updateMediaViewerSize());
    this.mediaViewerBox = new HBox(this.mediaViewer);
    this.getDialogPane().setContent(this.createContent());
    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    this.setResultConverter(buttonType -> null);
    // Dispose of any loaded MediaPlayer
    this.setOnCloseRequest(event -> this.mediaViewer.setMedia(null));
  }

  private SplitPane createContent() {
    final Language language = this.config.language();

    this.mediaViewerBox.setAlignment(Pos.CENTER);
    this.mediaViewerBox.setMinHeight(200);
    this.mediaViewerBox.heightProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewerSize());
    this.stage().widthProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewerSize());

    this.picturesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null)
        this.setMedia(newValue);
    });

    VBox.setVgrow(this.tagsList, Priority.ALWAYS);

    this.copyTagsButton.setGraphic(this.config.theme().getIcon(Icon.COPY_TAGS, Icon.Size.SMALL));
    this.copyTagsButton.setTooltip(new Tooltip(language.translate("dialog.similar_images.copy_tags")));
    this.copyTagsButton.setOnAction(event -> this.onCopyTags());

    final HBox hBox = new HBox(
        5,
        new HorizontalSpacer(),
        new Label(language.translate("dialog.similar_images.selected_image_tags")),
        new HorizontalSpacer(),
        this.copyTagsButton
    );
    hBox.setAlignment(Pos.CENTER_LEFT);

    final SplitPane splitPane = new SplitPane(
        this.mediaViewerBox,
        new SplitPane(
            this.picturesList,
            new VBox(
                5,
                hBox,
                this.tagsList
            )
        )
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    splitPane.setPrefWidth(800);
    splitPane.setPrefHeight(600);
    return splitPane;
  }

  /**
   * Set the list of medias to display.
   *
   * @param medias The medias to show.
   */
  public void setMedias(final @NotNull Collection<Pair<Picture, Float>> medias) {
    this.setMedia(null);
    this.picturesList.getItems().clear();
    this.picturesList.getItems().addAll(
        medias.stream()
            .sorted(Comparator.comparing(entry -> -entry.getValue()))
            .map(entry -> new PictureView(entry.getKey(), entry.getValue()))
            .toList()
    );
  }

  public void addTagCopyListener(@NotNull TagCopyListener listener) {
    this.tagCopyListeners.add(Objects.requireNonNull(listener));
  }

  private void onCopyTags() {
    final Set<Tag> tags = this.tagsList.getItems().stream()
        .map(TagView::tag)
        .collect(Collectors.toSet());
    this.tagCopyListeners.forEach(l -> l.onTagCopy(tags));
  }

  private void updateMediaViewerSize() {
    this.mediaViewer.updateImageViewSize(
        this.stage().getWidth() - 20,
        this.mediaViewerBox.getHeight() - 10
    );
  }

  private void setMedia(PictureView pictureView) {
    this.mediaViewer.setMedia(null);
    this.tagsList.getItems().clear();

    if (pictureView != null) {
      final Picture picture = pictureView.picture();
      this.mediaViewer.setMedia(picture);
      try {
        this.db.getImageTags(picture).stream()
            .sorted()
            .forEach(tag -> this.tagsList.getItems().add(new TagView(tag)));
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
      }
    }

    this.copyTagsButton.setDisable(this.tagsList.getItems().isEmpty());
  }

  private class PictureView extends HBox {
    private final Picture picture;

    private PictureView(@NotNull Picture picture, float confidence) {
      super(5);
      this.picture = Objects.requireNonNull(picture);

      final Path path = picture.path();
      final ImageView imageView = new ImageView();
      imageView.setPreserveRatio(true);
      final Label fileMetadataLabel = new Label();
      final Config config = SimilarImagesDialog.this.config;
      final Language language = config.language();

      boolean exists;
      try {
        exists = Files.exists(path);
      } catch (final SecurityException e) {
        exists = false;
      }

      if (!exists) fileMetadataLabel.setText(language.translate("media_viewer.missing_file"));
      else {
        fileMetadataLabel.setText(language.translate("media_viewer.loading"));
        FileUtils.loadImage(
            path,
            image -> {
              imageView.setImage(image);
              imageView.setFitWidth(Math.min(image.getWidth(), 100));
              imageView.setFitHeight(Math.min(image.getHeight(), 100));
              final String metadata = FileUtils.formatImageMetadata(path, image, config);
              fileMetadataLabel.setText(metadata);
              fileMetadataLabel.setTooltip(new Tooltip(metadata));
            },
            error -> fileMetadataLabel.setText(language.translate("media_viewer.file_loading_error"))
        );
      }

      final Label fileNameLabel = new Label(path.toString());
      final Label confidenceLabel = new Label(language.translate(
          "dialog.similar_images.confidence",
          new FormatArg("confidence", "%.1f".formatted(100 * confidence))
      ));
      this.getChildren().addAll(
          imageView,
          new VBox(5, fileNameLabel, fileMetadataLabel, confidenceLabel)
      );
    }

    public Picture picture() {
      return this.picture;
    }
  }

  @FunctionalInterface
  public interface TagCopyListener {
    void onTagCopy(@NotNull Set<Tag> tags);
  }
}
