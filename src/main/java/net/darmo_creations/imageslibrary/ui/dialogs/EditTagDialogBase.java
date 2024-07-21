package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.controlsfx.control.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base class for dialogs to edit a tag.
 */
public abstract class EditTagDialogBase extends DialogBase<Tag> {
  private final TextPopOver labelErrorPopup;
  protected final TextField labelField = new TextField();
  protected final ComboBox<TagTypeEntry> tagTypeComboBox = new ComboBox<>();
  private final TextPopOver definitionErrorPopup;
  protected final AutoCompleteField<Tag, Collection<String>> definitionField;

  protected boolean isLabelValid = false;
  protected boolean isDefinitionValid = true;

  protected final DatabaseConnection db;

  protected EditTagDialogBase(@NotNull Config config, @NotNull String name, @NotNull DatabaseConnection db) {
    super(config, name, false, ButtonTypes.OK, ButtonTypes.CANCEL);
    this.db = db;

    this.labelErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.definitionErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.definitionField = new AutoCompleteField<>(
        new StyleClassedTextField(),
        this.db.getAllTags(),
        Tag::label,
        new TagQuerySyntaxHighlighter(),
        List::of
    );

    this.getDialogPane().setPrefWidth(400);
    this.getDialogPane().setContent(this.createContent());
  }

  private Pane createContent() {
    final Language language = this.config.language();

    this.labelField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.labelField.getStyleClass();
      if (!TagLike.isLabelValid(newValue)) {
        this.labelErrorPopup.setText(language.translate("dialog.edit_tag.label.invalid"));
        this.showLabelErrorPopup();
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
        this.isLabelValid = false;
      } else if (this.isLabelAlreadyUsed(newValue)) {
        this.labelErrorPopup.setText(language.translate("dialog.edit_tag.label.duplicate"));
        this.showLabelErrorPopup();
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
        this.isLabelValid = false;
      } else {
        styleClass.remove("invalid");
        this.labelErrorPopup.hide();
        this.isLabelValid = true;
      }
      this.updateState();
    });

    this.tagTypeComboBox.setButtonCell(new TagTypeListCell());
    this.tagTypeComboBox.setCellFactory(param -> new TagTypeListCell());
    this.tagTypeComboBox.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue != null)
            this.updateState();
        });
    this.refreshTypesCombo();

    this.definitionField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.tagTypeComboBox.getStyleClass();
      if (!newValue.isBlank()) {
        this.isDefinitionValid = false;
        try {
          TagQueryParser.parse(newValue, this.db.getTagDefinitions(), DatabaseConnection.PSEUDO_TAGS, this.config);
          this.isDefinitionValid = true;
        } catch (final InvalidPseudoTagException e) {
          this.definitionErrorPopup.setText(language.translate("image_search_field.invalid_pseudo_tag", new FormatArg("tag", e.pseudoTag())));
        } catch (final TagQuerySyntaxErrorException e) {
          this.definitionErrorPopup.setText(language.translate("image_search_field.query_syntax_error"));
        } catch (final TagQueryTooLargeException e) {
          this.definitionErrorPopup.setText(language.translate("image_search_field.recursive_loop_error"));
        }
      } else
        this.isDefinitionValid = true;

      if (!this.isDefinitionValid) {
        this.showDefinitionErrorPopup();
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
      } else {
        this.definitionErrorPopup.hide();
        styleClass.remove("invalid");
      }
      this.updateState();
    });

    //noinspection unchecked
    return JavaFxUtils.newBorderPane(
        this.config,
        null,
        new Pair<>("dialog.edit_tag.label.label", this.labelField),
        new Pair<>("dialog.edit_tag.type.label", this.tagTypeComboBox),
        new Pair<>("dialog.edit_tag.definition.label", this.definitionField)
    );
  }

  protected void refreshTypesCombo() {
    this.tagTypeComboBox.getItems().clear();
    this.tagTypeComboBox.getItems().add(new TagTypeEntry(null));
    this.db.getAllTagTypes().stream()
        .sorted(Comparator.comparing(TagType::label))
        .forEach(tagType -> this.tagTypeComboBox.getItems().add(new TagTypeEntry(tagType)));
    this.tagTypeComboBox.getSelectionModel().select(0);
  }

  protected boolean isLabelAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTags().stream().anyMatch(tt -> tt.label().equals(newValue));
  }

  private void showLabelErrorPopup() {
    this.labelErrorPopup.show(this.labelField);
  }

  private void showDefinitionErrorPopup() {
    this.definitionErrorPopup.show(this.definitionField);
  }

  protected Optional<TagUpdate> getTagUpdate() {
    if (!this.isLabelValid || !this.isDefinitionValid)
      return Optional.empty();
    return Optional.of(new TagUpdate(
        0,
        this.labelField.getText().strip(),
        this.tagTypeComboBox.getValue().tagType().orElse(null),
        StringUtils.stripNullable(this.definitionField.getText()).orElse(null)
    ));
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  protected void updateState() {
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(this.getTagUpdate().isEmpty());
  }

  protected static class TagTypeEntry {
    private final TagType tagType;

    protected TagTypeEntry(TagType tagType) {
      this.tagType = tagType;
    }

    public Optional<TagType> tagType() {
      return Optional.ofNullable(this.tagType);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || this.getClass() != o.getClass())
        return false;
      return Objects.equals(this.tagType, ((TagTypeEntry) o).tagType);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.tagType);
    }
  }

  private class TagTypeListCell extends ListCell<TagTypeEntry> {
    @Override
    protected void updateItem(TagTypeEntry item, boolean empty) {
      super.updateItem(item, empty);
      if (empty) {
        this.setText(null);
        this.setStyle(null);
      } else {
        final var opt = item.tagType();
        if (opt.isPresent()) {
          this.setText(opt.get().label());
          this.setStyle("-fx-text-fill: %s;".formatted(StringUtils.colorToCss(opt.get().color())));
        } else {
          this.setText(EditTagDialogBase.this.config.language().translate("dialog.edit_tag.no_type"));
          this.setStyle(null);
        }
      }
    }
  }
}
