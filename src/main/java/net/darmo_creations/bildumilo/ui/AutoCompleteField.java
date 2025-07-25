package net.darmo_creations.bildumilo.ui;

import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.css.*;
import javafx.event.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import net.darmo_creations.bildumilo.config.*;
import net.darmo_creations.bildumilo.data.*;
import net.darmo_creations.bildumilo.ui.syntax_highlighting.*;
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

  private final Config config;
  private final SuggestionsMenu suggestionsPopup = new SuggestionsMenu();
  private final Set<T> entries;
  private final Predicate<T> entriesFilter;
  private final Function<T, String> stringConverter;
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
   * @param entriesFilter     An optional filter to apply to entries when showing suggestions.
   * @param stringConverter   A function to convert each suggestion into a string.
   * @param syntaxHighlighter An optional syntax highlighter that will color the text.
   * @param cssConverter      If the {@code syntaxHighlighter} argument is specified,
   *                          a function that converts a string returned by {@link Span} objects
   *                          into the appropriate type accepted by the {@code styledArea} object.
   * @param config            The app’s config.
   */
  public AutoCompleteField(
      @NotNull StyledTextArea<S, S> styledArea,
      final @NotNull Set<T> entries,
      Predicate<T> entriesFilter,
      @NotNull Function<T, String> stringConverter,
      SyntaxHighlighter syntaxHighlighter,
      Function<String, S> cssConverter,
      @NotNull Config config
  ) {
    this.entries = Objects.requireNonNull(entries);
    this.entriesFilter = entriesFilter;
    this.stringConverter = Objects.requireNonNull(stringConverter);
    this.cssConverter = cssConverter;
    this.config = config;
    if (!styledArea.getStyleClass().contains("text-input"))
      styledArea.getStyleClass().add("text-input");
    if (!styledArea.getStyleClass().contains("text-area"))
      styledArea.getStyleClass().add("text-area");
    this.styledArea = styledArea;
    this.setSyntaxHighlighter(syntaxHighlighter);
    this.suggestionsPopup.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePopupInteractions);
    styledArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePopupInteractions);
    EventStreams.nonNullValuesOf(styledArea.caretBoundsProperty()).subscribe(opt -> {
      if (opt.isPresent()) {
        final Bounds bounds = opt.get();
        this.suggestionsPopup.setX(bounds.getMaxX() + CARET_X_OFFSET);
        this.suggestionsPopup.setY(bounds.getMaxY() + CARET_Y_OFFSET);
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
      this.fillAndShowSuggestions(newValue);
      this.canShowSuggestions = false;
    });
    styledArea.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.SPACE && event.isShortcutDown())
        this.fillAndShowSuggestions(styledArea.getCaretPosition());
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

  private void handlePopupInteractions(@NotNull KeyEvent event) {
    if (this.suggestionsPopup.isShowing()) {
      switch (event.getCode()) {
        case TAB, ENTER -> {
          for (final var item : this.suggestionsPopup.getItems()) {
            if (item.isSelected()) {
              item.fire();
              event.consume();
              break;
            }
          }
        }
        case UP -> {
          this.suggestionsPopup.selectUp();
          event.consume();
        }
        case DOWN -> {
          this.suggestionsPopup.selectDown();
          event.consume();
        }
        case ESCAPE -> {
          this.suggestionsPopup.hide();
          event.consume();
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
    if (!this.suggestionsPopup.getItems().isEmpty() && this.getScene() != null)
      this.suggestionsPopup.show(this.getScene().getWindow());
  }

  private void hideSuggestions() {
    this.suggestionsPopup.hide();
  }

  private void fillAndShowSuggestions(int caretIndex) {
    final String text = this.getText();
    final String beforeCaret = text.substring(0, caretIndex);
    final Matcher matcher = WORD_START_PATTERN.matcher(beforeCaret);
    final String wordBegining = matcher.find() ? matcher.group(1) : "";
    final List<String> suggestions;
    if (wordBegining.isEmpty())
      suggestions = List.of();
    else
      suggestions = this.entries.stream()
          .filter(t -> this.entriesFilter != null && this.entriesFilter.test(t))
          .map(this.stringConverter)
          .filter(s -> s.startsWith(wordBegining) && !s.equals(wordBegining))
          .sorted()
          .toList();
    if (!suggestions.isEmpty()) {
      this.populatePopup(suggestions);
      if (!this.suggestionsPopup.isShowing())
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
    this.suggestionsPopup.getItems().clear();
    suggestions.stream()
        .limit(MAX_SHOWN_SUGGESTIONS)
        .map(this::newMenuItem)
        .forEach(menuItem -> this.suggestionsPopup.getItems().add(menuItem));
  }

  private SuggestionsMenuItem newMenuItem(@NotNull String suggestion) {
    final SuggestionsMenuItem item = new SuggestionsMenuItem(suggestion);
    item.setOnAction(event -> {
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

  /**
   * A popup that shows a list of clickable suggestions.
   */
  private class SuggestionsMenu extends PopupControl {
    private final ObservableList<SuggestionsMenuItem> items = FXCollections.observableArrayList();

    private boolean initialized = false;

    public SuggestionsMenu() {
      final VBox root = new VBox();
      root.getStyleClass().add("suggestions-menu");
      this.getScene().setRoot(root);

      this.items.addListener((ListChangeListener<? super SuggestionsMenuItem>) change -> {
        while (change.next()) {
          for (final SuggestionsMenuItem removed : change.getRemoved()) {
            root.getChildren().remove(removed);
          }
          for (final SuggestionsMenuItem added : change.getAddedSubList()) {
            root.getChildren().add(added);
            added.selectedProperty().addListener((observable, oldValue, newValue) -> {
              if (newValue)
                this.items.filtered(item -> item != added && item.isSelected())
                    .forEach(item -> item.setSelected(false));
            });
          }
        }
        if (!this.items.isEmpty())
          this.items.get(0).setSelected(true);
      });

      this.setOnShowing(event -> {
        if (!this.initialized) {
          AutoCompleteField.this.config.theme().applyTo(root);
          this.initialized = true;
        }
        for (int i = 0; i < this.items.size(); i++)
          this.items.get(i).setSelected(i == 0);
      });
    }

    public ObservableList<SuggestionsMenuItem> getItems() {
      return this.items;
    }

    public void selectUp() {
      final int size = this.items.size();
      for (int i = 0; i < size; i++) {
        if (this.items.get(i).isSelected()) {
          this.items.get(i == 0 ? size - 1 : i - 1).setSelected(true);
          break;
        }
      }
    }

    public void selectDown() {
      final int size = this.items.size();
      for (int i = 0; i < size; i++) {
        if (this.items.get(i).isSelected()) {
          this.items.get((i + 1) % size).setSelected(true);
          break;
        }
      }
    }
  }

  private static class SuggestionsMenuItem extends AnchorPane {
    public SuggestionsMenuItem(@NotNull String text) {
      final Label label = new Label(text);
      AnchorPane.setLeftAnchor(label, 0.0);
      AnchorPane.setRightAnchor(label, 0.0);
      AnchorPane.setTopAnchor(label, 0.0);
      AnchorPane.setBottomAnchor(label, 0.0);
      this.getChildren().add(label);

      this.getStyleClass().add("suggestions-menu-item");
      this.setPadding(new Insets(2));
      this.setCursor(Cursor.HAND);

      this.selected.addListener((observable, oldValue, newValue) ->
          this.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), newValue));
      this.setOnMouseEntered(event -> this.setSelected(true));
      this.setOnMouseClicked(event -> this.fire());
    }

    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);

    public BooleanProperty selectedProperty() {
      return this.selected;
    }

    public boolean isSelected() {
      return this.selected.get();
    }

    public void setSelected(boolean selected) {
      this.selected.set(selected);
    }

    private final ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>() {
      @Override
      protected void invalidated() {
        SuggestionsMenuItem.this.setEventHandler(ActionEvent.ACTION, this.get());
      }

      @Override
      public Object getBean() {
        return SuggestionsMenuItem.this;
      }

      @Override
      public String getName() {
        return "onAction";
      }
    };

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
      return this.onAction;
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
      this.onActionProperty().set(value);
    }

    public void fire() {
      this.fireEvent(new ActionEvent());
    }
  }
}
