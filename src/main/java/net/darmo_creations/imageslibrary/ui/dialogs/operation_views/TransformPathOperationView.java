package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

/**
 * This view provides a form to edit a {@link TransformPathOperation}.
 */
public class TransformPathOperationView extends OperationView<TransformPathOperation> {
  private TextField patternField;
  private TextField substituteField;
  private ToggleButton patternAsRegexCheckbox;

  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public TransformPathOperationView(
      TransformPathOperation operation,
      boolean expanded,
      @NotNull DatabaseConnection db,
      @NotNull Config config
  ) {
    super(operation, expanded, db, config);
    this.patternField.setText(operation == null ? "" : operation.pattern());
    this.substituteField.setText(operation == null ? "" : operation.substitute());
    this.patternAsRegexCheckbox.setSelected(operation != null && operation.isPatternRegex());
  }

  @Override
  protected String getTitleKey() {
    return TransformPathOperation.KEY;
  }

  @Override
  protected Optional<Node> setupContent(@NotNull Config config) {
    final Language language = config.language();

    this.patternField = new TextField();
    this.patternField.setPromptText(language.translate("operation_view.transform_path.pattern.prompt"));
    this.patternField.textProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());
    HBox.setHgrow(this.patternField, Priority.ALWAYS);

    this.substituteField = new TextField();
    this.substituteField.setPromptText(language.translate("operation_view.transform_path.substitute.prompt"));
    this.substituteField.textProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());

    this.patternAsRegexCheckbox = new ToggleButton();
    this.patternAsRegexCheckbox.setText(".*");
    this.patternAsRegexCheckbox.setTooltip(new Tooltip(language.translate("operation_view.transform_path.pattern_as_regex")));
    this.patternAsRegexCheckbox.selectedProperty()
        .addListener((observable, oldValue, newValue) -> this.notifyUpdateListeners());

    return Optional.of(new VBox(
        5,
        new HBox(5, this.patternField, this.patternAsRegexCheckbox),
        this.substituteField
    ));
  }

  @Override
  protected boolean isOperationValid() {
    if (this.patternField.getText().isEmpty())
      return false;
    try {
      Pattern.compile(this.patternField.getText());
    } catch (final PatternSyntaxException e) {
      return false;
    }
    return true;
  }

  @Override
  public TransformPathOperation getOperation() {
    return new TransformPathOperation(
        this.patternField.getText(),
        this.substituteField.getText(),
        this.patternAsRegexCheckbox.isSelected(),
        this.getCondition().orElse(null)
    );
  }
}
