package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;

/**
 * Dialog that shows images that are similar to a given one.
 */
public class SimilarImagesDialog extends DialogBase<Void> {
  private final HBox imageViewBox;
  private final ImageView imageView = new ImageView();
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final ListView<PictureView> picturesList = new ListView<>();

  public SimilarImagesDialog(@NotNull Config config) {
    super(config, "similar_images", true, false, ButtonTypes.CLOSE);
    this.imageViewBox = new HBox(this.imageView);
    this.getDialogPane().setContent(this.createContent());
    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    this.setResultConverter(buttonType -> null);
  }

  private Node createContent() {
    this.imageViewBox.setAlignment(Pos.CENTER);
    this.imageViewBox.setMinHeight(200);
    this.imageViewBox.heightProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewSize());
    this.stage().widthProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewSize());
    this.imageView.setPreserveRatio(true);

    final HBox fileNameBox = new HBox(this.fileNameLabel);
    fileNameBox.setAlignment(Pos.CENTER);
    fileNameBox.setPadding(new Insets(0, 5, 0, 5));

    final HBox metadataBox = new HBox(this.fileMetadataLabel);
    metadataBox.setAlignment(Pos.CENTER);
    metadataBox.setPadding(new Insets(0, 5, 0, 5));

    this.picturesList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null)
        this.setPicture(newValue);
    });
    VBox.setVgrow(this.picturesList, Priority.ALWAYS);

    final SplitPane splitPane = new SplitPane(
        this.imageViewBox,
        new VBox(5, fileNameBox, metadataBox, this.picturesList)
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    splitPane.setPrefWidth(800);
    splitPane.setPrefHeight(600);
    return splitPane;
  }

  /**
   * Set the list of pictures to display.
   *
   * @param pictures The pictures to show.
   */
  public void setPictures(final @NotNull Collection<Pair<Picture, Float>> pictures) {
    this.setPicture(null);
    this.picturesList.getItems().clear();
    this.picturesList.getItems().addAll(
        pictures.stream()
            .sorted(Comparator.comparing(entry -> -entry.getValue()))
            .map(entry -> new PictureView(entry.getKey(), entry.getValue()))
            .toList()
    );
  }

  private void updateImageViewSize() {
    final Image image = this.imageView.getImage();
    if (image != null) {
      final double width = Math.min(image.getWidth(), this.stage().getWidth() - 20);
      this.imageView.setFitWidth(width);
      final double height = Math.min(image.getHeight(), this.imageViewBox.getHeight() - 10);
      this.imageView.setFitHeight(height);
    }
  }

  private void setPicture(PictureView pictureView) {
    this.imageView.setImage(null);
    this.fileMetadataLabel.setText(null);
    this.fileMetadataLabel.setTooltip(null);
    if (pictureView != null) {
      final String fileName = pictureView.picture().path().getFileName().toString();
      this.fileNameLabel.setText(fileName);
      this.fileNameLabel.setTooltip(new Tooltip(fileName));
      this.fileMetadataLabel.setText(
          pictureView.metadata().orElse(this.config.language().translate("image_preview.missing_file")));
      pictureView.image().ifPresent(this.imageView::setImage);
      this.updateImageViewSize();
    } else {
      this.fileNameLabel.setText(null);
      this.fileNameLabel.setTooltip(new Tooltip(null));
    }
  }

  private class PictureView extends HBox {
    private Image image;
    private final Picture picture;
    private String metadata;

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
      if (!exists) {
        fileMetadataLabel.setText(language.translate("image_preview.missing_file"));
      } else {
        fileMetadataLabel.setText(language.translate("image_preview.loading"));
        FileUtils.loadImage(
            path,
            image -> {
              this.image = image;
              imageView.setImage(image);
              imageView.setFitWidth(Math.min(image.getWidth(), 100));
              imageView.setFitHeight(Math.min(image.getHeight(), 100));
              this.metadata = FileUtils.formatImageMetadata(path, image, config);
              fileMetadataLabel.setText(this.metadata);
              fileMetadataLabel.setTooltip(new Tooltip(this.metadata));
            },
            error -> fileMetadataLabel.setText(language.translate("image_preview.missing_file"))
        );
      }
      final Label fileNameLabel = new Label(path.toString());
      final Label confidenceLabel = new Label(language.translate(
          "dialog.similar_images.confidence",
          new FormatArg("confidence", "%.2f".formatted(100 * confidence))
      ));
      this.getChildren().addAll(
          imageView,
          new VBox(5, fileNameLabel, fileMetadataLabel, confidenceLabel)
      );
    }

    public Picture picture() {
      return this.picture;
    }

    public Optional<Image> image() {
      return Optional.ofNullable(this.image);
    }

    public Optional<String> metadata() {
      return Optional.ofNullable(this.metadata);
    }
  }
}
