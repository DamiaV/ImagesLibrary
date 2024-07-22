package net.darmo_creations.imageslibrary.ui.dialogs.operation_views;

import javafx.scene.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.data.batch_operations.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This view provides a form to edit a {@link RecomputeHashOperation}.
 */
public class RecomputeHashOperationView extends OperationView<RecomputeHashOperation> {
  /**
   * Create a new view.
   *
   * @param operation An optional operation to initialize the form with.
   * @param expanded  Whether to expand the panel by default.
   * @param db        The database.
   * @param config    The app’s config.
   */
  public RecomputeHashOperationView(
      RecomputeHashOperation operation,
      boolean expanded,
      @NotNull DatabaseConnection db,
      @NotNull Config config
  ) {
    super(operation, expanded, db, config);
  }

  @Override
  protected String getTitleKey() {
    return RecomputeHashOperation.KEY;
  }

  @Override
  protected Optional<Node> setupContent(@NotNull Config config) {
    return Optional.empty();
  }

  @Override
  protected boolean isOperationValid() {
    return true;
  }

  @Override
  public RecomputeHashOperation getOperation() {
    return new RecomputeHashOperation(this.getCondition().orElse(null));
  }
}
