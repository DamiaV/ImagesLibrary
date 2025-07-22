package net.darmo_creations.bildumilo.ui;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.util.*;
import net.darmo_creations.bildumilo.config.*;
import org.controlsfx.control.*;
import org.jetbrains.annotations.*;

/**
 * {@link PopOver} subclass that contains text.
 */
public class TextPopOver extends PopOver {
  private final Label label;
  private boolean initialized = false;

  /**
   * Create a new text popover.
   *
   * @param arrowLocation The position of the arrow.
   * @param config        The current config.
   */
  public TextPopOver(@NotNull PopOver.ArrowLocation arrowLocation, final @NotNull Config config) {
    this.label = new Label();
    this.label.setPadding(new Insets(5));
    this.setContentNode(this.label);
    this.setArrowLocation(arrowLocation);
    this.setFadeInDuration(new Duration(100));
    this.setFadeOutDuration(new Duration(100));
    this.showingProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue && !this.initialized) {
        // From https://stackoverflow.com/a/36404968/3779986
        config.theme().applyTo((Parent) this.getSkin().getNode());
        this.initialized = true;
      }
    });
  }

  /**
   * Set this popup’s text.
   *
   * @param text The new text.
   */
  public void setText(String text) {
    this.label.setText(text);
  }
}
