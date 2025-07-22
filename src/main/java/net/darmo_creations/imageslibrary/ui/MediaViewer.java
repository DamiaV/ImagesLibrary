package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;
import org.reactfx.util.*;

import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public class MediaViewer extends VBox {
  private final Config config;

  private final ImageView imageView = new ImageView();
  private final VideoPlayer videoPlayer;
  private MediaPlayer mediaPlayer; // Keep a reference to avoid garbage collection
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();

  private Consumer<Either<Image, MediaPlayer>> onLoadedCallback;
  private Consumer<Exception> onLoadErrorCallback;

  public MediaViewer(@NotNull Config config) {
    super(5);
    this.config = Objects.requireNonNull(config);
    this.videoPlayer = new VideoPlayer(config);

    this.imageView.setPreserveRatio(true);

    this.setAlignment(Pos.CENTER);
    this.getChildren().addAll(this.imageView, this.fileNameLabel, this.fileMetadataLabel);
  }

  public Image getImage() {
    return this.imageView.getImage();
  }

  public void setMedia(Picture picture) {
    this.imageView.setImage(null);
    this.videoPlayer.setMediaPlayer(null, true);
    this.mediaPlayer = null;

    if (picture == null) {
      this.fileNameLabel.setText(null);
      this.fileNameLabel.setTooltip(null);
      this.fileNameLabel.setGraphic(null);
      this.fileMetadataLabel.setText(null);
      this.fileMetadataLabel.setTooltip(null);
    } else {
      final Language language = this.config.language();
      final Path path = picture.path();
      final String fileName = path.getFileName().toString();
      this.fileNameLabel.setText(fileName);
      this.fileNameLabel.setTooltip(new Tooltip(fileName));
      final Icon icon = picture.isVideo() ? Icon.VIDEO : Icon.IMAGE;
      this.fileNameLabel.setGraphic(this.config.theme().getIcon(icon, Icon.Size.SMALL));

      boolean exists;
      try {
        exists = Files.exists(path);
      } catch (final SecurityException e) {
        exists = false;
      }

      if (!exists) {
        this.fileMetadataLabel.setText(language.translate("media_viewer.missing_file"));
        return;
      }

      this.fileMetadataLabel.setText(language.translate("media_viewer.loading"));
      if (picture.isVideo() && FileUtils.isSupportedVideoFile(path)) {
        try {
          this.mediaPlayer = FileUtils.loadVideo(
              path,
              mediaPlayer -> {
                final Media media = mediaPlayer.getMedia();
                if (media.getWidth() == 0 || media.getHeight() == 0) {
                  if (this.mediaPlayer != null) {
                    this.mediaPlayer.dispose();
                    this.mediaPlayer = null;
                  }
                  this.loadImage(path);
                } else {
                  this.videoPlayer.setMediaPlayer(mediaPlayer, true);
                  final String metadata = FileUtils.formatVideoMetadata(path, mediaPlayer, this.config);
                  this.updateImageViewBoxContent(true, metadata);
                }
              }
          );
        } catch (final MalformedURLException e) {
          this.onFileLoadingError(e);
        }
      } else this.loadImage(path);
    }
  }

  private void loadImage(@NotNull Path path) {
    FileUtils.loadImage(
        path,
        image -> {
          this.imageView.setImage(image);
          final String metadata = FileUtils.formatImageMetadata(path, image, this.config);
          this.updateImageViewBoxContent(false, metadata);
        },
        this::onFileLoadingError
    );
  }

  private void onFileLoadingError(Exception e) {
    App.logger().error("Error while loading file", e);
    this.fileMetadataLabel.setText(this.config.language().translate("media_viewer.file_loading_error"));
    if (this.onLoadErrorCallback != null)
      this.onLoadErrorCallback.accept(e);
  }

  private void updateImageViewBoxContent(boolean isVideo, @NotNull String metadata) {
    this.fileMetadataLabel.setText(metadata);
    this.fileMetadataLabel.setTooltip(new Tooltip(metadata));
    this.getChildren().set(0, isVideo ? this.videoPlayer : this.imageView);
    if (this.onLoadedCallback != null)
      this.onLoadedCallback.accept(isVideo ? Either.right(this.mediaPlayer) : Either.left(this.imageView.getImage()));
  }

  public void updateImageViewSize(double containerWidth, double containerHeight) {
    final double hOffset = this.fileNameLabel.getHeight() + this.fileMetadataLabel.getHeight() + 2 * this.getSpacing();

    if (this.getChildren().contains(this.imageView)) {
      final Image image = this.imageView.getImage();
      if (image != null) {
        final double width = Math.min(image.getWidth(), containerWidth);
        this.imageView.setFitWidth(width);
        final double height = Math.min(image.getHeight(), containerHeight);
        this.imageView.setFitHeight(height - hOffset);
      }
    } else {
      final Optional<MediaPlayer> mediaPlayer = this.videoPlayer.getMediaPlayer();
      if (mediaPlayer.isPresent()) {
        final Media media = mediaPlayer.get().getMedia();
        final double width = Math.min(media.getWidth(), containerWidth);
        this.videoPlayer.setFitWidth(width);
        final double height = Math.min(media.getHeight(), containerHeight);
        this.videoPlayer.setFitHeight(height - hOffset);
      }
    }
  }

  public void setOnLoadedCallback(Consumer<Either<Image, MediaPlayer>> onLoadedCallback) {
    this.onLoadedCallback = onLoadedCallback;
  }

  public void setOnLoadErrorCallback(Consumer<Exception> onLoadErrorCallback) {
    this.onLoadErrorCallback = onLoadErrorCallback;
  }
}
