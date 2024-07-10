package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Dialog to create/edit images.
 */
public class EditImagesDialog extends DialogBase<Boolean> {
  private final ImageView imageView;
  private final Label fileNameLabel = new Label();
  private final Label fileMetadataLabel = new Label();
  private final TextPopOver tagsErrorPopup;
  private final AutoCompleteField<Tag> tagsField;
  private final Button nextButton;
  private final Button skipButton;

  private boolean areTagsValid = false;

  private final DatabaseConnection db;
  // Local cache to avoid creating unnecessary new set views.
  private final Set<TagType> tagTypes;

  private final Queue<Picture> pictures = new LinkedList<>();
  private final Set<Tag> currentPictureTags = new HashSet<>();
  private Picture currentPicture;
  private boolean insert;
  private boolean anyUpdate;

  public EditImagesDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "edit_images", true, ButtonTypes.FINISH, ButtonTypes.NEXT, ButtonTypes.SKIP, ButtonTypes.CANCEL);
    this.db = db;
    this.tagTypes = db.getAllTagTypes();

    this.imageView = new ImageView();

    this.tagsErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.tagsField = new AutoCompleteField<>(new StyleClassedTextArea(), this.db.getAllTags(), Tag::label, null);

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

    this.getDialogPane().setContent(this.createContent());

    final Stage stage = this.stage();
    stage.setMinWidth(800);
    stage.setMinHeight(600);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton())
        this.applyChanges();
      return this.anyUpdate;
    });
  }

  private Node createContent() {
    final Language language = this.config.language();

    // TODO button to move image, button to list similar images
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

    this.tagsField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.tagsField.getStyleClass();
      if (!newValue.isBlank()) {
        this.areTagsValid = false;
        try {
          this.parseTags();
          this.areTagsValid = true;
        } catch (final IllegalArgumentException e) {
          this.tagsErrorPopup.setText(language.translate("dialog.edit_images.invalid_tags"));
        }
      } else
        this.areTagsValid = true;

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
    VBox.setVgrow(this.tagsField, Priority.ALWAYS);

    final SplitPane splitPane = new SplitPane(
        imageViewBox,
        new VBox(5, fileNameBox, metadataBox, this.tagsField)
    );
    splitPane.setOrientation(Orientation.VERTICAL);
    splitPane.setDividerPositions(0.75);
    return splitPane;
  }

  private void showTagsErrorPopup() {
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
    this.pictures.clear();
    this.pictures.addAll(pictures);
    this.anyUpdate = false;
    this.nextPicture();
  }

  /**
   * Pop the next picture from the queue and reset the edit form with the image’s data.
   */
  private void nextPicture() {
    if (this.pictures.isEmpty())
      return;
    this.currentPicture = this.pictures.poll();
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
      FileUtils.loadImage(path, image -> {
        this.imageView.setImage(image);
        final String text = FileUtils.formatImageMetadata(path, image, this.config);
        this.fileMetadataLabel.setText(text);
        this.fileMetadataLabel.setTooltip(new Tooltip(text));
      }, error -> this.fileMetadataLabel.setText(language.translate("image_preview.missing_file")));
    }
    final var joiner = new StringJoiner(" ");
    this.currentPictureTags.forEach(tag -> {
      String t = tag.label();
      if (tag.type().isPresent())
        t = tag.type().get().symbol() + t;
      joiner.add(t);
    });
    this.tagsField.setText(joiner.toString());
    this.updateState();
  }

  private boolean applyChanges() {
    final Optional<PictureUpdate> update = this.getPictureUpdate();
    if (update.isPresent())
      try {
        this.db.updatePicture(update.get());
        this.anyUpdate = true;
      } catch (final DatabaseOperationException e) {
        Alerts.databaseError(this.config, e.errorCode());
        return false;
      }
    return true;
  }

  private Optional<PictureUpdate> getPictureUpdate() {
    if (!this.areTagsValid)
      return Optional.empty();
    final Set<Pair<Optional<TagType>, String>> parsedTags;
    try {
      parsedTags = this.parseTags();
    } catch (final IllegalArgumentException e) {
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
    return Optional.of(new PictureUpdate(
        this.insert ? 0 : this.currentPicture.id(),
        this.currentPicture.path(),
        this.currentPicture.hash(),
        toAdd,
        toRemove
    ));
  }

  /**
   * Parse the tags from the tags field’s text.
   *
   * @return The set of parsed tags with their optional type.
   * @throws IllegalArgumentException If any invalid tag or tag type is encountered.
   */
  private Set<Pair<Optional<TagType>, String>> parseTags() {
    final var text = StringUtils.stripNullable(this.tagsField.getText());
    if (text.isEmpty())
      return Set.of();

    final Set<Pair<Optional<TagType>, String>> tags = new HashSet<>();
    for (final String tag : text.get().split("\\s+")) {
      final var splitTag = TagLike.splitLabel(tag);
      final var tagTypeSymbol = splitTag.getKey();
      final String tagLabel = splitTag.getValue();
      if (tagTypeSymbol.isPresent()) {
        final char symbol = tagTypeSymbol.get();
        final var any = this.tagTypes.stream().filter(tagType -> tagType.symbol() == symbol).findAny();
        if (any.isEmpty())
          throw new IllegalArgumentException("Invalid tag type symbol: " + symbol);
        tags.add(new Pair<>(any, tagLabel));
      } else
        tags.add(new Pair<>(Optional.empty(), tagLabel));
    }

    return tags;
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  private void updateState() {
    final boolean oneRemaining = this.pictures.size() <= 1;
    final boolean invalid = this.getPictureUpdate().isEmpty();
    this.getDialogPane().lookupButton(ButtonTypes.FINISH).setDisable(!oneRemaining || invalid);
    this.nextButton.setDisable(oneRemaining || invalid);
    this.skipButton.setDisable(oneRemaining);
  }
}
