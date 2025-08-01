package net.darmo_creations.bildumilo.ui.dialogs;

import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Base class for this app’s dialogs.
 *
 * @param <T> Type of returned values.
 */
public abstract class DialogBase<T> extends Dialog<T> {
  protected String name;
  protected final Config config;
  private boolean internalTitleUpdate;
  private final EventHandler<KeyEvent> keyEventEventHandler;

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
    this.titleProperty().addListener((observable, oldValue, newValue) -> {
      if (this.internalTitleUpdate) return;
      this.internalTitleUpdate = true;
      this.titleProperty().setValue(newValue + " – " + App.NAME);
      this.internalTitleUpdate = false;
    });
    this.refreshTitle();
    this.getDialogPane().getButtonTypes().addAll(buttonTypes);
    config.theme().getAppIcon().ifPresent(this.stage().getIcons()::add);
    this.keyEventEventHandler = event -> {
      if (event.getCode() == KeyCode.ESCAPE) // Avoid event being consumed by focused widget
        this.hide();
    };
    this.stage().addEventFilter(KeyEvent.KEY_PRESSED, this.keyEventEventHandler);
  }

  protected void removeGlobalEventFilter() {
    this.stage().removeEventFilter(KeyEvent.KEY_PRESSED, this.keyEventEventHandler);
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
   * Disable all interactions with this dialog’s content.
   */
  protected void disableInteractions() {
    this.stage().getScene().getRoot().setDisable(true);
  }

  /**
   * Restore all interactions with this dialog’s content.
   */
  protected void restoreInteractions() {
    this.stage().getScene().getRoot().setDisable(false);
  }

  /**
   * This dialog’s stage.
   */
  protected Stage stage() {
    return (Stage) this.getDialogPane().getScene().getWindow();
  }
}
