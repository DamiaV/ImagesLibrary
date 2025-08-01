package net.darmo_creations.bildumilo.config;

import javafx.beans.property.*;
import net.darmo_creations.bildumilo.*;
import net.darmo_creations.bildumilo.themes.*;
import net.darmo_creations.bildumilo.utils.*;
import org.ini4j.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This class holds configuration options for the whole application.
 * <p>
 * All options except debug are mutable at runtime. But some will require the configuration to be saved to disk
 * and the application to be restarted to apply.
 */
public final class Config {
  /**
   * Array of all available language codes.
   */
  private static final String[] LANGUAGE_CODES = {
      "en",
      "fr",
      "eo",
  };
  private static final String DEFAULT_LANGUAGE_CODE = LANGUAGE_CODES[0];
  private static final Map<String, Language> LANGUAGES = new HashMap<>();
  private static final String DEFAULT_DB_FILE = "db.sqlite3";
  private static final int DEFAULT_SLIDESHOW_DELAY = 5; // s
  public static final int MIN_SLIDESHOW_DELAY = 2;
  public static final int MAX_SLIDESHOW_DELAY = 30;

  private static final Path SETTINGS_FILE = Path.of("settings.ini");

  private static final String APP_SECTION = "App";
  private static final String LANGUAGE_OPTION = "language";
  private static final String THEME_OPTION = "theme";
  private static final String DB_FILE = "database_file";
  private static final String QUERIES_SECTION = "Queries";
  private static final String CASE_SENSITIVITE_BY_DEFAULT = "case_sensitive_by_default";
  private static final String QUERY_SYNTAX_HIGHLIGHTING = "syntax_highlighting";
  private static final String SLIDESHOW_SECTION = "Slideshow";
  private static final String SHUFFLE_SLIDESHOW_IMAGES = "shuffle";
  private static final String SLIDESHOW_DELAY = "delay";

  /**
   * Load the configuration from the settings file.
   * <p>
   * It loads all available resource bundles and themes.
   *
   * @param debug Value of the debug option.
   * @return A configuration object.
   * @throws IOException     If an IO error occurs.
   * @throws ConfigException If the file could not be parsed correctly.
   */
  @Contract(value = "_ -> new")
  public static Config loadConfig(boolean debug) throws IOException, ConfigException {
    loadLanguages();
    Theme.loadThemes();

    final Wini ini = getOrCreateIniFile();

    final String langCode = StringUtils.stripNullable(ini.get(APP_SECTION, LANGUAGE_OPTION)).orElse(DEFAULT_LANGUAGE_CODE);
    if (!LANGUAGES.containsKey(langCode))
      throw new ConfigException("unsupported language code: " + langCode);

    final String themeID = StringUtils.stripNullable(ini.get(APP_SECTION, THEME_OPTION)).orElse(Theme.DEFAULT_THEME_ID);
    final Theme theme = Theme.getTheme(themeID).orElseThrow(() -> new ConfigException("undefined theme: " + themeID));

    final String path = StringUtils.stripNullable(ini.get(APP_SECTION, DB_FILE, String.class)).orElse(DEFAULT_DB_FILE);
    final Path databaseFile = Paths.get(path);

    final boolean caseSensitiveDefault = Optional.ofNullable(ini.get(QUERIES_SECTION, CASE_SENSITIVITE_BY_DEFAULT, Boolean.class)).orElse(false);
    final boolean querySH = Optional.ofNullable(ini.get(QUERIES_SECTION, QUERY_SYNTAX_HIGHLIGHTING, Boolean.class)).orElse(false);

    final boolean shuffle = Optional.ofNullable(ini.get(SLIDESHOW_SECTION, SHUFFLE_SLIDESHOW_IMAGES, Boolean.class)).orElse(false);
    int delay = Optional.ofNullable(ini.get(SLIDESHOW_SECTION, SLIDESHOW_DELAY, Integer.class)).orElse(DEFAULT_SLIDESHOW_DELAY);
    if (delay < MIN_SLIDESHOW_DELAY)
      delay = MIN_SLIDESHOW_DELAY;
    else if (delay > MAX_SLIDESHOW_DELAY)
      delay = MAX_SLIDESHOW_DELAY;

    try {
      return new Config(
          LANGUAGES.get(langCode),
          theme,
          databaseFile,
          caseSensitiveDefault,
          querySH,
          shuffle,
          delay,
          debug
      );
    } catch (final IllegalArgumentException e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Return the Ini file designated by {@link #SETTINGS_FILE}. If the file does not exist, it is created.
   *
   * @return The {@link Wini} wrapper object.
   * @throws IOException If the file does not exist and could not be created.
   */
  private static Wini getOrCreateIniFile() throws IOException {
    if (!Files.exists(SETTINGS_FILE))
      Files.createFile(SETTINGS_FILE);
    return new Wini(SETTINGS_FILE.toFile());
  }

  /**
   * Load resource bundles for all available languages and populate {@link #LANGUAGES} field.
   *
   * @throws IOException If any IO error occurs.
   */
  private static void loadLanguages() throws IOException {
    LANGUAGES.clear();
    for (final var langCode : LANGUAGE_CODES) {
      final ResourceBundle bundle = getResourceBundle(new Locale(langCode));
      if (bundle != null) {
        final String langName = bundle.getString("language_name");
        LANGUAGES.put(langCode, new Language(langCode, langName, new Locale(langCode), bundle));
      }
    }
    if (LANGUAGES.isEmpty()) {
      throw new IOException("no languages found");
    }
  }

  /**
   * Return the resource bundle for the given locale.
   *
   * @param locale A locale.
   * @return The locale’s resources.
   */
  private static ResourceBundle getResourceBundle(@NotNull Locale locale) {
    return ResourceBundle.getBundle(
        App.RESOURCES_ROOT.substring(1).replace('/', '.') + "translations.ui",
        locale
    );
  }

  /**
   * List of all available languages.
   *
   * @return A new copy of the internal list.
   */
  @Contract(pure = true, value = "-> new")
  @Unmodifiable
  public static List<Language> languages() {
    return LANGUAGES.values().stream().sorted(Comparator.comparing(lang -> lang.name().toLowerCase())).toList();
  }

  private final Language language;
  private final Theme theme;
  private final Path databaseFile;
  private final boolean debug;
  private final BooleanProperty caseSensitiveQueriesByDefault = new SimpleBooleanProperty(this, "case_sensitive_by_default", false);
  private final BooleanProperty querySyntaxHighlighting = new SimpleBooleanProperty(this, "query_syntax_highlighting", false);
  private final BooleanProperty shuffleSlideshows = new SimpleBooleanProperty(this, "shuffle_slideshows", false);
  private final IntegerProperty slideshowDelay = new SimpleIntegerProperty(this, "slideshow_delay", DEFAULT_SLIDESHOW_DELAY);

  /**
   * Create a configuration object.
   *
   * @param language                      Language to use.
   * @param theme                         Theme to use.
   * @param databaseFile                  Path to the database file.
   * @param caseSensitiveQueriesByDefault Whether pseudo-tag pattern should be treated as case sensitive when no flag is present.
   * @param querySyntaxHighlighting       Whether to perform syntax highlighting in the tag query search bar.
   * @param shuffleSlideshows             Whether slideshow images should be shuffled.
   * @param slideshowDelay                The delay between each image in slideshows in seconds.
   * @param debug                         Whether to run the app in debug mode.
   */
  public Config(
      @NotNull Language language,
      @NotNull Theme theme,
      @NotNull Path databaseFile,
      boolean caseSensitiveQueriesByDefault,
      boolean querySyntaxHighlighting,
      boolean shuffleSlideshows,
      int slideshowDelay,
      boolean debug
  ) {
    this.language = Objects.requireNonNull(language);
    this.theme = Objects.requireNonNull(theme);
    this.databaseFile = databaseFile.toAbsolutePath();
    this.setCaseSensitiveQueriesByDefault(caseSensitiveQueriesByDefault);
    this.setQuerySyntaxHighlightingEnabled(querySyntaxHighlighting);
    this.setShuffleSlideshowsEnabled(shuffleSlideshows);
    this.setSlideshowDelay(slideshowDelay);
    this.debug = debug;
  }

  /**
   * The language to use.
   */
  public Language language() {
    return this.language;
  }

  /**
   * The theme to use.
   */
  public Theme theme() {
    return this.theme;
  }

  /**
   * The path to the database file to use.
   */
  public Path databaseFile() {
    return this.databaseFile;
  }

  public BooleanProperty caseSensitiveQueriesByDefaultProperty() {
    return this.caseSensitiveQueriesByDefault;
  }

  /**
   * Whether pseudo-tag pattern should be treated as case sensitive when no flag is present.
   */
  public boolean caseSensitiveQueriesByDefault() {
    return this.caseSensitiveQueriesByDefault.get();
  }

  /**
   * Set whether pseudo-tag pattern should be treated as case sensitive when no flag is present.
   *
   * @param caseSensitiveQueriesByDefault The new value.
   */
  public void setCaseSensitiveQueriesByDefault(boolean caseSensitiveQueriesByDefault) {
    this.caseSensitiveQueriesByDefault.set(caseSensitiveQueriesByDefault);
  }

  public BooleanProperty querySyntaxHighlightingProperty() {
    return this.querySyntaxHighlighting;
  }

  /**
   * Whether tag query syntax highlighting is enabled.
   */
  public boolean isQuerySyntaxHighlightingEnabled() {
    return this.querySyntaxHighlighting.get();
  }

  /**
   * Set whether tag query syntax highlighting is enabled.
   *
   * @param querySyntaxHighlighting The new value.
   */
  public void setQuerySyntaxHighlightingEnabled(boolean querySyntaxHighlighting) {
    this.querySyntaxHighlighting.set(querySyntaxHighlighting);
  }

  public BooleanProperty shuffleSlideshowsProperty() {
    return this.shuffleSlideshows;
  }

  /**
   * Whether slideshow images should be shuffled.
   */
  public boolean isShuffleSlideshowsEnabled() {
    return this.shuffleSlideshows.get();
  }

  /**
   * Set whether slideshow images should be shuffled.
   *
   * @param shuffleSlideshows The new value.
   */
  public void setShuffleSlideshowsEnabled(boolean shuffleSlideshows) {
    this.shuffleSlideshows.set(shuffleSlideshows);
  }

  public IntegerProperty slideshowDelayProperty() {
    return this.slideshowDelay;
  }

  /**
   * The delay between each image in slideshows in seconds.
   */
  public int slideshowDelay() {
    return this.slideshowDelay.get();
  }

  /**
   * Set the delay between each image in slideshows in seconds.
   *
   * @param slideshowDelay The new value.
   */
  public void setSlideshowDelay(int slideshowDelay) {
    this.slideshowDelay.set(slideshowDelay);
  }

  /**
   * Whether the app is in debug mode.
   */
  public boolean isDebug() {
    return this.debug;
  }

  /**
   * Return a copy of this object and replace its language by the given one.
   *
   * @param language The language to use.
   * @return A new configuration object.
   */
  @Contract(pure = true, value = "_ -> new")
  public Config withLanguage(@NotNull Language language) {
    return new Config(
        language,
        this.theme,
        this.databaseFile,
        this.caseSensitiveQueriesByDefault.get(),
        this.querySyntaxHighlighting.get(),
        this.shuffleSlideshows.get(),
        this.slideshowDelay.get(),
        this.debug
    );
  }

  /**
   * Return a copy of this object and replace its theme by the given one.
   *
   * @param theme The theme to use.
   * @return A new configuration object.
   */
  @Contract(pure = true, value = "_ -> new")
  public Config withTheme(@NotNull Theme theme) {
    return new Config(
        this.language,
        theme,
        this.databaseFile,
        this.caseSensitiveQueriesByDefault.get(),
        this.querySyntaxHighlighting.get(),
        this.shuffleSlideshows.get(),
        this.slideshowDelay.get(),
        this.debug
    );
  }

  /**
   * Return a copy of this object and replace its database file path by the given one.
   *
   * @param path The database file to use.
   * @return A new configuration object.
   */
  @Contract(pure = true, value = "_ -> new")
  public Config withDatabaseFile(@NotNull Path path) {
    return new Config(
        this.language,
        this.theme,
        path,
        this.caseSensitiveQueriesByDefault.get(),
        this.querySyntaxHighlighting.get(),
        this.shuffleSlideshows.get(),
        this.slideshowDelay.get(),
        this.debug
    );
  }

  /**
   * Clone this object.
   *
   * @return A new deep copy of this object.
   */
  public Config copy() {
    return new Config(
        this.language,
        this.theme,
        this.databaseFile,
        this.caseSensitiveQueriesByDefault.get(),
        this.querySyntaxHighlighting.get(),
        this.shuffleSlideshows.get(),
        this.slideshowDelay.get(),
        this.debug
    );
  }

  /**
   * Save this configuration object to the disk.
   */
  public void save() throws IOException {
    App.logger().info("Saving config…");
    final Wini ini = getOrCreateIniFile();
    ini.put(APP_SECTION, LANGUAGE_OPTION, this.language.code());
    ini.put(APP_SECTION, THEME_OPTION, this.theme.id());
    ini.put(APP_SECTION, DB_FILE, this.databaseFile);
    ini.put(QUERIES_SECTION, CASE_SENSITIVITE_BY_DEFAULT, this.caseSensitiveQueriesByDefault.get());
    ini.put(QUERIES_SECTION, QUERY_SYNTAX_HIGHLIGHTING, this.querySyntaxHighlighting.get());
    ini.put(SLIDESHOW_SECTION, SHUFFLE_SLIDESHOW_IMAGES, this.shuffleSlideshows.get());
    ini.put(SLIDESHOW_SECTION, SLIDESHOW_DELAY, this.slideshowDelay.get());
    ini.store();
    App.logger().info("Done.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || this.getClass() != o.getClass()) return false;
    final var config = (Config) o;
    return this.debug == config.debug
           && this.caseSensitiveQueriesByDefault.get() == config.caseSensitiveQueriesByDefault.get()
           && this.querySyntaxHighlighting.get() == config.querySyntaxHighlighting.get()
           && this.shuffleSlideshows.get() == config.shuffleSlideshows.get()
           && this.slideshowDelay.get() == config.slideshowDelay.get()
           && Objects.equals(this.language, config.language)
           && Objects.equals(this.theme, config.theme)
           && Objects.equals(this.databaseFile, config.databaseFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.language,
        this.theme,
        this.databaseFile,
        this.debug,
        this.caseSensitiveQueriesByDefault.get(),
        this.querySyntaxHighlighting.get(),
        this.shuffleSlideshows.get(),
        this.slideshowDelay.get()
    );
  }
}
