package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;

public class ImagePreviewPane extends SplitPane implements ClickableListCellFactory.ClickListener<TagView> {
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<EditTagsListener> editTagsListeners = new HashSet<>();
  private final Set<Consumer<Picture>> similarImagesListeners = new HashSet<>();

  private final Button openInExplorerButton = new Button();
  private final Button showSimilarImagesButton = new Button();
  private final HBox mediaViewerBox;
  private final MediaViewer mediaViewer;
  private final Button editTagsButton = new Button();
  private final ListView<TagView> tagsList = new ListView<>();

  @Nullable
  private Picture picture;

  public ImagePreviewPane(final @NotNull Config config) {
    this.setMinWidth(300);

    final Language language = config.language();
    final Theme theme = config.theme();

    this.openInExplorerButton.setTooltip(new Tooltip(language.translate("image_preview.open_in_explorer_button.label")));
    this.openInExplorerButton.setGraphic(theme.getIcon(Icon.OPEN_FILE_IN_EXPLORER, Icon.Size.SMALL));
    this.openInExplorerButton.setOnAction(e -> this.onOpenFile());

    this.showSimilarImagesButton.setTooltip(new Tooltip(language.translate("image_preview.show_similar_images_button.label")));
    this.showSimilarImagesButton.setGraphic(theme.getIcon(Icon.SHOW_SILIMAR_IMAGES, Icon.Size.SMALL));
    this.showSimilarImagesButton.setOnAction(e -> this.showSimilarImages());

    this.mediaViewer = new MediaViewer(config);
    this.mediaViewer.setOnLoadedCallback(ignored -> this.onMediaLoaded());
    this.mediaViewer.setOnLoadErrorCallback(this::onFileLoadingError);

    this.mediaViewerBox = new HBox(this.mediaViewer);
    this.mediaViewerBox.setAlignment(Pos.CENTER);
    this.mediaViewerBox.setMinHeight(200);
    this.mediaViewerBox.heightProperty().addListener(
        (observable, oldValue, newValue) -> this.updateImageViewSize());
    this.widthProperty().addListener(
        (observable, oldValue, newValue) -> this.updateImageViewSize());

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
        this.mediaViewerBox,
        new VBox(5, tagsTitleBox, this.tagsList)
    );
    this.setDividerPositions(0.75);
    this.setMedia(null, null, false);
  }

  private void updateImageViewSize() {
    this.mediaViewer.updateImageViewSize(
        this.getWidth() - 10,
        this.mediaViewerBox.getHeight() - 10
    );
  }

  /**
   * Return the currently loaded {@link Picture}.
   */
  public Optional<Picture> getImage() {
    return Optional.ofNullable(this.picture);
  }

  /**
   * Set the image to show.
   *
   * @param picture          The image to show.
   * @param tags             The tags for the image.
   * @param hasSimilarImages Whether the given picture has similar images.
   */
  @Contract("!null, null, _ -> fail")
  public void setMedia(Picture picture, Set<Tag> tags, boolean hasSimilarImages) {
    if (picture != null)
      Objects.requireNonNull(tags);
    this.picture = picture;

    this.mediaViewer.setMedia(picture);
    this.editTagsButton.setDisable(picture == null);
    this.tagsList.getItems().clear();

    if (picture != null) {
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

  private void onMediaLoaded() {
    this.openInExplorerButton.setDisable(false);
    this.updateImageViewSize();
  }

  private void onFileLoadingError(Exception error) {
    this.openInExplorerButton.setDisable(true);
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
