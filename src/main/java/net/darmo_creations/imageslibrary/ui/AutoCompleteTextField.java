package net.darmo_creations.imageslibrary.ui;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.fxmisc.richtext.*;
import org.reactfx.*;

import java.util.*;
import java.util.function.*;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * <p>
 * Original caret-following popup code from:
 * https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/PopupDemo.java
 */
public class AutoCompleteTextField<T> extends InlineCssTextField {
  private static final int CARET_X_OFFSET = -20;
  private static final int CARET_Y_OFFSET = 0;
  private static final int MAX_SHOWN_SUGGESTIONS = 10;

  private final ContextMenu entriesPopup = new ContextMenu();
  private boolean hidePopupTemporarily = false;
  private boolean canShowSuggestions = false;

  /**
   * Construct a new AutoCompleteTextField.
   *
   * @param entries         The set of entries.
   * @param stringConverter A function to convert a suggestion into a string.
   */
  public AutoCompleteTextField(final Set<T> entries, Function<T, String> stringConverter) {
    EventStreams.nonNullValuesOf(this.caretBoundsProperty()).subscribe(opt -> {
      if (opt.isPresent()) {
        final Bounds bounds = opt.get();
        this.entriesPopup.setX(bounds.getMaxX() + CARET_X_OFFSET);
        this.entriesPopup.setY(bounds.getMaxY() + CARET_Y_OFFSET);
        if (this.hidePopupTemporarily) {
          this.entriesPopup.show(this.getScene().getWindow());
          this.hidePopupTemporarily = false;
        }
      } else {
        this.entriesPopup.hide();
        this.hidePopupTemporarily = true;
      }
    });
    this.selectedTextProperty().addListener((observable, oldValue, newValue) -> this.hideSuggestions());
    this.textProperty().addListener((observableValue, oldValue, newValue) -> this.canShowSuggestions = true);
    this.focusedProperty().addListener((observableValue, oldValue, newValue) -> this.hideSuggestions());
    this.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (!this.canShowSuggestions) {
        this.hideSuggestions();
        return;
      }
      final String text = this.getText();
      if (text == null || text.isEmpty()) {
        this.hideSuggestions();
        return;
      }
      final int caretPos = newValue;
      this.showSuggestions(entries, stringConverter, caretPos);
      this.canShowSuggestions = false;
    });
    this.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.SPACE && event.isControlDown()
          && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown())
        this.showSuggestions(entries, stringConverter, this.getCaretPosition());
    });
  }

  private void hideSuggestions() {
    this.entriesPopup.hide();
  }

  private void showSuggestions(final Set<T> entries, Function<T, String> stringConverter, int caretIndex) {
    final String text = this.getText();
    final String beforeCaret = text.substring(0, caretIndex);
    final String wordBegining = beforeCaret.substring(beforeCaret.lastIndexOf(' ') + 1);
    final var suggestions = entries.stream()
        .map(stringConverter)
        .filter(s -> s.startsWith(wordBegining))
        .sorted()
        .toList();
    if (!suggestions.isEmpty()) {
      this.populatePopup(suggestions);
      if (!this.entriesPopup.isShowing())
        this.entriesPopup.show(this.getScene().getWindow());
    } else {
      this.hideSuggestions();
    }
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
      final int caretPosition = this.getCaretPosition();
      final String beforeCaret = text.substring(0, caretPosition);
      final int length = beforeCaret.substring(beforeCaret.lastIndexOf(' ') + 1).length();
      this.insertText(caretPosition, suggestion.substring(length));
      this.hideSuggestions();
    });
    return item;
  }
}