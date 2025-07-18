package net.darmo_creations.imageslibrary.ui;

import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.themes.*;

import java.util.*;

public class VideoPlayer extends VBox {
  private final Config config;

  private final MediaView mediaView = new MediaView();
  private final Button playButton = new Button();
  private final Button stopButton = new Button();
  private final Button loopButton = new Button();
  private final Slider progressSlider = new Slider();
  private final Label progressLabel = new Label();
  private final Button toggleSoundButton = new Button();
  private final Slider soundSlider = new Slider();
  private final HBox controlsBox;

  private boolean isPausedWhileAdjusting;
  private boolean isManuallyAdjustingTime;
  private boolean hasSound;
  private double volumeBeforeMute;
  private final DoubleProperty volume = new SimpleDoubleProperty(this, "volume", -1);
  private boolean loop;
  private ChangeListener<Number> progressListener;

  public VideoPlayer(Config config) {
    this.config = config;

    this.setAlignment(Pos.CENTER);
    this.getStyleClass().add("video-player");

    this.mediaView.setPreserveRatio(true);
    this.mediaView.setOnMouseClicked(event -> this.togglePlay());

    this.playButton.setOnAction(event -> this.togglePlay());
    this.playButton.setPadding(Insets.EMPTY);

    this.stopButton.setTooltip(new Tooltip(this.config.language().translate("video_player.controls.stop")));
    this.stopButton.setGraphic(this.config.theme().getIcon(Icon.CONTROL_STOP, Icon.Size.BIG));
    this.stopButton.setOnAction(event -> this.stop());
    this.stopButton.setPadding(Insets.EMPTY);

    this.progressSlider.setMin(0);
    this.progressSlider.setOnMouseClicked(event -> this.onProgressBarClicked());
    this.progressSlider.setOnMousePressed(event -> this.onProgressBarPressed());
    this.progressSlider.setOnMouseReleased(event -> this.onProgressBarReleased());
    HBox.setHgrow(this.progressSlider, Priority.ALWAYS);

    this.loopButton.setOnAction(event -> this.toggleRepeat(null));
    this.loopButton.setPadding(Insets.EMPTY);

    this.toggleSoundButton.setOnAction(event -> this.toggleSound());
    this.toggleSoundButton.setPadding(Insets.EMPTY);

    this.soundSlider.setPrefWidth(30);
    this.soundSlider.setMin(0);
    this.soundSlider.setMax(1);
    this.soundSlider.valueProperty()
        .addListener((observable, oldValue, newValue) -> this.updateControls());

    this.controlsBox = new HBox(
        5,
        this.playButton,
        this.stopButton,
        this.progressSlider,
        this.progressLabel,
        this.loopButton,
        this.toggleSoundButton,
        this.soundSlider
    );
    this.controlsBox.getStyleClass().add("video-player-controls");
    this.controlsBox.setPadding(new Insets(3));
    this.controlsBox.setAlignment(Pos.CENTER);

    this.getChildren().addAll(this.mediaView, this.controlsBox);

    this.updateControls();
  }

  private void togglePlay() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) mediaPlayer.pause();
    else mediaPlayer.play();
  }

  private void stop() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    mediaPlayer.stop();
  }

  private void toggleRepeat(Boolean repeat) {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    final int inf = MediaPlayer.INDEFINITE;
    final int cycleCount;
    if (repeat != null) cycleCount = repeat ? inf : 1;
    else cycleCount = mediaPlayer.getCycleCount() == inf ? 1 : inf;
    mediaPlayer.setCycleCount(cycleCount);

    this.loop = cycleCount == inf;
    this.updateControls();
  }

  private void toggleSound() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    if (mediaPlayer.getVolume() != 0) {
      this.volumeBeforeMute = mediaPlayer.getVolume();
      mediaPlayer.setVolume(0);
    } else mediaPlayer.setVolume(this.volumeBeforeMute);
    this.updateControls();
  }

  public Optional<MediaPlayer> getMediaPlayer() {
    return Optional.ofNullable(this.mediaView.getMediaPlayer());
  }

  /**
   * Set the {@link MediaPlayer} that should be played.
   *
   * @param mediaPlayer     The {@link MediaPlayer} to play.
   * @param disposePrevious If true, any {@link MediaPlayer} that was previously passed will be disposed of.
   */
  public void setMediaPlayer(MediaPlayer mediaPlayer, boolean disposePrevious) {
    this.volume.unbind();
    if (this.progressListener != null)
      this.progressSlider.valueProperty().removeListener(this.progressListener);
    if (disposePrevious && this.mediaView.getMediaPlayer() != null)
      this.mediaView.getMediaPlayer().dispose();

    this.mediaView.setMediaPlayer(mediaPlayer);

    if (mediaPlayer != null) {
      this.hasSound = mediaPlayer.getMedia()
          .getTracks()
          .stream()
          .anyMatch(track -> track instanceof AudioTrack);
      this.soundSlider.valueProperty().bindBidirectional(mediaPlayer.volumeProperty());
      if (this.volume.get() >= 0) mediaPlayer.setVolume(this.volume.getValue());
      this.volume.bind(mediaPlayer.volumeProperty());

      this.progressSlider.setMax(mediaPlayer.getTotalDuration().toMillis());
      this.progressSlider.setValue(0);
      // Need to keep track of this as it references a MediaPlayer object that may be disposed of
      this.progressListener = (observable, oldValue, newValue) -> {
        if (this.isManuallyAdjustingTime)
          mediaPlayer.seek(Duration.millis(newValue.longValue()));
      };
      this.progressSlider.valueProperty().addListener(this.progressListener);
      mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
        if (!this.isManuallyAdjustingTime)
          this.progressSlider.setValue(newValue.toMillis());
      });

      this.toggleRepeat(this.loop);

      mediaPlayer.setOnPlaying(this::updateControls);
      mediaPlayer.setOnPaused(this::updateControls);
      mediaPlayer.setOnStopped(() -> {
        mediaPlayer.pause(); // Exit from STOPPED status as it causes issues with controls
        mediaPlayer.seek(Duration.ZERO);
        // Small hack to circumvent currentTimeProperty() not being updtated at this stage
        this.progressSlider.setValue(0);
        this.updateControls();
      });
      mediaPlayer.setOnEndOfMedia(() -> {
        if (mediaPlayer.getCycleCount() == MediaPlayer.INDEFINITE) {
          mediaPlayer.seek(Duration.ZERO);
          this.updateControls();
        } else mediaPlayer.stop();
      });

      final Duration totalDuration = mediaPlayer.getTotalDuration();
      final int totalHours = (int) totalDuration.toHours();
      final int totalMinutes = (int) (totalDuration.toMinutes() % 60);
      final int totalSeconds = (int) (totalDuration.toSeconds() % 60);
      final String totalTime = totalHours != 0
          ? "%02d:%02d:%02d".formatted(totalHours, totalMinutes, totalSeconds)
          : "%02d:%02d".formatted(totalMinutes, totalSeconds);
      // FIXME not triggered when the progress bar is clicked or dragged after the media was stopped
      mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
        final int hours = (int) newValue.toHours();
        final int minutes = (int) (newValue.toMinutes() % 60);
        final int seconds = (int) (newValue.toSeconds() % 60);
        final String time = totalHours != 0
            ? "%02d:%02d:%02d".formatted(hours, minutes, seconds)
            : "%02d:%02d".formatted(minutes, seconds);
        this.progressLabel.setText(time + " / " + totalTime);
      });
    } else {
      this.hasSound = false;
      this.progressSlider.setValue(0);
      this.progressLabel.setText("--:-- / --:--");
    }
    this.updateControls();
  }

  private void onProgressBarClicked() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    mediaPlayer.seek(Duration.millis(this.progressSlider.getValue()));
  }

  private void onProgressBarPressed() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    this.isManuallyAdjustingTime = true;
    final var status = mediaPlayer.getStatus();
    if (status == MediaPlayer.Status.PLAYING) {
      this.isPausedWhileAdjusting = true;
      mediaPlayer.pause();
    }
  }

  private void onProgressBarReleased() {
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();
    if (mediaPlayer == null) return;

    this.isManuallyAdjustingTime = false;
    if (this.isPausedWhileAdjusting) {
      this.isPausedWhileAdjusting = false;
      mediaPlayer.play();
    }
  }

  private void updateControls() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();
    final MediaPlayer mediaPlayer = this.mediaView.getMediaPlayer();

    final boolean disable = mediaPlayer == null;

    final boolean isPlaying = !disable && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    this.playButton.setTooltip(new Tooltip(language.translate("video_player.controls." + (isPlaying ? "pause" : "play"))));
    this.playButton.setGraphic(theme.getIcon(isPlaying ? Icon.CONTROL_PAUSE : Icon.CONTROL_PLAY, Icon.Size.BIG));

    final boolean isRepeat = !disable && mediaPlayer.getCycleCount() == MediaPlayer.INDEFINITE;
    this.loopButton.setTooltip(new Tooltip(language.translate("video_player.controls.loop." + (isRepeat ? "enabled" : "disabled"))));
    this.loopButton.setGraphic(theme.getIcon(isRepeat ? Icon.CONTROL_LOOP_ON : Icon.CONTROL_LOOP_OFF, Icon.Size.BIG));

    final boolean isSoundOn = !disable && this.hasSound && mediaPlayer.getVolume() != 0;
    this.toggleSoundButton.setTooltip(new Tooltip(language.translate("video_player.controls.sound." + (isSoundOn ? "on" : "off"))));
    final Icon soundIcon;
    if (isSoundOn) {
      if (mediaPlayer.getVolume() > 0.66) soundIcon = Icon.CONTROL_SOUND_ON;
      else if (mediaPlayer.getVolume() > 0.33) soundIcon = Icon.CONTROL_SOUND_LOWER;
      else soundIcon = Icon.CONTROL_SOUND_LOWEST;
    } else soundIcon = Icon.CONTROL_SOUND_OFF;
    this.toggleSoundButton.setGraphic(theme.getIcon(soundIcon, Icon.Size.BIG));

    this.playButton.setDisable(disable);
    this.stopButton.setDisable(disable);
    this.loopButton.setDisable(disable);
    this.progressSlider.setDisable(disable);
    this.toggleSoundButton.setDisable(disable || !this.hasSound);
    this.soundSlider.setDisable(disable || !this.hasSound);
  }

  public void setFitWidth(double width) {
    this.mediaView.setFitWidth(width);
  }

  public void setFitHeight(double height) {
    this.mediaView.setFitHeight(height - this.controlsBox.getHeight());
  }
}
