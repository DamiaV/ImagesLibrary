package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.scene.control.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base class for this app’s dialogs.
 *
 * @param <T> Type of returned values.
 */
public abstract class DialogBase<T> extends Dialog<T> {
  private final String name;
  protected final Config config;

  /**
   * Create a modal dialog.
   *
   * @param config      The app’s config.
   * @param name        Dialog’s name. Used for the title’s translation key.
   * @param resizable   Whether the dialog should be resizable.
   * @param buttonTypes The dialog’s button types.
   */
  public DialogBase(
      @NotNull Config config,
      @NotNull String name,
      boolean resizable,
      final @NotNull ButtonType... buttonTypes
  ) {
    this(config, name, resizable, true, buttonTypes);
  }

  /**
   * Create a dialog.
   *
   * @param config      The app’s config.
   * @param name        Dialog’s name. Used for the title’s translation key.
   * @param resizable   Whether the dialog should be resizable.
   * @param modal       Whether this dialog should be modal.
   * @param buttonTypes The dialog’s button types.
   */
  public DialogBase(
      @NotNull Config config,
      @NotNull String name,
      boolean resizable,
      boolean modal,
      final @NotNull ButtonType... buttonTypes
  ) {
    this.name = name;
    this.config = config;
    config.theme().applyTo(this.stage().getScene());
    this.initModality(modal ? Modality.APPLICATION_MODAL : Modality.NONE);
    this.setResizable(resizable);
    this.refreshTitle();
    this.getDialogPane().getButtonTypes().addAll(buttonTypes);
    config.theme().getAppIcon().ifPresent(this.stage().getIcons()::add);
  }

  protected void refreshTitle() {
    this.setTitle(this.config.language().translate("dialog.%s.title".formatted(this.name),
        this.getTitleFormatArgs().toArray(FormatArg[]::new)));
  }

  /**
   * Return a list of {@link FormatArg}s to use when formatting this dialog’s title.
   */
  protected List<FormatArg> getTitleFormatArgs() {
    return List.of();
  }

  /**
   * This dialog’s stage.
   */
  protected Stage stage() {
    return (Stage) this.getDialogPane().getScene().getWindow();
  }
}
