package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import org.controlsfx.control.*;

public class TextPopOver extends PopOver {
  private final Label label;
  private boolean initialized = false;

  public TextPopOver(PopOver.ArrowLocation arrowLocation, Config config) {
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

  public void setText(String text) {
    this.label.setText(text);
  }
}
