package net.darmo_creations.imageslibrary.ui;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.jetbrains.annotations.*;

import java.util.*;

public final class ClickableListCellFactory {
  /**
   * Create a {@link ListCell} that fires an event when it is clicked or double-clicked.
   *
   * @param listener A listener that will listen to these events.
   * @return A new {@link ListCell} subclass.
   */
  @Contract(pure = true, value = "_ -> new")
  public static <T> ListCell<T> forListener(@NotNull ClickListener<T> listener) {
    return new ListCell<>() {
      // Adapted from javafx.scene.control.skin.ListViewSkin.createDefaultCellImpl()
      @Override
      public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
          this.setText(null);
          this.setGraphic(null);
        } else if (item instanceof Node newNode) {
          this.setText(null);
          final Node currentNode = this.getGraphic();
          if (!newNode.equals(currentNode)) {
            this.setGraphic(newNode);
            this.installMouseClickListener(item);
          }
        } else {
          this.setText(Objects.toString(item));
          this.setGraphic(null);
          if (item != null)
            this.installMouseClickListener(item);
        }
      }

      private void installMouseClickListener(@NotNull T item) {
        this.setOnMouseClicked(event -> {
          if (event.getButton().equals(MouseButton.PRIMARY)) {
            if (event.getClickCount() == 1)
              listener.onItemClick(item);
            else if (event.getClickCount() > 1)
              listener.onItemDoubleClick(item);
          }
        });
      }
    };
  }

  private ClickableListCellFactory() {
  }

  public interface ClickListener<T> {
    /**
     * Called when an item is clicked.
     *
     * @param item The clicked item.
     */
    void onItemClick(@NotNull T item);

    /**
     * Called when an item is double-clicked.
     *
     * @param item The double-clicked item.
     */
    void onItemDoubleClick(@NotNull T item);
  }
}
