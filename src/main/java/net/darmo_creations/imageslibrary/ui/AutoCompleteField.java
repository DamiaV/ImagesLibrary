package net.darmo_creations.imageslibrary.ui;

import javafx.beans.value.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.syntax_highlighting.*;
import org.fxmisc.richtext.*;
import org.jetbrains.annotations.*;
import org.reactfx.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

/**
 * This class wraps a {@link StyledTextArea} and attaches an "autocomplete" functionality to it,
 * based on a supplied list of entries.
 * <p>
 * It may also perform syntax highlighting if given a {@link SyntaxHighlighter} object.
 * <p>
 * Original caret-following popup code from:
 * https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/PopupDemo.java
 *
 * @param <T> The type of the suggestions.
 * @param <S> The type of the style values accepted by the {@link StyledTextArea#setStyle(int, int, S)} method.
 */
public class AutoCompleteField<T, S> extends AnchorPane {
  private static final Pattern WORD_START_PATTERN =
      Pattern.compile("(?:%s|^)(%s)$".formatted(TagLike.NOT_LABEL_PATTERN, TagLike.LABEL_PATTERN));

  private static final int CARET_X_OFFSET = -20;
  private static final int CARET_Y_OFFSET = 0;
  private static final int MAX_SHOWN_SUGGESTIONS = 10;

  private final ContextMenu entriesPopup = new ContextMenu();
  private final Function<String, S> cssConverter;
  @Nullable
  private SyntaxHighlighter syntaxHighlighter;
  @Nullable
  private String previousHighlightClass;
  private boolean hidePopupTemporarily = false;
  private boolean canShowSuggestions = false;

  private final StyledTextArea<S, S> styledArea;
  private final LinkedList<String> history = new LinkedList<>();
  private int historyIndex;
  private boolean internalUpdate = false;

  /**
   * Wrap a {@link StyledTextArea}.
   *
   * @param styledArea        The text input to wrap.
   * @param entries           The set of entries.
   * @param stringConverter   A function to convert each suggestion into a string.
   * @param syntaxHighlighter An optional syntax highlighter that will color the text.
   * @param cssConverter      If the {@code syntaxHighlighter} argument is specified,
   *                          a function that converts a string returned by {@link Span} objects
   *                          into the appropriate type accepted by the {@code styledArea} object.
   */
  public AutoCompleteField(
      @NotNull StyledTextArea<S, S> styledArea,
      final @NotNull Set<T> entries,
      @NotNull Function<T, String> stringConverter,
      SyntaxHighlighter syntaxHighlighter,
      Function<String, S> cssConverter
  ) {
    this.cssConverter = cssConverter;
    if (!styledArea.getStyleClass().contains("text-input"))
      styledArea.getStyleClass().add("text-input");
    if (!styledArea.getStyleClass().contains("text-area"))
      styledArea.getStyleClass().add("text-area");
    this.styledArea = styledArea;
    this.setSyntaxHighlighter(syntaxHighlighter);
    this.entriesPopup.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      // Prevent space from validating the selected popup entry
      if (event.getCode() == KeyCode.SPACE)
        event.consume();
    });
    this.entriesPopup.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSuggestionsCompletion);
    styledArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSuggestionsCompletion);
    EventStreams.nonNullValuesOf(styledArea.caretBoundsProperty()).subscribe(opt -> {
      if (opt.isPresent()) {
        final Bounds bounds = opt.get();
        this.entriesPopup.setX(bounds.getMaxX() + CARET_X_OFFSET);
        this.entriesPopup.setY(bounds.getMaxY() + CARET_Y_OFFSET);
        if (this.hidePopupTemporarily) {
          this.showSuggestions();
          this.hidePopupTemporarily = false;
        }
      } else {
        this.hideSuggestions();
        this.hidePopupTemporarily = true;
      }
    });
    styledArea.selectedTextProperty().addListener((observable, oldValue, newValue) -> {
      this.hideSuggestions();
      this.refreshHighlighting();
    });
    styledArea.textProperty().addListener((observableValue, oldValue, newValue) -> {
      this.canShowSuggestions = true;
      this.refreshHighlighting();
      if (!this.internalUpdate) {
        if (this.historyIndex >= 0) // Clear history after current index
          this.history.subList(this.historyIndex + 1, this.history.size()).clear();
        this.history.add(newValue); // Add new value
        this.historyIndex++;
        if (this.entriesPopup.isShowing())
          this.focusFirstSuggestion();
      }
    });
    styledArea.focusedProperty().addListener((observableValue, oldValue, newValue) -> this.hideSuggestions());
    styledArea.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (!this.canShowSuggestions) {
        this.hideSuggestions();
        return;
      }
      final String text = this.getText();
      if (text == null || text.isEmpty()) {
        this.hideSuggestions();
        return;
      }
      this.fillAndShowSuggestions(entries, stringConverter, newValue);
      this.canShowSuggestions = false;
    });
    styledArea.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.SPACE && event.isShortcutDown())
        this.fillAndShowSuggestions(entries, stringConverter, styledArea.getCaretPosition());
    });
    styledArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.Z && event.isShortcutDown()) {
        this.undoText();
        event.consume();
      } else if (event.getCode() == KeyCode.Y && event.isShortcutDown()) {
        this.redoText();
        event.consume();
      }
    });
    this.history.add("");
    this.historyIndex = 0;

    AnchorPane.setTopAnchor(styledArea, 0.0);
    AnchorPane.setBottomAnchor(styledArea, 0.0);
    AnchorPane.setLeftAnchor(styledArea, 0.0);
    AnchorPane.setRightAnchor(styledArea, 0.0);
    this.getChildren().add(styledArea);
  }

  private void handleSuggestionsCompletion(@NotNull KeyEvent event) {
    if (this.entriesPopup.isShowing() && (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER)) {
      final List<Node> nodes = this.entriesPopup.getSkin()
          .getNode()
          .lookupAll(".menu-item")
          .stream()
          .sorted(Comparator.comparing(Node::getId))
          .toList();
      for (int i = 0; i < nodes.size(); i++) {
        if (nodes.get(i).isFocused()) {
          event.consume();
          this.entriesPopup.getItems().get(i).fire();
          break;
        }
      }
    }
  }

  private void undoText() {
    if (this.historyIndex > 0) {
      this.internalUpdate = true;
      final int caretPosition = this.styledArea.getCaretPosition(); // Prevent caret from going to the end
      this.styledArea.replaceText(this.history.get(--this.historyIndex));
      this.styledArea.moveTo(Math.min(caretPosition, this.styledArea.getText().length()));
      this.internalUpdate = false;
    }
  }

  private void redoText() {
    if (this.historyIndex < this.history.size() - 1) {
      this.internalUpdate = true;
      final int caretPosition = this.styledArea.getCaretPosition(); // Prevent caret from going to the end
      this.styledArea.replaceText(this.history.get(++this.historyIndex));
      this.styledArea.moveTo(Math.min(caretPosition, this.styledArea.getText().length()));
      this.internalUpdate = false;
    }
  }

  public String getText() {
    return this.styledArea.getText();
  }

  /**
   * Set the text of the wrapped {@link StyledTextArea}.
   * <p>
   * The undo history will be reset.
   * Text highlighting will be updated only if the new text differs from the current one.
   *
   * @param text The new text.
   */
  public void setText(@NotNull String text) {
    this.history.clear();
    final boolean forceRefresh = text.equals(this.styledArea.getText());
    if (forceRefresh) {
      this.history.add(text);
      this.historyIndex = 0;
    } else
      this.historyIndex = -1;
    this.styledArea.replaceText(text);
    if (forceRefresh)
      this.refreshHighlighting();
  }

  public final ObservableValue<String> textProperty() {
    return this.styledArea.textProperty();
  }

  @Override
  public void requestFocus() {
    this.styledArea.requestFocus();
  }

  public void refreshHighlighting() {
    this.styledArea.clearStyle(0);
    if (this.syntaxHighlighter != null)
      this.syntaxHighlighter.highlight(this.getText())
          .forEach(span -> this.styledArea.setStyle(span.start(), span.end() + 1, this.cssConverter.apply(span.css())));
  }

  public void setSyntaxHighlighter(SyntaxHighlighter syntaxHighlighter) {
    this.syntaxHighlighter = syntaxHighlighter;
    if (this.previousHighlightClass != null)
      this.styledArea.getStyleClass().remove(this.previousHighlightClass);
    if (syntaxHighlighter != null) {
      this.previousHighlightClass = "highlight-" + syntaxHighlighter.cssClass();
      this.styledArea.getStyleClass().add(this.previousHighlightClass);
    }
    this.refreshHighlighting();
  }

  private void showSuggestions() {
    if (!this.entriesPopup.getItems().isEmpty() && this.getScene() != null) {
      this.entriesPopup.show(this.getScene().getWindow());
      this.focusFirstSuggestion();
    }
  }

  private void hideSuggestions() {
    this.entriesPopup.hide();
  }

  private void focusFirstSuggestion() {
    this.entriesPopup.getSkin()
        .getNode()
        .lookupAll(".menu-item")
        .stream()
        .min(Comparator.comparing(Node::getId))
        .ifPresent(Node::requestFocus);
  }

  private void fillAndShowSuggestions(
      final @NotNull Set<T> entries,
      @NotNull Function<T, String> stringConverter,
      int caretIndex
  ) {
    final String text = this.getText();
    final String beforeCaret = text.substring(0, caretIndex);
    final Matcher matcher = WORD_START_PATTERN.matcher(beforeCaret);
    final String wordBegining = matcher.find() ? matcher.group(1) : "";
    final List<String> suggestions;
    if (wordBegining.isEmpty())
      suggestions = List.of();
    else
      suggestions = entries.stream()
          .map(stringConverter)
          .filter(s -> s.startsWith(wordBegining) && !s.equals(wordBegining))
          .sorted()
          .toList();
    if (!suggestions.isEmpty()) {
      this.populatePopup(suggestions);
      if (!this.entriesPopup.isShowing())
        this.showSuggestions();
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
  private void populatePopup(final @NotNull List<String> suggestions) {
    this.entriesPopup.getItems().clear();
    suggestions.stream()
        .limit(MAX_SHOWN_SUGGESTIONS)
        .map(this::newMenuItem)
        .forEach(menuItem -> {
          menuItem.setId("suggestion-%02d".formatted(this.entriesPopup.getItems().size()));
          this.entriesPopup.getItems().add(menuItem);
        });
  }

  private MenuItem newMenuItem(@NotNull String suggestion) {
    final CustomMenuItem item = new CustomMenuItem(new Label(suggestion), true);
    item.setOnAction(actionEvent -> {
      final String text = this.getText();
      final int caretPosition = this.styledArea.getCaretPosition();
      final String beforeCaret = text.substring(0, caretPosition);
      final Matcher matcher = WORD_START_PATTERN.matcher(beforeCaret);
      final String wordBegining = matcher.find() ? matcher.group(1) : "";
      final int length = wordBegining.length();
      this.styledArea.insertText(caretPosition, suggestion.substring(length));
      this.hideSuggestions();
    });
    return item;
  }
}
