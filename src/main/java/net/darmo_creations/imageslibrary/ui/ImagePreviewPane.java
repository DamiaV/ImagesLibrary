package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class ImagePreviewPane extends SplitPane implements ClickableListCellFactory.ClickListener<TagView> {
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<EditTagsListener> editTagsListeners = new HashSet<>();
  private final Set<Consumer<Picture>> similarImagesListeners = new HashSet<>();

  private final Button openInExplorerButton = new Button();
  private final Button showSimilarImagesButton = new Button();
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final HBox imageViewBox;
  private final ImageView imageView = new ImageView();
  private final Button editTagsButton = new Button();
  private final ListView<TagView> tagsList = new ListView<>();

  private final Config config;
  @Nullable
  private Picture picture;

  public ImagePreviewPane(final @NotNull Config config) {
    this.config = config;
    this.setMinWidth(300);

    final Language language = config.language();
    final Theme theme = config.theme();

    this.openInExplorerButton.setTooltip(new Tooltip(language.translate("image_preview.open_in_explorer_button.label")));
    this.openInExplorerButton.setGraphic(theme.getIcon(Icon.OPEN_FILE_IN_EXPLORER, Icon.Size.SMALL));
    this.openInExplorerButton.setOnAction(e -> this.onOpenFile());

    this.showSimilarImagesButton.setTooltip(new Tooltip(language.translate("image_preview.show_similar_images_button.label")));
    this.showSimilarImagesButton.setGraphic(theme.getIcon(Icon.SHOW_SILIMAR_IMAGES, Icon.Size.SMALL));
    this.showSimilarImagesButton.setOnAction(e -> this.showSimilarImages());

    final HBox fileNameBox = new HBox(this.fileNameLabel);
    fileNameBox.setAlignment(Pos.CENTER);
    fileNameBox.setPadding(new Insets(0, 5, 0, 5));

    final HBox metadataBox = new HBox(this.fileMetadataLabel);
    metadataBox.setAlignment(Pos.CENTER);
    metadataBox.setPadding(new Insets(0, 5, 0, 5));

    this.imageViewBox = new HBox(this.imageView);
    this.imageViewBox.setAlignment(Pos.CENTER);
    this.imageViewBox.setMinHeight(200);
    this.imageViewBox.heightProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewSize());
    this.widthProperty().addListener((observable, oldValue, newValue) -> this.updateImageViewSize());
    this.imageView.setPreserveRatio(true);

    final HBox tagsLabelBox = new HBox(new Label(language.translate("image_preview.section.tags.title")));
    tagsLabelBox.getStyleClass().add("section-title");
    tagsLabelBox.setAlignment(Pos.CENTER);

    this.editTagsButton.setOnAction(e -> this.editTagsListeners.forEach(
        listener -> listener.onEditTags(Objects.requireNonNull(this.picture))));
    this.editTagsButton.setTooltip(new Tooltip(language.translate("image_preview.section.tags.edit_tags_button")));
    this.editTagsButton.setGraphic(theme.getIcon(Icon.EDIT_TAGS, Icon.Size.SMALL));
    this.editTagsButton.setDisable(true);

    HBox.setHgrow(tagsLabelBox, Priority.ALWAYS);
    final HBox tagsTitleBox = new HBox(
        5,
        tagsLabelBox,
        this.editTagsButton,
        this.showSimilarImagesButton,
        this.openInExplorerButton
    );
    tagsTitleBox.setPadding(new Insets(0, 5, 0, 5));

    this.tagsList.setPrefHeight(150);
    this.tagsList.setCellFactory(ignored -> ClickableListCellFactory.forListener(this));
    VBox.setVgrow(this.tagsList, Priority.ALWAYS);

    this.setOrientation(Orientation.VERTICAL);
    this.getItems().addAll(
        this.imageViewBox,
        new VBox(5, fileNameBox, metadataBox, tagsTitleBox, this.tagsList)
    );
    this.setDividerPositions(0.75);
    this.setImage(null, null, false);
  }

  private void updateImageViewSize() {
    final Image image = this.imageView.getImage();
    if (image != null) {
      final double width = Math.min(image.getWidth(), this.getWidth() - 10);
      this.imageView.setFitWidth(width);
      final double height = Math.min(image.getHeight(), this.imageViewBox.getHeight() - 10);
      this.imageView.setFitHeight(height);
    }
  }

  /**
   * Set the image to show.
   *
   * @param picture          The image to show.
   * @param tags             The tags for the image.
   * @param hasSimilarImages Whether the given picture has similar images.
   */
  @Contract("!null, null, _ -> fail")
  public void setImage(Picture picture, Set<Tag> tags, boolean hasSimilarImages) {
    if (picture != null)
      Objects.requireNonNull(tags);
    this.picture = picture;

    this.imageView.setImage(null);
    this.fileNameLabel.setText(null);
    this.fileNameLabel.setTooltip(null);
    this.fileNameLabel.setGraphic(null);
    this.fileMetadataLabel.setText(null);
    this.fileMetadataLabel.setTooltip(null);
    this.editTagsButton.setDisable(picture == null);
    this.tagsList.getItems().clear();

    if (picture != null) {
      final Language language = this.config.language();
      final Path path = picture.path();
      final String fileName = path.getFileName().toString();
      this.fileNameLabel.setText(fileName);
      this.fileNameLabel.setTooltip(new Tooltip(fileName));
      this.fileNameLabel.setGraphic(
          this.config.theme().getIcon(picture.isVideo() ? Icon.VIDEO : Icon.IMAGE, Icon.Size.SMALL));
      boolean exists;
      try {
        exists = Files.exists(path);
      } catch (final SecurityException e) {
        exists = false;
      }
      if (!exists) {
        this.fileMetadataLabel.setText(language.translate("image_preview.missing_file"));
      } else {
        this.fileMetadataLabel.setText(language.translate("image_preview.loading"));
        FileUtils.loadImage(
            path,
            image -> {
              this.imageView.setImage(image);
              final String text = FileUtils.formatImageMetadata(path, image, this.config);
              this.fileMetadataLabel.setText(text);
              this.fileMetadataLabel.setTooltip(new Tooltip(text));
              this.openInExplorerButton.setDisable(false);
              this.updateImageViewSize();
            },
            error -> {
              this.fileMetadataLabel.setText(language.translate("image_preview.missing_file"));
              this.openInExplorerButton.setDisable(true);
            }
        );
      }
      final var tagsEntries = tags.stream()
          .sorted()
          .map(TagView::new)
          .toList();
      this.tagsList.getItems().addAll(tagsEntries);
      this.showSimilarImagesButton.setDisable(!hasSimilarImages);
    } else {
      this.openInExplorerButton.setDisable(true);
      this.showSimilarImagesButton.setDisable(true);
    }
  }

  public void addTagClickListener(@NotNull TagClickListener listener) {
    this.tagClickListeners.add(Objects.requireNonNull(listener));
  }

  public void addEditTagsListener(@NotNull EditTagsListener listener) {
    this.editTagsListeners.add(Objects.requireNonNull(listener));
  }

  private void onOpenFile() {
    if (this.picture != null)
      FileUtils.openInFileExplorer(this.picture.path().toString());
  }

  private void showSimilarImages() {
    this.similarImagesListeners.forEach(l -> l.accept(this.picture));
  }

  @Override
  public void onItemClick(@NotNull TagView item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull TagView item) {
    this.tagClickListeners.forEach(listener -> listener.onTagClick(item.tag()));
  }

  public void addSimilarImagesListeners(@NotNull Consumer<Picture> similarImagesListener) {
    this.similarImagesListeners.add(Objects.requireNonNull(similarImagesListener));
  }

  public interface EditTagsListener {
    void onEditTags(@NotNull Picture picture);
  }
}
