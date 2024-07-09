package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.paint.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Dialog to create a new tag type.
 */
public class CreateTagTypeDialog extends EditTagTypeDialogBase {
  /**
   * Create a tag type creation dialog.
   *
   * @param config The app’s configuration.
   * @param db     The database to insert new tag types into.
   */
  public CreateTagTypeDialog(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(config, "create_tag_type", db);
    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        try {
          //noinspection OptionalGetWithoutIsPresent
          final var update = this.getTagTypeUpdate().get();
          db.insertTagTypes(Set.of(update));
          return db.getAllTagTypes().stream()
              .filter(tagType -> tagType.label().equals(update.label()))
              .findFirst()
              .orElseThrow(); // Should never happen
        } catch (DatabaseOperationException e) {
          App.logger().error("Exception caught while inserting new tag type", e);
          Alerts.databaseError(config, e.errorCode());
        }
      }
      return null;
    });
  }

  public void reset() {
    this.labelField.setText("");
    this.symbolField.setText("");
    this.colorField.setValue(Color.BLACK); // TODO random color
  }
}
