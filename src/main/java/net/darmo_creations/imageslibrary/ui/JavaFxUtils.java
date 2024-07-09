package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import org.jetbrains.annotations.*;

public final class JavaFxUtils {
  /**
   * Build a new {@link BorderPane} for the given title and content.
   *
   * @param config The current config.
   * @param title  The pane’s title. May be null.
   * @param rows   The pane’s rows: label and associated control node.
   * @return A new {@link BorderPane} object.
   */
  @SuppressWarnings("unchecked")
  @Contract(value = "_, _, _ -> new")
  public static BorderPane newBorderPane(
      @NotNull Config config,
      String title,
      final @NotNull Pair<String, ? extends Node>... rows
  ) {
    final GridPane gridPane = new GridPane();
    gridPane.setHgap(10);
    gridPane.setVgap(5);
    gridPane.setPadding(new Insets(10, 0, 10, 0));
    BorderPane.setAlignment(gridPane, Pos.CENTER);

    for (int i = 0; i < rows.length; i++) {
      final Label nodeLabel = new Label(config.language().translate(rows[i].getKey()));
      nodeLabel.setWrapText(true);
      GridPane.setHalignment(nodeLabel, HPos.RIGHT);
      final Node node = rows[i].getValue();
      GridPane.setHalignment(node, HPos.LEFT);
      gridPane.addRow(i, nodeLabel, node);
      final RowConstraints rc = new RowConstraints();
      rc.setVgrow(Priority.SOMETIMES);
      gridPane.getRowConstraints().add(rc);
    }

    final var cc1 = new ColumnConstraints();
    cc1.setMaxWidth(400);
    cc1.setHgrow(Priority.SOMETIMES);
    final var cc2 = new ColumnConstraints();
    cc2.setHgrow(Priority.SOMETIMES);
    gridPane.getColumnConstraints().addAll(cc1, cc2);

    final BorderPane borderPane = new BorderPane(gridPane, null, null, null, null);

    if (title != null) {
      final Label titleLabel = new Label(config.language().translate(title));
      BorderPane.setAlignment(titleLabel, Pos.CENTER);
      borderPane.setTop(titleLabel);
    }

    return borderPane;
  }

  private JavaFxUtils() {
  }
}
