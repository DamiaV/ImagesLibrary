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
public class SimilarImagesDialog extends DialogBase<ButtonType> {
  private final ImageView imageView = new ImageView();
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final ListView<PictureView> picturesList = new ListView<>();

  public SimilarImagesDialog(@NotNull Config config) {
    super(config, "similar_images", true, false, ButtonTypes.CLOSE);
    this.getDialogPane().setContent(this.createContent());
    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);
  }

  private Node createContent() {
    final HBox imageViewBox = new HBox(this.imageView);
    imageViewBox.setAlignment(Pos.CENTER);
    this.imageView.setPreserveRatio(true);
    this.imageView.fitHeightProperty().bind(imageViewBox.heightProperty().subtract(10));
    this.imageView.fitWidthProperty().bind(this.stage().widthProperty().subtract(30));
    imageViewBox.setMinHeight(200);

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
        imageViewBox,
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
      imageView.setFitWidth(100);
      imageView.setFitHeight(100);
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
