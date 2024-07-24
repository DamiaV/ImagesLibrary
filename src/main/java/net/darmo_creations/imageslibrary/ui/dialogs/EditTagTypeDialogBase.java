package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.*;
import org.controlsfx.control.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base class for dialogs to edit a tag type.
 */
public abstract class EditTagTypeDialogBase extends DialogBase<TagType> {
  private final TextPopOver labelErrorPopup;
  protected final TextField labelField = new TextField();
  private final TextPopOver symbolErrorPopup;
  protected final TextField symbolField = new TextField();
  protected final ColorPicker colorField = new ColorPicker();

  protected boolean isLabelValid = false;
  protected boolean isSymbolValid = false;

  protected final DatabaseConnection db;

  protected EditTagTypeDialogBase(@NotNull Config config, @NotNull String name, @NotNull DatabaseConnection db) {
    super(config, name, false, ButtonTypes.OK, ButtonTypes.CANCEL);
    this.db = Objects.requireNonNull(db);

    this.labelErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);
    this.symbolErrorPopup = new TextPopOver(PopOver.ArrowLocation.LEFT_CENTER, config);

    this.getDialogPane().setPrefWidth(400);
    this.getDialogPane().setContent(this.createContent());

    this.setOnShowing(event -> this.labelField.requestFocus());
  }

  private Pane createContent() {
    final Language language = this.config.language();

    this.labelField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!TagTypeLike.isLabelValid(newValue)) {
        this.labelErrorPopup.setText(language.translate("dialog.edit_tag_type.label.invalid"));
        this.showLabelErrorPopup();
        this.isLabelValid = false;
      } else if (this.isLabelAlreadyUsed(newValue)) {
        this.labelErrorPopup.setText(language.translate("dialog.edit_tag_type.label.duplicate"));
        this.showLabelErrorPopup();
        this.isLabelValid = false;
      } else {
        this.labelErrorPopup.hide();
        this.isLabelValid = true;
      }
      this.updateState();
    });

    this.symbolField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.length() != 1 || !TagTypeLike.isSymbolValid(newValue.charAt(0))) {
        this.symbolErrorPopup.setText(language.translate("dialog.edit_tag_type.symbol.invalid"));
        this.showSymbolErrorPopup();
        this.isSymbolValid = false;
      } else if (this.isSymbolAlreadyUsed(newValue)) {
        this.symbolErrorPopup.setText(language.translate("dialog.edit_tag_type.symbol.duplicate"));
        this.showSymbolErrorPopup();
        this.isSymbolValid = false;
      } else {
        this.symbolErrorPopup.hide();
        this.isSymbolValid = true;
      }
      this.updateState();
    });

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

  protected boolean isLabelAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTagTypes().stream().anyMatch(tt -> tt.label().equals(newValue));
  }

  protected boolean isSymbolAlreadyUsed(@NotNull String newValue) {
    return this.db.getAllTagTypes().stream().anyMatch(tt -> tt.symbol() == newValue.charAt(0));
  }

  private void showLabelErrorPopup() {
    if (this.getDialogPane().getScene() != null && this.stage().isShowing()) // Fix an NPE that sometimes occur
      this.labelErrorPopup.show(this.labelField);
  }

  private void showSymbolErrorPopup() {
    if (this.getDialogPane().getScene() != null && this.stage().isShowing()) // Fix an NPE that sometimes occur
      this.symbolErrorPopup.show(this.symbolField);
  }

  protected Optional<TagTypeUpdate> getTagTypeUpdate() {
    if (!this.isLabelValid || !this.isSymbolValid)
      return Optional.empty();
    return Optional.of(new TagTypeUpdate(
        0,
        this.labelField.getText().strip(),
        this.symbolField.getText().charAt(0),
        colorToInt(this.colorField.getValue())
    ));
  }

  protected static Color colorFromInt(int color) {
    return Color.rgb(color >> 16 & 0xff, color >> 8 & 0xff, color & 0xff);
  }

  protected static int colorToInt(@NotNull Color color) {
    final int r = (int) Math.round(color.getRed() * 255);
    final int g = (int) Math.round(color.getGreen() * 255);
    final int b = (int) Math.round(color.getBlue() * 255);
    return r << 16 | g << 8 | b;
  }

  /**
   * Update the state of this dialog’s buttons.
   */
  protected void updateState() {
    this.getDialogPane().lookupButton(ButtonTypes.OK).setDisable(this.getTagTypeUpdate().isEmpty());
  }
}
