package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.themes.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Dialog to create/edit images.
 */
public class EditImagesDialog extends DialogBase<Boolean> {
  private final ImageView imageView = new ImageView();
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final Button viewSimilarImagesButton = new Button();
  private final Button moveButton = new Button();
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
  // Local caches to avoid creating unnecessary new set views.
  private final Set<TagType> tagTypes;
  private final Set<Tag> allTags;

  private final Queue<Picture> pictures = new LinkedList<>();
  private final Set<Tag> currentPictureTags = new HashSet<>();
  private Path targetPath;
  private Picture currentPicture;
  private boolean insert;
  private boolean anyUpdate;
  private boolean preventClosing;

  public EditImagesDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "edit_images", true, ButtonTypes.FINISH, ButtonTypes.SKIP, ButtonTypes.NEXT, ButtonTypes.CANCEL);
    this.db = db;
    this.tagTypes = db.getAllTagTypes();
    this.allTags = db.getAllTags();

    this.similarImagesDialog = new SimilarImagesDialog(config);

    this.tagsErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.tagsField = new AutoCompleteField<>(
        new InlineCssTextArea(),
        this.db.getAllTags(),
        Tag::label,
        new TagListSyntaxHighlighter(this.allTags, this.tagTypes),
        Function.identity()
    );

    this.nextButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.NEXT);
    this.nextButton.addEventFilter(ActionEvent.ACTION, event -> {
      if (this.applyChanges())
        this.nextPicture();
      event.consume();
    });
    this.skipButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.SKIP);
    this.skipButton.addEventFilter(ActionEvent.ACTION, event -> {
      this.nextPicture();
      event.consume();
    });
    this.finishButton = (Button) this.getDialogPane().lookupButton(ButtonTypes.FINISH);

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
      // Hide the similar images dialog when this one closes
      this.similarImagesDialog.hide();
      if (this.preventClosing) {
        event.consume();
        this.preventClosing = false;
      }
    });
  }

  private Node createContent() {
    final Language language = this.config.language();

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

    this.viewSimilarImagesButton.setText(language.translate("dialog.edit_images.view_similar_images"));
    this.viewSimilarImagesButton.setGraphic(this.config.theme().getIcon(Icon.SHOW_SILIMAR_IMAGES, Icon.Size.SMALL));
    this.viewSimilarImagesButton.setOnAction(event -> this.onViewSimilarAction());

    this.moveButton.setText(language.translate("dialog.edit_images.move_image"));
    this.moveButton.setGraphic(this.config.theme().getIcon(Icon.MOVE_IMAGES, Icon.Size.SMALL));
    this.moveButton.setOnAction(event -> this.onMoveAction());

    this.clearPathButton.setText(language.translate("dialog.edit_images.clear_path"));
    this.clearPathButton.setGraphic(this.config.theme().getIcon(Icon.CLEAR_TEXT, Icon.Size.SMALL));
    this.clearPathButton.setOnAction(event -> this.clearTargetPath());
    this.clearPathButton.setDisable(true);

    this.overwriteTargetCheckBox.setText(language.translate("dialog.edit_images.overwrite_target"));
    this.overwriteTargetCheckBox.setDisable(true);

    final HBox buttonsBox = new HBox(
        5,
        this.viewSimilarImagesButton,
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
          this.parseTags();
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
      if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
        if (!this.nextButton.isDisabled())
          this.nextButton.fire();
        else
          this.finishButton.fire();
        event.consume();
      }
    });
    VBox.setVgrow(this.tagsField, Priority.ALWAYS);

    final SplitPane splitPane = new SplitPane(
        imageViewBox,
        new VBox(5, fileNameBox, metadataBox, buttonsBox, this.pathBox, this.tagsField)
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    splitPane.setPrefWidth(800);
    splitPane.setPrefHeight(600);
    return splitPane;
  }

  private void showTagsErrorPopup() {
    if (this.getDialogPane().getScene() != null && this.stage().isShowing()) // Fix an NPE that sometimes occur
      this.tagsErrorPopup.show(this.tagsField);
  }

  /**
   * Set the list of pictures to insert or edit.
   * If inserting, this dialog will compute each picture’s hash.
   *
   * @param pictures The pictures to insert/edit.
   * @param insert   If true the pictures will be inserted, otherwise they will be updated.
   */
  public void setPictures(final @NotNull Collection<Picture> pictures, boolean insert) {
    if (pictures.isEmpty())
      throw new IllegalArgumentException("pictures must not be empty");
    this.insert = insert;
    this.name = insert ? "insert_images" : "edit_images";
    this.refreshTitle();
    this.pictures.clear();
    this.pictures.addAll(pictures);
    this.anyUpdate = false;
    this.clearTargetPath();
    this.nextPicture();
  }

  /**
   * Pop the next picture from the queue and reset the edit form with the image’s data.
   */
  private void nextPicture() {
    if (this.pictures.isEmpty())
      return;
    this.currentPicture = this.pictures.poll();
    if (!this.insert)
      try {
        this.currentPictureTags.clear();
        this.currentPictureTags.addAll(this.db.getImageTags(this.currentPicture));
      } catch (final DatabaseOperationException e) {
        Alerts.error(this.config, "dialog.edit_images.tags_querying_error.header", null, null);
      }
    this.imageView.setImage(null);
    final Path path = this.currentPicture.path();
    final String fileName = path.getFileName().toString();
    this.fileNameLabel.setText(fileName);
    this.fileNameLabel.setTooltip(new Tooltip(fileName));
    this.fileMetadataLabel.setText(null);
    this.fileMetadataLabel.setTooltip(null);
    final Language language = this.config.language();
    if (!Files.exists(path)) {
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
          },
          error -> this.fileMetadataLabel.setText(language.translate("image_preview.missing_file"))
      );
    }
    final var joiner = new StringJoiner(" ");
    if (!this.insert) {
      this.currentPictureTags.forEach(tag -> {
        String t = tag.label();
        if (tag.type().isPresent())
          t = tag.type().get().symbol() + t;
        joiner.add(t);
      });
      this.tagsField.setText(joiner.toString());
    }
    this.updateState();
    List<Pair<Picture, Float>> similarPictures;
    try {
      similarPictures = this.db.getSimilarImages(this.currentPicture.hash(), this.currentPicture);
    } catch (final DatabaseOperationException e) {
      App.logger().error("Error fetching similar pictures", e);
      similarPictures = List.of();
    }
    this.viewSimilarImagesButton.setDisable(similarPictures.isEmpty());
    this.similarImagesDialog.setPictures(similarPictures);
  }

  private boolean applyChanges() {
    final Optional<PictureUpdate> update = this.getPictureUpdate(true);
    if (update.isPresent()) {
      if (this.insert)
        try {
          this.currentPicture = this.db.insertPicture(update.get());
          this.anyUpdate = true;
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
          return false;
        }
      else
        try {
          this.db.updatePicture(update.get());
          this.anyUpdate = true;
        } catch (final DatabaseOperationException e) {
          Alerts.databaseError(this.config, e.errorCode());
          return false;
        }
    }

    if (this.targetPath != null && !this.targetPath.equals(this.currentPicture.path()))
      try {
        this.db.movePicture(this.currentPicture, this.targetPath, this.overwriteTargetCheckBox.isSelected());
        this.anyUpdate = true;
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
        return false;
      }

    return true;
  }

  private Optional<PictureUpdate> getPictureUpdate(boolean recomputeHash) {
    if (!this.areTagsValid)
      return Optional.empty();

    final Set<Pair<Optional<TagType>, String>> parsedTags;
    try {
      parsedTags = this.parseTags();
    } catch (final TagParseException e) {
      return Optional.empty();
    }
    final var toAdd = parsedTags.stream()
        // Parsed tags that are not in the current tags
        .filter(pair -> this.currentPictureTags.stream().noneMatch(tag -> tag.label().equals(pair.getValue())))
        .map(pair -> new Pair<>(pair.getKey().orElse(null), pair.getValue()))
        .collect(Collectors.toSet());
    final var toRemove = this.currentPictureTags.stream()
        // Current tags that are not in the parsed tags
        .filter(tag -> parsedTags.stream().noneMatch(pair -> pair.getValue().equals(tag.label())))
        .collect(Collectors.toSet());

    Hash hash = new Hash(0);
    if (this.insert)
      hash = this.currentPicture.hash();
    else if (recomputeHash)
      try {
        hash = Hash.computeForFile(this.currentPicture.path());
      } catch (final Exception e) {
        App.logger().error("Error computing hash for file {}", this.currentPicture.path(), e);
      }

    return Optional.of(new PictureUpdate(
        this.insert ? 0 : this.currentPicture.id(),
        this.currentPicture.path(),
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

  private void clearTargetPath() {
    this.pathBox.setVisible(false);
    this.targetPath = null;
    this.targetPathLabel.setText(null);
    this.clearPathButton.setDisable(true);
    this.overwriteTargetCheckBox.setDisable(true);
  }

  /**
   * Parse the tags from the tags field’s text.
   *
   * @return The set of parsed tags with their optional type.
   * @throws TagParseException If any invalid tag or tag type is encountered.
   */
  private Set<Pair<Optional<TagType>, String>> parseTags() throws TagParseException {
    final var text = StringUtils.stripNullable(this.tagsField.getText());
    if (text.isEmpty())
      return Set.of();

    final Set<Pair<Optional<TagType>, String>> tags = new HashSet<>();
    for (final String tag : text.get().split("\\s+")) {
      final Pair<Optional<Character>, String> splitTag;
      try {
        splitTag = TagLike.splitLabel(tag);
      } catch (final TagParseException e) {
        throw new TagParseException(
            e,
            "dialog.edit_images." + e.translationKey(),
            e.formatArgs()
        );
      }
      final var tagTypeSymbol = splitTag.getKey();
      final String tagLabel = splitTag.getValue();
      final var tagOpt = this.allTags.stream()
          .filter(t -> t.label().equals(tagLabel))
          .findFirst();
      if (tagOpt.isPresent() && tagOpt.get().definition().isPresent())
        throw new TagParseException(
            "dialog.edit_images.compound_tag_error",
            new FormatArg("label", tagOpt.get().label())
        );
      if (tagTypeSymbol.isPresent()) {
        final char symbol = tagTypeSymbol.get();
        final var tagType = this.tagTypes.stream().filter(type -> type.symbol() == symbol).findAny();
        if (tagType.isEmpty())
          throw new TagParseException(
              "dialog.edit_images.undefined_tag_type_symbol",
              new FormatArg("symbol", symbol)
          );
        else if (tagOpt.isPresent()) {
          final var expectedType = tagOpt.get().type();
          final TagType actualType = tagType.get();
          if (expectedType.isEmpty())
            throw new TagParseException(
                "dialog.edit_images.mismatch_tag_types",
                new FormatArg("label", tagOpt.get().label()),
                new FormatArg("actual_symbol", actualType.symbol())
            );
          else if (expectedType.get() != actualType)
            throw new TagParseException(
                "dialog.edit_images.mismatch_tag_types_2",
                new FormatArg("label", tagOpt.get().label()),
                new FormatArg("expected_symbol", expectedType.get().symbol()),
                new FormatArg("actual_symbol", actualType.symbol())
            );
        }
        tags.add(new Pair<>(tagType, tagLabel));
      } else
        tags.add(new Pair<>(Optional.empty(), tagLabel));
    }

    return tags;
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  private void updateState() {
    final boolean noneRemaining = this.pictures.isEmpty();
    final boolean invalid = this.getPictureUpdate(false).isEmpty();
    this.nextButton.setDisable(noneRemaining || invalid);
    this.skipButton.setDisable(noneRemaining);
    this.finishButton.setDisable(!noneRemaining || invalid);
  }
}
