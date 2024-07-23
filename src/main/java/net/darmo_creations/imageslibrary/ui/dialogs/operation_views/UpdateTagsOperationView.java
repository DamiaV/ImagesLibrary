package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * This view provides a form to edit an {@link UpdateTagsOperation}.
 */
public class UpdateTagsOperationView extends OperationView<UpdateTagsOperation> {
  private AutoCompleteField<Tag, String> tagsToAddField;
  private AutoCompleteField<Tag, String> tagsToRemoveField;

  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public UpdateTagsOperationView(
      UpdateTagsOperation operation,
      boolean expanded,
      @NotNull DatabaseConnection db,
      @NotNull Config config
  ) {
    super(operation, expanded, db, config);
    if (operation != null) {
      this.tagsToAddField.setText(
          operation.tagsToAdd().stream()
              .sorted()
              .map(parsedTag -> {
                if (parsedTag.tagType().isPresent() && this.getTag(parsedTag.label()).isEmpty())
                  // Show type symbol if tag does not exist
                  return parsedTag.tagType().get().symbol() + parsedTag.label();
                return parsedTag.label();
              })
              .collect(Collectors.joining(" ")));
      this.tagsToRemoveField.setText(
          operation.tagsToRemove().stream()
              .map(Tag::label)
              .sorted()
              .collect(Collectors.joining(" ")));
    }
  }

  @Override
  protected String getTitleKey() {
    return UpdateTagsOperation.KEY;
  }

  @Override
  protected Optional<Node> setupContent(Config config) {
    final Language language = config.language();

    this.tagsToAddField = new AutoCompleteField<>(
        new InlineCssTextArea(),
        this.db.getAllTags(),
        Tag::label,
        new TagListSyntaxHighlighter(this.db.getAllTags(), this.db.getAllTagTypes()),
        Function.identity(),
        config);
    this.tagsToAddField.textProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());
    this.tagsToAddField.setPrefHeight(100);
    this.tagsToAddField.setPrefWidth(200);
    VBox.setVgrow(this.tagsToAddField, Priority.ALWAYS);

    this.tagsToRemoveField = new AutoCompleteField<>(
        new InlineCssTextArea(),
        this.db.getAllTags(),
        Tag::label,
        new TagListSyntaxHighlighter(this.db.getAllTags(), this.db.getAllTagTypes()),
        Function.identity(),
        config);
    this.tagsToRemoveField.textProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());
    this.tagsToRemoveField.setPrefHeight(100);
    this.tagsToRemoveField.setPrefWidth(200);
    VBox.setVgrow(this.tagsToRemoveField, Priority.ALWAYS);

    final VBox addBox = new VBox(5, new Label(language.translate("operation_view.update_tags.tags_to_add")), this.tagsToAddField);
    HBox.setHgrow(addBox, Priority.ALWAYS);
    final VBox removeBox = new VBox(5, new Label(language.translate("operation_view.update_tags.tags_to_remove")), this.tagsToRemoveField);
    HBox.setHgrow(removeBox, Priority.ALWAYS);
    return Optional.of(new HBox(5, addBox, removeBox));
  }

  @Override
  protected boolean isOperationValid() {
    try {
      final Set<ParsedTag> toAdd = this.getTags(this.tagsToAddField, Function.identity());
      final Set<Tag> toRemove = this.getTags(this.tagsToRemoveField, pt -> this.getTag(pt.label()).orElseThrow());
      return !toAdd.isEmpty() || !toRemove.isEmpty();
    } catch (final TagParseException | NoSuchElementException e) {
      return false;
    }
  }

  @Override
  public UpdateTagsOperation getOperation() {
    try {
      return new UpdateTagsOperation(
          this.getTags(this.tagsToAddField, Function.identity()),
          this.getTags(this.tagsToRemoveField, pt -> this.getTag(pt.label()).orElseThrow()),
          this.getCondition().orElse(null)
      );
    } catch (final TagParseException | NoSuchElementException e) {
      throw new IllegalStateException(e);
    }
  }

  private Optional<Tag> getTag(@NotNull String label) {
    return this.db.getAllTags().stream()
        .filter(tag -> tag.label().equals(label))
        .findFirst();
  }

  private <T> Set<T> getTags(@NotNull AutoCompleteField<Tag, String> tagsField, @NotNull Function<ParsedTag, T> mapper)
      throws TagParseException {
    return JavaFxUtils.parseTags(tagsField, this.db).stream().map(mapper).collect(Collectors.toSet());
  }
}
