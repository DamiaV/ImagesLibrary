package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.application.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A dialog that shows images.
 */
public class ImageViewerDialog extends DialogBase<Void> {
  private final HBox imageViewBox;
  private final ImageView imageView = new ImageView();

  private final List<Picture> pictures = new LinkedList<>();
  private int index = -1;
  private Thread animationThread;
  private Slideshow slideshow;

  public ImageViewerDialog(@NotNull Config config) {
    super(config, "slideshow", true, ButtonType.CLOSE);

    this.imageViewBox = new HBox(this.imageView);
    this.imageViewBox.setAlignment(Pos.CENTER);
    this.imageView.setPreserveRatio(true);

    final DialogPane dialogPane = this.getDialogPane();
    dialogPane.setContent(this.imageViewBox);
    dialogPane.setStyle("-fx-background-color: black;");
    dialogPane.getStyleClass().remove("dialog-pane"); // Remove default margins, etc.
    dialogPane.getChildren().removeIf(child -> child instanceof ButtonBar); // Remove button bar at the bottom

    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);
    stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.LEFT) {
        this.advancePicture(-1);
        if (this.slideshow != null)
          this.slideshow.resetDelay();
        event.consume();
      } else if (event.getCode() == KeyCode.RIGHT) {
        this.advancePicture(1);
        if (this.slideshow != null)
          this.slideshow.resetDelay();
        event.consume();
      }
    });
    stage.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        this.advancePicture(1);
        event.consume();
      }
    });
    stage.initStyle(StageStyle.UNDECORATED);
    this.getDialogPane().getScene().setCursor(Cursor.NONE);

    this.setOnShowing(event -> stage.setFullScreen(true));
    this.setOnShown(event -> this.startSlideshow());
    this.setOnHiding(event -> this.stopSlideshow());

    this.setResultConverter(buttonType -> null);
  }

  /**
   * Set the pictures to show as a slideshow.
   *
   * @param pictures The pictures to show.
   */
  public void setPictures(final @NotNull List<Picture> pictures) {
    this.pictures.clear();
    this.pictures.addAll(pictures);
    if (this.config.isShuffleSlideshowsEnabled())
      Collections.shuffle(this.pictures);
    this.index = -1;
    if (!pictures.isEmpty())
      this.advancePicture(1);
  }

  private void advancePicture(int direction) {
    this.imageView.setImage(null); // Clear in case a previous image is still loading
    if (direction > 0)
      this.index = (this.index + 1) % this.pictures.size();
    else if (this.index > 0)
      this.index = this.index - 1;
    else
      this.index = this.pictures.size() - 1;
    FileUtils.loadImage(
        this.pictures.get(this.index).path(),
        image -> {
          this.imageView.setImage(image);
          this.imageView.setFitWidth(Math.min(image.getWidth(), this.imageViewBox.getWidth()));
          this.imageView.setFitHeight(Math.min(image.getHeight(), this.imageViewBox.getHeight()));
        },
        e -> {
        }
    );
  }

  private void startSlideshow() {
    final int delay = this.config.slideshowDelay() * 1000;
    this.slideshow = new Slideshow(delay);
    this.animationThread = new Thread(this.slideshow, "Slideshow Thread");
    this.animationThread.start();
  }

  private void stopSlideshow() {
    if (this.animationThread != null) {
      this.animationThread.interrupt();
      this.animationThread = null;
      this.slideshow = null;
    }
  }

  /**
   * This class manages the animation of a slideshow.
   */
  private class Slideshow implements Runnable {
    private final int delay;
    private long prevTime;

    /**
     * Create a new slideshow.
     *
     * @param delay The delay between each image in milliseconds.
     */
    public Slideshow(int delay) {
      this.delay = delay;
    }

    @Override
    public void run() {
      this.prevTime = System.currentTimeMillis();
      final var dialog = ImageViewerDialog.this;
      while (!dialog.animationThread.isInterrupted()) {
        final long time = System.currentTimeMillis();
        if (time - this.prevTime > this.delay) {
          Platform.runLater(() -> dialog.advancePicture(1));
          this.prevTime = time;
        }
      }
    }

    /**
     * Reset the delay before the next picture change.
     */
    public void resetDelay() {
      this.prevTime = System.currentTimeMillis();
    }
  }
}
