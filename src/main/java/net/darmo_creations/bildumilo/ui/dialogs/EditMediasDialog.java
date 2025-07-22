package net.darmo_creations.bildumilo.ui.dialogs;

import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.ui.*;
import net.darmo_creations.bildumilo.ui.syntax_highlighting.*;
import net.darmo_creations.bildumilo.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Dialog to create/edit medias.
 */
public class EditMediasDialog extends DialogBase<Boolean> {
  private final HBox mediaViewerBox;
  private final MediaViewer mediaViewer;
  private final TextField fileNameField = new TextField();
  private final Button viewSimilarImagesButton = new Button();
  private final Button moveButton = new Button();
  private final Button showInExplorerButton = new Button();
  private final Button clearPathButton = new Button();
  private final CheckBox overwriteTargetCheckBox = new CheckBox();
  private final Label targetPathLabel = new Label();
  private final HBox pathBox = new HBox(5);
  private final TextPopOver tagsErrorPopup;
  private final AutoCompleteField<Tag, String> tagsField;
  private final Button nextButton;
  private final Button skipButton;
  private final Button finishButton;

  private final SimilarImagesDialog similarImagesDialog;

  private boolean areTagsValid = false;

  private final DatabaseConnection db;

  private int totalMedias;
  private final Queue<MediaFile> mediaFiles = new LinkedList<>();
  @Nullable
  private Hash computedHash;
  private final Set<Tag> currentMediaTags = new HashSet<>();
  private Path targetPath;
  private MediaFile currentMediaFile;
  private boolean insert;
  private boolean anyUpdate;
  private boolean preventClosing;

  public EditMediasDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "edit_images", true, ButtonTypes.FINISH, ButtonTypes.SKIP, ButtonTypes.NEXT, ButtonTypes.CANCEL);
    this.db = Objects.requireNonNull(db);

    this.mediaViewer = new MediaViewer(config);
    this.mediaViewer.setOnLoadedCallback(ignored -> this.updateMediaViewerSize());

    this.similarImagesDialog = new SimilarImagesDialog(config, db);
    this.similarImagesDialog.addTagCopyListener(this::onCopyTags);

    this.tagsErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.tagsField = new AutoCompleteField<>(
        new InlineCssTextArea(),
        this.db.getAllTags(),
        t -> t.definition().isEmpty(),
        Tag::label,
        new TagListSyntaxHighlighter(db.getAllTags(), db.getAllTagTypes()),
        Function.identity(),
        config
    );

    this.nextButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.NEXT);
    this.nextButton.addEventFilter(ActionEvent.ACTION, event -> {
      if (this.applyChanges())
        this.nextMedia();
      event.consume();
    });
    this.skipButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.SKIP);
    this.skipButton.addEventFilter(ActionEvent.ACTION, event -> {
      this.nextMedia();
      event.consume();
    });
    this.finishButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.FINISH);

    this.mediaViewerBox = new HBox(this.mediaViewer);
    this.getDialogPane().setContent(this.createContent());

    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton())
        this.preventClosing = !this.applyChanges();
      return this.anyUpdate;
    });

    this.setOnCloseRequest(event -> {
      if (this.preventClosing) {
        event.consume();
        this.preventClosing = false;
      } else {
        // Hide the similar images dialog when this one closes
        this.similarImagesDialog.hide();
        this.mediaViewer.setMedia(null); // Dispose of any loaded MediaPlayer
      }
    });
  }

  private SplitPane createContent() {
    final Language language = this.config.language();

    this.mediaViewerBox.setAlignment(Pos.CENTER);
    this.mediaViewerBox.setMinHeight(200);
    this.mediaViewerBox.heightProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewerSize());
    this.stage().widthProperty().addListener((observable, oldValue, newValue) -> this.updateMediaViewerSize());

    this.fileNameField.textProperty().addListener((observable, oldValue, newValue) -> this.updateState());
    HBox.setHgrow(this.fileNameField, Priority.ALWAYS);
    final HBox fileNameBox = new HBox(
        5,
        new Label(language.translate("dialog.edit_images.file_name")),
        this.fileNameField
    );
    fileNameBox.setAlignment(Pos.CENTER_LEFT);

    this.viewSimilarImagesButton.setText(language.translate("dialog.edit_images.view_similar_images"));
    this.viewSimilarImagesButton.setGraphic(this.config.theme().getIcon(Icon.SHOW_SILIMAR_IMAGES, Icon.Size.SMALL));
    this.viewSimilarImagesButton.setOnAction(event -> this.onViewSimilarAction());

    this.moveButton.setText(language.translate("dialog.edit_images.move_image"));
    this.moveButton.setGraphic(this.config.theme().getIcon(Icon.MOVE_MEDIAS, Icon.Size.SMALL));
    this.moveButton.setOnAction(event -> this.onMoveAction());

    this.showInExplorerButton.setText(language.translate("dialog.edit_images.show_in_explorer"));
    this.showInExplorerButton.setGraphic(this.config.theme().getIcon(Icon.OPEN_FILE_IN_EXPLORER, Icon.Size.SMALL));
    this.showInExplorerButton.setOnAction(event -> this.onOpenFileAction());

    this.clearPathButton.setText(language.translate("dialog.edit_images.clear_path"));
    this.clearPathButton.setGraphic(this.config.theme().getIcon(Icon.CLEAR_TEXT, Icon.Size.SMALL));
    this.clearPathButton.setOnAction(event -> this.clearTargetPath());
    this.clearPathButton.setDisable(true);

    this.overwriteTargetCheckBox.setText(language.translate("dialog.edit_images.overwrite_target"));
    this.overwriteTargetCheckBox.setDisable(true);

    final HBox buttonsBox = new HBox(
        5,
        this.viewSimilarImagesButton,
        this.showInExplorerButton,
        this.moveButton,
        this.clearPathButton,
        this.overwriteTargetCheckBox
    );
    buttonsBox.setAlignment(Pos.CENTER);
    buttonsBox.setPadding(new Insets(0, 5, 0, 5));

    this.pathBox.setAlignment(Pos.CENTER);
    this.pathBox.setPadding(new Insets(0, 5, 0, 5));
    this.pathBox.managedProperty().bind(this.pathBox.visibleProperty());
    this.pathBox.getChildren().addAll(
        new Label(language.translate("dialog.edit_images.target_directory.label")),
        this.targetPathLabel
    );
    this.pathBox.setVisible(false);

    this.tagsField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.tagsField.getStyleClass();
      if (!newValue.isBlank()) {
        this.areTagsValid = false;
        try {
          JavaFxUtils.parseTags(this.tagsField, this.db);
          this.areTagsValid = true;
        } catch (final TagParseException e) {
          this.tagsErrorPopup.setText(language.translate(e.translationKey(), e.formatArgs()));
        }
      } else {
        this.areTagsValid = false;
        this.tagsErrorPopup.setText(language.translate("dialog.edit_images.no_tags"));
      }

      if (!this.areTagsValid) {
        this.showTagsErrorPopup();
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
      } else {
        this.tagsErrorPopup.hide();
        styleClass.remove("invalid");
      }
      this.updateState();
    });
    this.tagsField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER && event.isShortcutDown()) {
        if (!this.nextButton.isDisabled())
          this.nextButton.fire();
        else
          this.finishButton.fire();
        event.consume();
      }
    });
    VBox.setVgrow(this.tagsField, Priority.ALWAYS);

    final SplitPane splitPane = new SplitPane(
        this.mediaViewerBox,
        new VBox(5, buttonsBox, this.pathBox, fileNameBox, this.tagsField)
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    splitPane.setPrefWidth(800);
    splitPane.setPrefHeight(600);
    return splitPane;
  }

  @Override
  protected List<FormatArg> getTitleFormatArgs() {
    return List.of(
        new FormatArg("count", this.totalMedias - (this.mediaFiles != null ? this.mediaFiles.size() : 0)),
        new FormatArg("total", this.totalMedias)
    );
  }

  private void showTagsErrorPopup() {
    if (this.getDialogPane().getScene() != null && this.stage().isShowing()) // Fix an NPE that sometimes occur
      this.tagsErrorPopup.show(this.tagsField);
  }

  private void onCopyTags(final @NotNull Set<Tag> tags) {
    final var joiner = new StringJoiner(" ");
    tags.stream()
        .map(Tag::label)
        .sorted()
        .forEach(joiner::add);
    this.tagsField.setText(joiner.toString());
  }

  /**
   * Set the list of medias to insert or edit.
   * If inserting, this dialog will compute each media’s hash.
   *
   * @param mediaFiles The medias to insert/edit.
   * @param insert     If true the medias will be inserted, otherwise they will be updated.
   */
  public void setMedias(final @NotNull Collection<MediaFile> mediaFiles, boolean insert) {
    if (mediaFiles.isEmpty())
      throw new IllegalArgumentException("mediaFiles must not be empty");
    this.insert = insert;
    this.name = insert ? "insert_images" : "edit_images";
    this.refreshTitle();
    this.mediaFiles.clear();
    this.mediaFiles.addAll(mediaFiles);
    this.totalMedias = mediaFiles.size();
    this.currentMediaTags.clear();
    this.tagsField.setText("");
    this.anyUpdate = false;
    this.clearTargetPath();
    this.nextMedia();
  }

  private void updateMediaViewerSize() {
    this.mediaViewer.updateSize(
        this.stage().getWidth() - 20,
        this.mediaViewerBox.getHeight() - 10
    );
  }

  /**
   * Pop the next media from the queue and reset the edit form with the media’s data.
   */
  private void nextMedia() {
    if (this.mediaFiles.isEmpty())
      return;
    this.currentMediaFile = this.mediaFiles.poll();
    if (!this.insert) // Get all tage from the new current media
      try {
        this.currentMediaTags.clear();
        this.currentMediaTags.addAll(this.db.getMediaTags(this.currentMediaFile));
      } catch (final DatabaseOperationException e) {
        Alerts.error(this.config, "dialog.edit_images.tags_querying_error.header", null, null);
      }
    this.mediaViewer.setMedia(this.currentMediaFile);
    this.fileNameField.setText(this.currentMediaFile.path().getFileName().toString());

    final var joiner = new StringJoiner(" ");
    if (!this.insert) {
      this.currentMediaTags.stream()
          .map(Tag::label)
          .sorted()
          .forEach(joiner::add);
      this.tagsField.setText(joiner.toString());
    } else
      this.tagsField.refreshHighlighting();
    this.updateState();
    List<Pair<MediaFile, Float>> similarImages = List.of();
    final Optional<Hash> hash = this.currentMediaFile.hash()
        // Try to compute missing hash
        .or(() -> {
          final Optional<Hash> h = Hash.computeForFile(this.currentMediaFile.path());
          this.computedHash = h.orElse(null);
          return h;
        });
    if (hash.isPresent())
      try {
        similarImages = this.db.getSimilarImages(hash.get(), this.currentMediaFile);
      } catch (final DatabaseOperationException e) {
        App.logger().error("Error fetching similar images", e);
      }
    this.viewSimilarImagesButton.setDisable(similarImages.isEmpty());
    this.similarImagesDialog.setMedias(similarImages);
    this.refreshTitle();
  }

  private boolean applyChanges() {
    final Optional<MediaFileUpdate> update = this.getMediaFileUpdate(true);
    if (update.isPresent()) {
      if (this.insert)
        try {
          this.currentMediaFile = this.db.insertMedia(update.get());
          this.anyUpdate = true;
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
          return false;
        }
      else
        try {
          this.db.updateMedia(update.get());
          this.anyUpdate = true;
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
          return false;
        }
    }

    //noinspection OptionalGetWithoutIsPresent
    final String name = Objects.requireNonNull(StringUtils.stripNullable(this.fileNameField.getText()).get());
    Path targetPath = this.targetPath;
    if (targetPath == null)
      targetPath = this.currentMediaFile.path().getParent();
    targetPath = targetPath.resolve(name);

    if (!targetPath.equals(this.currentMediaFile.path()))
      try {
        this.db.moveOrRenameMedia(this.currentMediaFile, targetPath, this.overwriteTargetCheckBox.isSelected());
        this.anyUpdate = true;
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
        return false;
      }

    return true;
  }

  private Optional<MediaFileUpdate> getMediaFileUpdate(boolean recomputeHash) {
    if (!this.areTagsValid)
      return Optional.empty();

    final Set<ParsedTag> parsedTags;
    try {
      parsedTags = JavaFxUtils.parseTags(this.tagsField, this.db);
    } catch (final TagParseException e) {
      return Optional.empty();
    }
    final var toAdd = parsedTags.stream()
        // Parsed tags that are not in the current tags
        .filter(parsedTag -> this.currentMediaTags.stream().noneMatch(tag -> tag.label().equals(parsedTag.label())))
        .map(parsedTag -> new ParsedTag(parsedTag.tagType(), parsedTag.label()))
        .collect(Collectors.toSet());
    final var toRemove = this.currentMediaTags.stream()
        // Current tags that are not in the parsed tags
        .filter(tag -> parsedTags.stream().noneMatch(pair -> pair.label().equals(tag.label())))
        .collect(Collectors.toSet());

    final Optional<String> name = StringUtils.stripNullable(this.fileNameField.getText());
    if (name.isEmpty() || !FileUtils.isValidFile(Path.of(name.get())))
      return Optional.empty();

    Optional<Hash> hash = Optional.ofNullable(this.computedHash);
    if (recomputeHash && hash.isEmpty())
      hash = Hash.computeForFile(this.currentMediaFile.path());

    return Optional.of(new MediaFileUpdate(
        this.insert ? 0 : this.currentMediaFile.id(),
        this.currentMediaFile.path(),
        hash,
        toAdd,
        toRemove
    ));
  }

  private void onViewSimilarAction() {
    if (!this.similarImagesDialog.isShowing())
      this.similarImagesDialog.show();
  }

  private void onMoveAction() {
    final Optional<Path> path = FileChoosers.showDirectoryChooser(this.config, this.stage());
    this.pathBox.setVisible(path.isPresent());
    if (path.isEmpty())
      return;
    this.targetPath = path.get();
    this.targetPathLabel.setText(this.targetPath.toString());
    this.clearPathButton.setDisable(false);
    this.overwriteTargetCheckBox.setDisable(false);
  }

  private void onOpenFileAction() {
    if (this.currentMediaFile != null)
      FileUtils.openInFileExplorer(this.currentMediaFile.path().toString());
  }

  private void clearTargetPath() {
    this.pathBox.setVisible(false);
    this.targetPath = null;
    this.targetPathLabel.setText(null);
    this.clearPathButton.setDisable(true);
    this.overwriteTargetCheckBox.setDisable(true);
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  private void updateState() {
    final boolean noneRemaining = this.mediaFiles.isEmpty();
    final boolean invalid = this.getMediaFileUpdate(false).isEmpty();
    this.nextButton.setDisable(noneRemaining || invalid);
    this.skipButton.setDisable(noneRemaining);
    this.finishButton.setDisable(!noneRemaining || invalid);
  }
}
