package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;

import java.util.*;
import java.util.function.*;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * <p>
 * Original code from Caleb Brinkman on GitHub at https://gist.github.com/floralvikings/10290131
 */
public class AutoCompleteTextField<T> extends TextField {
  private static final int MAX_SHOWN_SUGGESTIONS = 10;

  private final ContextMenu entriesPopup = new ContextMenu();

  /**
   * Construct a new AutoCompleteTextField.
   *
   * @param entries         The set of entries.
   * @param stringConverter A function to convert a suggestion into a string.
   */
  public AutoCompleteTextField(final Set<T> entries, Function<T, String> stringConverter) {
    this.selectedTextProperty().addListener((observable, oldValue, newValue) -> this.entriesPopup.hide());
    this.textProperty().addListener((observableValue, oldValue, newValue) -> {
      if (newValue.isEmpty()) {
        this.entriesPopup.hide();
      } else {
        int caretPosition = this.getCaretPosition();
        // Caret position is not updated yet at this stage, manually adjust it
        if (oldValue.length() < newValue.length())
          caretPosition++; // A character was added
        else
          caretPosition--; // A character was removed
        String beforeCaret = newValue.substring(0, Math.min(caretPosition, newValue.length()));
        String wordBegining = beforeCaret.substring(beforeCaret.lastIndexOf(' ') + 1);
        final var suggestions = entries.stream()
            .map(stringConverter)
            .filter(s -> s.startsWith(wordBegining))
            .sorted()
            .toList();
        if (!suggestions.isEmpty()) {
          this.populatePopup(suggestions);
          if (!this.entriesPopup.isShowing())
            this.entriesPopup.show(this, Side.BOTTOM, 0, 0);
        } else {
          this.entriesPopup.hide();
        }
      }
    });
    this.focusedProperty().addListener((observableValue, oldValue, newValue) -> this.entriesPopup.hide());
  }

  /**
   * Populate the entry set with the given suggestions.
   * Display is limited to {@link #MAX_SHOWN_SUGGESTIONS} entries, for performance.
   *
   * @param suggestions The set of suggestions.
   */
  private void populatePopup(final List<String> suggestions) {
    this.entriesPopup.getItems().clear();
    suggestions.stream()
        .limit(MAX_SHOWN_SUGGESTIONS)
        .map(this::newMenuItem)
        .forEach(this.entriesPopup.getItems()::add);
  }

  private MenuItem newMenuItem(String suggestion) {
    final CustomMenuItem item = new CustomMenuItem(new Label(suggestion), true);
    item.setOnAction(actionEvent -> {
      final String text = this.getText();
      final int caretPosition = Math.min(this.getCaretPosition(), text.length());
      final String beforeCaret = text.substring(0, caretPosition);
      final int length = beforeCaret.substring(beforeCaret.lastIndexOf(' ') + 1).length();
      this.insertText(caretPosition, suggestion.substring(length));
      this.entriesPopup.hide();
    });
    return item;
  }
}