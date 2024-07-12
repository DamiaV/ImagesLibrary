package net.darmo_creations.imageslibrary.ui.dialogs;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Dialog to edit a tag.
 */
public class EditTagDialog extends EditTagDialogBase {
  private Tag tag;

  /**
   * Create a tag editing dialog.
   *
   * @param config The app’s configuration.
   * @param db     The database to update tags from.
   */
  public EditTagDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "edit_tag", db);
    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        try {
          //noinspection OptionalGetWithoutIsPresent
          final var update = this.getTagUpdate().get();
          db.updateTags(Set.of(update));
          return db.getAllTags().stream()
              .filter(tag -> tag.label().equals(update.label()))
              .findFirst()
              .orElseThrow(); // Should never happen
        } catch (final DatabaseOperationException e) {
          App.logger().error("Exception caught while updating tag", e);
          Alerts.databaseError(config, e.errorCode());
        }
      }
      return null;
    });
  }

  public void setTag(@NotNull Tag tag) {
    this.tag = tag;
    this.labelField.setText(tag.label());
    this.refreshTypesCombo();
    final var type = tag.type();
    if (type.isPresent())
      this.tagTypeComboBox.getSelectionModel().select(new TagTypeEntry(type.get()));
    else
      this.tagTypeComboBox.getSelectionModel().select(0);
    this.definitionField.setText(tag.definition().orElse(""));
    this.definitionField.setDisable(this.db.getAllTagsCounts().get(tag.id()) != 0);
    this.refreshTitle();
    this.updateState();
  }

  @Override
  protected boolean isLabelAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTagTypes().stream().anyMatch(tt -> tt.id() != this.tag.id() && tt.label().equals(newValue));
  }

  @Override
  protected Optional<TagUpdate> getTagUpdate() {
    if (!this.isLabelValid || !this.isDefinitionValid)
      return Optional.empty();
    return Optional.of(new TagUpdate(
        this.tag.id(),
        this.labelField.getText().strip(),
        this.tagTypeComboBox.getValue().tagType().orElse(null),
        StringUtils.stripNullable(this.definitionField.getText()).orElse(null)
    ));
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  @Override
  protected void updateState() {
    final var update = this.getTagUpdate();
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(
        update.isEmpty()
        || update.get().label().equals(this.tag.label())
           && update.get().type().equals(this.tag.type())
           && update.get().definition().equals(this.tag.definition())
    );
  }

  @Override
  protected List<FormatArg> getTitleFormatArgs() {
    return this.tag != null ? List.of(new FormatArg("label", this.tag.label())) : List.of();
  }
}
