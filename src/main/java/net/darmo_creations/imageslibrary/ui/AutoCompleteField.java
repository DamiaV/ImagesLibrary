package net.darmo_creations.imageslibrary.ui;

import javafx.beans.value.*;
import javafx.geometry.*;
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
    styledArea.selectedTextProperty().addListener((observable, oldValue, newValue) -> this.hideSuggestions());
    styledArea.textProperty().addListener((observableValue, oldValue, newValue) -> {
      this.canShowSuggestions = true;
      this.highlight();
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
      if (event.getCode() == KeyCode.SPACE && event.isControlDown()
          && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown())
        this.fillAndShowSuggestions(entries, stringConverter, styledArea.getCaretPosition());
    });

    AnchorPane.setTopAnchor(styledArea, 0.0);
    AnchorPane.setBottomAnchor(styledArea, 0.0);
    AnchorPane.setLeftAnchor(styledArea, 0.0);
    AnchorPane.setRightAnchor(styledArea, 0.0);
    this.getChildren().add(styledArea);
  }

  public String getText() {
    return this.styledArea.getText();
  }

  public void setText(@NotNull String text) {
    this.styledArea.replaceText(text);
  }

  public final ObservableValue<String> textProperty() {
    return this.styledArea.textProperty();
  }

  @Override
  public void requestFocus() {
    this.styledArea.requestFocus();
  }

  private void highlight() {
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
    this.highlight();
  }

  private void showSuggestions() {
    if (!this.entriesPopup.getItems().isEmpty() && this.getScene() != null)
      this.entriesPopup.show(this.getScene().getWindow());
  }

  private void hideSuggestions() {
    this.entriesPopup.hide();
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
    final var suggestions = entries.stream()
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
        .forEach(this.entriesPopup.getItems()::add);
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
