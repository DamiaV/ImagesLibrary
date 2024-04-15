package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.utils.*;

import java.util.*;

/**
 * Dialog to edit a tag type.
 */
public class EditTagTypeDialog extends DialogBase<ButtonType> {
  private final TextField labelField = new TextField();
  private final TextField symbolField = new TextField();
  private final ColorPicker colorField = new ColorPicker();

  private TagType tagType;

  /**
   * Create a tag type editing dialog.
   *
   * @param config The app’s configuration.
   */
  public EditTagTypeDialog(Config config, DatabaseConnection db) {
    super("edit_tag_type", false, config, ButtonTypes.OK, ButtonTypes.CANCEL);

    this.getDialogPane().setPrefWidth(350);
    this.getDialogPane().setContent(this.createContent());
    config.theme().getAppIcon().ifPresent(this::setIcon);

    this.setResultConverter(buttonType -> {
      if (!buttonType.getButtonData().isCancelButton()) {
        try {
          //noinspection OptionalGetWithoutIsPresent
          db.updateTagTypes(Set.of(this.getTagTypeUpdate().get()));
        } catch (DatabaseOperationException e) {
          App.logger().error("Exception caught while updating tag type", e);
          Alerts.error(
              config,
              "dialog.edit_tag_type.alert.save_error.header",
              "dialog.edit_tag_type.alert.save_error.content",
              null,
              new FormatArg("code", e.errorCode())
          );
        }
      }
      return buttonType;
    });
  }

  public void setTagType(TagType tagType) {
    this.tagType = tagType;
    this.labelField.setText(tagType.label());
    this.symbolField.setText(String.valueOf(tagType.symbol()));
    this.colorField.setValue(colorFromInt(tagType.color()));
    this.refreshTitle();
    this.updateState();
  }

  private Pane createContent() {
    this.labelField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.labelField.getStyleClass();
      if (!TagTypeLike.isLabelValid(newValue)) {
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
      } else
        styleClass.remove("invalid");
      this.updateState();
    });
    HBox.setHgrow(this.labelField, Priority.ALWAYS);

    this.symbolField.textProperty().addListener((observable, oldValue, newValue) -> {
      final var styleClass = this.symbolField.getStyleClass();
      if (newValue.length() != 1 || !TagTypeLike.isSymbolValid(newValue.charAt(0))) {
        if (!styleClass.contains("invalid"))
          styleClass.add("invalid");
      } else
        styleClass.remove("invalid");
      this.updateState();
    });
    HBox.setHgrow(this.symbolField, Priority.ALWAYS);

    this.colorField.valueProperty().addListener((observable, oldValue, newValue) -> this.updateState());

    //noinspection unchecked
    return JavaFxUtils.newBorderPane(
        this.config,
        null,
        new Pair<>("dialog.edit_tag_type.label.label", this.labelField),
        new Pair<>("dialog.edit_tag_type.symbol.label", this.symbolField),
        new Pair<>("dialog.edit_tag_type.color.label", this.colorField)
    );
  }

  private Optional<TagTypeUpdate> getTagTypeUpdate() {
    if (!TagTypeLike.isLabelValid(this.labelField.getText())
        || this.symbolField.getText().length() != 1
        || !TagTypeLike.isSymbolValid(this.symbolField.getText().charAt(0)))
      return Optional.empty();
    return Optional.of(new TagTypeUpdate(
        this.tagType.id(),
        this.labelField.getText().strip(),
        this.symbolField.getText().charAt(0),
        colorToInt(this.colorField.getValue())
    ));
  }

  private static Color colorFromInt(int color) {
    return Color.rgb(color >> 24 & 0xff, color >> 16 & 0xff, color >> 8 & 0xff);
  }

  private static int colorToInt(Color color) {
    final int r = (int) Math.round(color.getRed() * 255);
    final int g = (int) Math.round(color.getGreen() * 255);
    final int b = (int) Math.round(color.getBlue() * 255);
    return r << 24 | g << 16 | b << 8;
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  private void updateState() {
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
