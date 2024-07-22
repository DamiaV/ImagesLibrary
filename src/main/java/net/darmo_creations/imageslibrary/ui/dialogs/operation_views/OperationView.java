package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import net.darmo_creations.imageslibrary.themes.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * A {@link Pane} that shows a form to edit an {@link Operation}.
 *
 * @param <T> The type of {@link Operation} this panel represents.
 */
public abstract class OperationView<T extends Operation> extends TitledPane {
  private final Button moveUpButton = new Button();
  private final Button moveDownButton = new Button();
  private final ConditionView conditionView;

  protected final DatabaseConnection db;
  private final Set<OperationUpdateListener> operationUpdateListeners = new HashSet<>();

  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public OperationView(T operation, boolean expanded, @NotNull DatabaseConnection db, @NotNull Config config) {
    this.db = db;
    final Language language = config.language();
    final Theme theme = config.theme();

    this.setText(language.translate("operation_view." + this.getTitleKey()));
    this.setAnimated(false);
    this.setExpanded(expanded);

    final Button deleteButton = new Button();
    deleteButton.setTooltip(new Tooltip(language.translate("operation_view.delete_button.tooltip")));
    deleteButton.setGraphic(theme.getIcon(Icon.DELETE_OPERATION, Icon.Size.SMALL));
    deleteButton.setOnAction(event -> this.operationUpdateListeners.forEach(l -> l.onOperationDelete(this)));

    this.moveUpButton.setTooltip(new Tooltip(language.translate("operation_view.move_up_button.tooltip")));
    this.moveUpButton.setGraphic(theme.getIcon(Icon.MOVE_UP, Icon.Size.SMALL));
    this.moveUpButton.setOnAction(event -> this.operationUpdateListeners.forEach(l -> l.onOperationMoveUp(this)));

    this.moveDownButton.setTooltip(new Tooltip(language.translate("operation_view.move_down_button.tooltip")));
    this.moveDownButton.setGraphic(theme.getIcon(Icon.MOVE_DOWN, Icon.Size.SMALL));
    this.moveDownButton.setOnAction(event -> this.operationUpdateListeners.forEach(l -> l.onOperationMoveDown(this)));

    this.conditionView = new ConditionView(config, db);
    this.conditionView.addUpdateListener(v -> this.notifyUpdateListeners());
    if (operation != null && operation.condition().isPresent())
      this.conditionView.setup(operation.condition().get());

    final HBox buttonsBox = new HBox(
        5,
        deleteButton,
        this.moveUpButton,
        this.moveDownButton
    );
    buttonsBox.setAlignment(Pos.CENTER_LEFT);
    this.setGraphic(buttonsBox);

    final HBox contentBox = new HBox(5);
    this.setupContent(config).ifPresent(content -> {
      HBox.setHgrow(content, Priority.ALWAYS);
      contentBox.getChildren().addAll(content, new Separator(Orientation.VERTICAL));
    });
    HBox.setHgrow(this.conditionView, Priority.ALWAYS);
    contentBox.getChildren().add(this.conditionView);
    this.setContent(contentBox);
  }

  /**
   * Set whether the “Move up” button should be enabled.
   */
  public final void setCanMoveUp(boolean b) {
    this.moveUpButton.setDisable(!b);
  }

  /**
   * Set whether the “Move down” button should be enabled.
   */
  public final void setCanMoveDown(boolean b) {
    this.moveDownButton.setDisable(!b);
  }

  /**
   * Indicates whether the form contains valid data.
   *
   * @return True if the form’s data is valid, false otherwise.
   */
  public final boolean isValid() {
    return this.conditionView.isValid() && this.isOperationValid();
  }

  /**
   * Get the {@link Operation} instance for this view.
   *
   * @return A new {@link Operation} object.
   * @throws IllegalStateException If {@link #isValid()} evaluates to false.
   */
  @Contract(pure = true, value = "-> new")
  public abstract T getOperation();

  public void addUpdateListener(@NotNull OperationUpdateListener listener) {
    this.operationUpdateListeners.add(Objects.requireNonNull(listener));
  }

  protected abstract String getTitleKey();

  protected abstract Optional<Node> setupContent(@NotNull Config config);

  protected abstract boolean isOperationValid();

  protected Optional<Condition> getCondition() {
    return this.conditionView.getCondition();
  }

  protected void notifyUpdateListeners() {
    this.operationUpdateListeners.forEach(l -> l.onOperationUpdate(this));
  }

  public interface OperationUpdateListener {
    void onOperationUpdate(@NotNull OperationView<?> source);

    void onOperationDelete(@NotNull OperationView<?> source);

    void onOperationMoveUp(@NotNull OperationView<?> source);

    void onOperationMoveDown(@NotNull OperationView<?> source);
  }
}
