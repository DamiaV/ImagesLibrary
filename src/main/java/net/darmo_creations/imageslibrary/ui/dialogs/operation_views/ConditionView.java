package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.layout.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import net.darmo_creations.imageslibrary.query_parser.*;
import net.darmo_creations.imageslibrary.query_parser.ex.*;
import net.darmo_creations.imageslibrary.ui.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.*;

/**
 * A {@link Pane} that shows a form to edit an {@link Condition}.
 */
public class ConditionView extends VBox {
  private final ComboBox<ConditionType> conditionTypeCombo = new ComboBox<>();
  private final AutoCompleteField<Tag, Collection<String>> tagsField;
  private final HBox fieldBox = new HBox(5);

  private final Config config;
  private final Set<Consumer<ConditionView>> updateListeners = new HashSet<>();

  /**
   * Create a new view.
   *
   * @param config The app’s config.
   * @param db     The database.
   */
  public ConditionView(@NotNull Config config, @NotNull DatabaseConnection db) {
    super(5);
    this.config = config;

    this.tagsField = new AutoCompleteField<>(
        new StyleClassedTextField(),
        db.getAllTags(),
        t -> true,
        Tag::label,
        new TagQuerySyntaxHighlighter(),
        List::of,
        config
    );

    final var converter = new ConditionTypeStringConverter();
    this.conditionTypeCombo.setButtonCell(new ComboBoxListCell<>(converter));
    this.conditionTypeCombo.setCellFactory(param -> new ComboBoxListCell<>(converter));
    this.conditionTypeCombo.getItems().addAll(ConditionType.values());
    this.conditionTypeCombo.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> this.onConditionTypeChange(newValue));
    this.conditionTypeCombo.getSelectionModel().select(0);

    this.tagsField.textProperty().addListener((observable, oldValue, newValue) -> this.notifyListeners());
    HBox.setHgrow(this.tagsField, Priority.ALWAYS);

    final HBox typeBox = new HBox(
        5,
        new Label(config.language().translate("condition_view.title")),
        this.conditionTypeCombo
    );
    typeBox.setAlignment(Pos.CENTER_LEFT);
    this.fieldBox.managedProperty().bind(this.fieldBox.visibleProperty());
    this.fieldBox.setAlignment(Pos.CENTER_LEFT);
    this.fieldBox.getChildren().addAll(
        new Label(config.language().translate("condition_view.tags_field.label")),
        this.tagsField
    );
    this.getChildren().addAll(typeBox, this.fieldBox);
  }

  /**
   * Fill the form with the data of the given {@link Condition}.
   *
   * @param condition The condition to pull data from. May be null.
   */
  public void setup(Condition condition) {
    if (condition == null)
      this.conditionTypeCombo.getSelectionModel().select(ConditionType.NONE);
    else if (condition instanceof TagQueryCondition c) {
      this.conditionTypeCombo.getSelectionModel().select(ConditionType.TAG_QUERY);
      this.tagsField.setText(c.tagQuery());
    }
  }

  /**
   * Indicates whether this form contains valid data.
   *
   * @return True if the form’s data is valid, false otherwise.
   */
  public boolean isValid() {
    return switch (this.conditionTypeCombo.getSelectionModel().getSelectedItem()) {
      case TAG_QUERY -> {
        try {
          TagQueryParser.parse(
              this.tagsField.getText(),
              Map.of(),
              DatabaseConnection.PSEUDO_TAGS,
              this.config
          );
          yield true;
        } catch (final InvalidPseudoTagException | TagQuerySyntaxErrorException | TagQueryTooLargeException e) {
          yield false;
        }
      }
      case NONE -> true;
    };
  }

  /**
   * A {@link Condition} object that represents the form’s data.
   */
  @Contract(pure = true, value = "-> new")
  public Optional<Condition> getCondition() {
    return switch (this.conditionTypeCombo.getSelectionModel().getSelectedItem()) {
      case TAG_QUERY -> Optional.of(new TagQueryCondition(this.tagsField.getText()));
      case NONE -> Optional.empty();
    };
  }

  public void addUpdateListener(@NotNull Consumer<ConditionView> listener) {
    this.updateListeners.add(Objects.requireNonNull(listener));
  }

  private void onConditionTypeChange(@NotNull ConditionType type) {
    this.fieldBox.setVisible(type == ConditionType.TAG_QUERY);
    this.notifyListeners();
  }

  private void notifyListeners() {
    this.updateListeners.forEach(l -> l.accept(this));
  }

  private enum ConditionType {
    NONE,
    TAG_QUERY,
  }

  private class ConditionTypeStringConverter extends StringConverter<ConditionType> {
    @Override
    public String toString(ConditionType conditionType) {
      return ConditionView.this.config.language()
          .translate("condition_view.type." + conditionType.name().toLowerCase());
    }

    @Override
    public ConditionType fromString(String string) {
      return null; // Not needed
    }
  }
}
