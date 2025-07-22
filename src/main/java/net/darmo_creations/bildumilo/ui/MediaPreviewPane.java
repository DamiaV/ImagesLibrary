package net.darmo_creations.bildumilo.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;

public class MediaPreviewPane extends SplitPane implements ClickableListCellFactory.ClickListener<TagView> {
  private final Set<TagClickListener> tagClickListeners = new HashSet<>();
  private final Set<EditTagsListener> editTagsListeners = new HashSet<>();
  private final Set<Consumer<MediaFile>> similarImagesListeners = new HashSet<>();

  private final Button openInExplorerButton = new Button();
  private final Button showSimilarImagesButton = new Button();
  private final HBox mediaViewerBox;
  private final MediaViewer mediaViewer;
  private final Button editTagsButton = new Button();
  private final ListView<TagView> tagsList = new ListView<>();

  @Nullable
  private MediaFile mediaFile;

  public MediaPreviewPane(final @NotNull Config config) {
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
        (observable, oldValue, newValue) -> this.updateMediaViewerSize());
    this.widthProperty().addListener(
        (observable, oldValue, newValue) -> this.updateMediaViewerSize());

    final HBox tagsLabelBox = new HBox(new Label(language.translate("image_preview.section.tags.title")));
    tagsLabelBox.getStyleClass().add("section-title");
    tagsLabelBox.setAlignment(Pos.CENTER);

    this.editTagsButton.setOnAction(e -> this.editTagsListeners.forEach(
        listener -> listener.onEditTags(Objects.requireNonNull(this.mediaFile))));
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

  private void updateMediaViewerSize() {
    this.mediaViewer.updateSize(
        this.getWidth() - 10,
        this.mediaViewerBox.getHeight() - 10
    );
  }

  /**
   * Return the currently loaded {@link MediaFile}.
   */
  public Optional<MediaFile> getMediaFile() {
    return Optional.ofNullable(this.mediaFile);
  }

  /**
   * Set the media to show.
   *
   * @param mediaFile        The media to show.
   * @param tags             The tags for the media.
   * @param hasSimilarImages If the media is an image, whether it has similar images.
   */
  @Contract("!null, null, _ -> fail")
  public void setMedia(MediaFile mediaFile, Set<Tag> tags, boolean hasSimilarImages) {
    if (mediaFile != null)
      Objects.requireNonNull(tags);
    this.mediaFile = mediaFile;

    this.mediaViewer.setMedia(mediaFile);
    this.editTagsButton.setDisable(mediaFile == null);
    this.tagsList.getItems().clear();

    if (mediaFile != null) {
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
    this.updateMediaViewerSize();
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
    if (this.mediaFile != null)
      FileUtils.openInFileExplorer(this.mediaFile.path().toString());
  }

  private void showSimilarImages() {
    this.similarImagesListeners.forEach(l -> l.accept(this.mediaFile));
  }

  @Override
  public void onItemClick(@NotNull TagView item) {
  }

  @Override
  public void onItemDoubleClick(@NotNull TagView item) {
    this.tagClickListeners.forEach(listener -> listener.onTagClick(item.tag()));
  }

  public void addSimilarImagesListeners(@NotNull Consumer<MediaFile> similarImagesListener) {
    this.similarImagesListeners.add(Objects.requireNonNull(similarImagesListener));
  }

  public interface EditTagsListener {
    void onEditTags(@NotNull MediaFile mediaFile);
  }
}
