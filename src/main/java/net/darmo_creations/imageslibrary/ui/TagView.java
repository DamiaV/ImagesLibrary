package net.darmo_creations.imageslibrary.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This panel displays a tag.
 */
public class TagView extends HBox {
  private final Tag tag;

  /**
   * Create a new panel for the given tag.
   *
   * @param tag The tag to display.
   */
  public TagView(final @NotNull Tag tag) {
    this.tag = Objects.requireNonNull(tag);
    final Label label = new Label(tag.label());
    tag.type().ifPresent(tagType -> {
      label.setText(tagType.symbol() + label.getText());
      label.setStyle("-fx-text-fill: %s;".formatted(StringUtils.colorToCss(tagType.color())));
    });
    this.getChildren().add(label);
  }

  /**
   * The tag held by this panel.
   */
  public Tag tag() {
    return this.tag;
  }
}
