package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;

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

  /**
   * Parse the tags from the given text field.
   *
   * @return The set of parsed tags with their optional type.
   * @throws TagParseException If any invalid tag or tag type is encountered.
   */
  public static Set<ParsedTag> parseTags(
      @NotNull AutoCompleteField<Tag, String> tagsField,
      @NotNull DatabaseConnection db
  ) throws TagParseException {
    final var text = StringUtils.stripNullable(tagsField.getText());
    if (text.isEmpty())
      return Set.of();

    final Set<ParsedTag> tags = new HashSet<>();
    for (final String tag : text.get().split("\\s+")) {
      final Pair<Optional<Character>, String> splitTag;
      try {
        splitTag = TagLike.splitLabel(tag);
      } catch (final TagParseException e) {
        throw new TagParseException(
            e,
            "dialog.edit_images." + e.translationKey(),
            e.formatArgs()
        );
      }
      final var tagTypeSymbol = splitTag.getKey();
      final String tagLabel = splitTag.getValue();
      final var tagOpt = db.getAllTags().stream()
          .filter(t -> t.label().equals(tagLabel))
          .findFirst();
      if (tagOpt.isPresent() && tagOpt.get().definition().isPresent())
        throw new TagParseException(
            "dialog.edit_images.compound_tag_error",
            new FormatArg("label", tagOpt.get().label())
        );
      if (tagTypeSymbol.isPresent()) {
        final char symbol = tagTypeSymbol.get();
        final var tagType = db.getAllTagTypes().stream().filter(type -> type.symbol() == symbol).findAny();
        if (tagType.isEmpty())
          throw new TagParseException(
              "dialog.edit_images.undefined_tag_type_symbol",
              new FormatArg("symbol", symbol)
          );
        else if (tagOpt.isPresent()) {
          final var expectedType = tagOpt.get().type();
          final TagType actualType = tagType.get();
          if (expectedType.isEmpty())
            throw new TagParseException(
                "dialog.edit_images.mismatch_tag_types",
                new FormatArg("label", tagOpt.get().label()),
                new FormatArg("actual_symbol", actualType.symbol())
            );
          else if (expectedType.get() != actualType)
            throw new TagParseException(
                "dialog.edit_images.mismatch_tag_types_2",
                new FormatArg("label", tagOpt.get().label()),
                new FormatArg("expected_symbol", expectedType.get().symbol()),
                new FormatArg("actual_symbol", actualType.symbol())
            );
        }
        tags.add(new ParsedTag(tagType, tagLabel));
      } else
        tags.add(new ParsedTag(Optional.empty(), tagLabel));
    }

    return tags;
  }
}
