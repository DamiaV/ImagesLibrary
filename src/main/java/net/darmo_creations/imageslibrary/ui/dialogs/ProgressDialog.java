package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.ui.*;
import org.jetbrains.annotations.*;

/**
 * A dialog that shows the progress of a process.
 * <p>
 * Clicking the “Cancel” button or closing this dialog will interrupt the process it is attached to.
 */
public class ProgressDialog extends DialogBase<Void> implements ProgressManager {
  private final Label messageLabel = new Label();
  private final Label progressLabel = new Label();
  private final ProgressBar progressBar = new ProgressBar();

  private boolean cancelled = false;

  public ProgressDialog(@NotNull Config config, @NotNull String name) {
    super(config, name, false, ButtonTypes.CANCEL);

    this.progressBar.setMaxWidth(Double.MAX_VALUE);
    final VBox content = new VBox(5, this.messageLabel, this.progressLabel, this.progressBar);
    content.setAlignment(Pos.TOP_CENTER);
    this.getDialogPane().setContent(content);
    this.getDialogPane().setPrefWidth(500);

    this.setOnShown(event -> {
      this.messageLabel.setText(null);
      this.progressLabel.setText(null);
      this.progressBar.setProgress(0);
      this.cancelled = false;
    });

    this.setResultConverter(buttonType -> {
      if (buttonType.getButtonData().isCancelButton())
        this.cancelled = true;
      return null;
    });
  }

  @Override
  public void notifyProgress(@NotNull String messageKey, int total, int progress) {
    final Language language = this.config.language();
    this.messageLabel.setText(language.translate(messageKey));
    this.progressLabel.setText("%s/%s".formatted(language.formatNumber(progress), language.formatNumber(total)));
    this.progressBar.setProgress(progress / (double) total);
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }
}
