package net.darmo_creations.imageslibrary.ui;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.jetbrains.annotations.*;

public final class DoubleClickableListCellFactory {
  /**
   * Create a {@link ListCell} that fires an event when it is double-clicked.
   *
   * @param listener A listener that will listen to these events.
   * @return A new {@link ListCell} subclass.
   */
  @Contract(pure = true, value = "_ -> new")
  public static <T> ListCell<T> forListener(DoubleClickListener<T> listener) {
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
          Node currentNode = this.getGraphic();
          if (currentNode == null || !currentNode.equals(newNode))
            this.setGraphic(newNode);
          this.installMouseClickListener(item);
        } else {
          this.setText(item == null ? "null" : item.toString());
          this.setGraphic(null);
          this.installMouseClickListener(item);
        }
      }

      private void installMouseClickListener(T item) {
        this.setOnMouseClicked(event -> {
          if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() > 1)
            listener.onItemDoubleClick(item);
        });
      }
    };
  }

  private DoubleClickableListCellFactory() {
  }

  @FunctionalInterface
  public interface DoubleClickListener<T> {
    /**
     * Called when an item is double-clicked.
     *
     * @param item The double-clicked item.
     */
    void onItemDoubleClick(T item);
  }
}
