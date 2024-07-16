package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.Node;
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
  private final HBox imageViewBox;
  private final ImageView imageView = new ImageView();
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final ListView<PictureView> picturesList = new ListView<>();
  private final ListView<TagView> tagsList = new ListView<>();
  private final Button copyTagsButton = new Button();

  private final DatabaseConnection db;
  private final Set<TagCopyListener> tagCopyListeners = new HashSet<>();

  public SimilarImagesDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "similar_images", true, false, ButtonTypes.CLOSE);
    this.db = db;
    this.imageViewBox = new HBox(this.imageView);
    this.getDialogPane().setContent(this.createContent());
    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    this.setResultConverter(buttonType -> null);
  }

  private Node createContent() {
    final Language language = this.config.language();

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
        this.imageViewBox,
        new VBox(
            5,
            fileNameBox,
            metadataBox,
            new SplitPane(
                this.picturesList,
                new VBox(
                    5,
                    hBox,
                    this.tagsList
                )
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

  public void addTagCopyListener(@NotNull TagCopyListener listener) {
    this.tagCopyListeners.add(Objects.requireNonNull(listener));
  }

  private void onCopyTags() {
    final Set<Tag> tags = this.tagsList.getItems().stream()
        .map(TagView::tag)
        .collect(Collectors.toSet());
    this.tagCopyListeners.forEach(l -> l.onTagCopy(tags));
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
    this.tagsList.getItems().clear();
    if (pictureView != null) {
      final Picture picture = pictureView.picture();
      final String fileName = picture.path().getFileName().toString();
      this.fileNameLabel.setText(fileName);
      this.fileNameLabel.setTooltip(new Tooltip(fileName));
      this.fileMetadataLabel.setText(
          pictureView.metadata().orElse(this.config.language().translate("image_preview.missing_file")));
      pictureView.image().ifPresent(this.imageView::setImage);
      this.updateImageViewSize();
      try {
        this.db.getImageTags(picture).stream()
            .sorted(Comparator.comparing(Tag::label))
            .forEach(tag -> this.tagsList.getItems().add(new TagView(tag)));
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
      }
    } else {
      this.fileNameLabel.setText(null);
      this.fileNameLabel.setTooltip(new Tooltip(null));
    }
    this.copyTagsButton.setDisable(this.tagsList.getItems().isEmpty());
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

  @FunctionalInterface
  public interface TagCopyListener {
    void onTagCopy(@NotNull Set<Tag> tags);
  }
}
