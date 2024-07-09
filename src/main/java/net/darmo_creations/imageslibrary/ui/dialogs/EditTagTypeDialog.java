package net.darmo_creations.imageslibrary.ui.dialogs;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Dialog to edit a tag type.
 */
public class EditTagTypeDialog extends EditTagTypeDialogBase {
  private TagType tagType;

  /**
   * Create a tag type editing dialog.
   *
   * @param config The app’s configuration.
   * @param db     The database to update tag types from.
   */
  public EditTagTypeDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "edit_tag_type", db);
    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        try {
          //noinspection OptionalGetWithoutIsPresent
          final var update = this.getTagTypeUpdate().get();
          db.updateTagTypes(Set.of(update));
          return db.getAllTagTypes().stream()
              .filter(tagType -> tagType.label().equals(update.label()))
              .findFirst()
              .orElseThrow(); // Should never happen
        } catch (final DatabaseOperationException e) {
          App.logger().error("Exception caught while updating tag type", e);
          Alerts.databaseError(config, e.errorCode());
        }
      }
      return null;
    });
  }

  public void setTagType(@NotNull TagType tagType) {
    this.tagType = tagType;
    this.labelField.setText(tagType.label());
    this.symbolField.setText(String.valueOf(tagType.symbol()));
    this.colorField.setValue(colorFromInt(tagType.color()));
    this.refreshTitle();
    this.updateState();
  }

  @Override
  protected boolean isLabelAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTagTypes().stream().anyMatch(tt -> tt.id() != this.tagType.id() && tt.label().equals(newValue));
  }

  @Override
  protected boolean isSymbolAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTagTypes().stream().anyMatch(tt -> tt.id() != this.tagType.id() && tt.symbol() == newValue.charAt(0));
  }

  @Override
  protected Optional<TagTypeUpdate> getTagTypeUpdate() {
    if (!this.isLabelValid || !this.isSymbolValid)
      return Optional.empty();
    return Optional.of(new TagTypeUpdate(
        this.tagType.id(),
        this.labelField.getText().strip(),
        this.symbolField.getText().charAt(0),
        colorToInt(this.colorField.getValue())
    ));
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  @Override
  protected void updateState() {
    final var update = this.getTagTypeUpdate();
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(
        update.isEmpty()
        || update.get().label().equals(this.tagType.label())
           && update.get().symbol() == this.tagType.symbol()
           && update.get().color() == this.tagType.color()
    );
  }

  @Override
  protected List<FormatArg> getTitleFormatArgs() {
    return this.tagType != null ? List.of(new FormatArg("label", this.tagType.label())) : List.of();
  }
}
