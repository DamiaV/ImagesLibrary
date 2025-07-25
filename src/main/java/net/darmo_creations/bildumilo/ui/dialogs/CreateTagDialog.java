package net.darmo_creations.bildumilo.ui.dialogs;

import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Dialog to create a new tag.
 */
public class CreateTagDialog extends EditTagDialogBase {
  /**
   * Create a tag creation dialog.
   *
   * @param config The app’s configuration.
   * @param db     The database to insert new tags into.
   */
  public CreateTagDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "create_tag", db);
    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        try {
          //noinspection OptionalGetWithoutIsPresent
          final var update = this.getTagUpdate().get();
          db.insertTags(Set.of(update));
          return db.getAllTags().stream()
              .filter(tag -> tag.label().equals(update.label()))
              .findFirst()
              .orElseThrow(); // Should never happen
        } catch (final DatabaseOperationException e) {
          App.logger().error("Exception caught while inserting new tag", e);
          Alerts.databaseError(config, e.errorCode());
        }
      }
      return null;
    });
  }

  public void reset(TagType tagType) {
    this.labelField.setText("");
    this.refreshTypesCombo();
    this.tagTypeComboBox.getSelectionModel().select(new TagTypeEntry(tagType));
    this.definitionField.setText("");
  }
}
