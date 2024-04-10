package net.darmo_creations.imageslibrary.config;

import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

/**
 * This class represents a language.
 */
public final class Language {
  private static final Pattern PLURAL_SUFFIX_PATTERN =
      Pattern.compile("^(\\w+(?:\\.\\w+)*)\\.plural(?:_([2-9]|[1-9]\\d+))?$");

  private final String code;
  private final String name;
  private final Locale locale;
  private final ResourceBundle resources;
  /**
   * Mapping of plurals structured as
   * {base key: {count: plural text template}}
   */
  private final Map<String, Map<Integer, String>> plurals = new HashMap<>();
  private static final int DEFAULT_PLURAL_KEY = -1;

  /**
   * Create a language for the given code and resource.
   *
   * @param code      Language’s code.
   * @param name      Language’s name in the language itself.
   * @param locale    Language’s locale.
   * @param resources Language’s resources, i.e. translations.
   */
  public Language(
      String code,
      String name,
      Locale locale,
      final ResourceBundle resources
  ) {
    this.code = Objects.requireNonNull(code);
    this.name = Objects.requireNonNull(name);
    this.locale = Objects.requireNonNull(locale);
    this.resources = Objects.requireNonNull(resources);
    this.extractSuffixes();
  }

  /**
   * Extract the plurals and day suffixes.
   * <p>
   * Day suffixes must be specified in the form {@code calendar.suffix.<pattern>} where pattern may be one of:
   * <li>{@code <digits>}: for a specific day number</li>
   * <li>{@code *<digits>}: for all days ending with specific digits</li>
   * <li>{@code *}: for all days</li>
   * <p>
   * Plurals must be specified in the form {@code <base_key>.plural[_<number>]}
   * where base key is an existing translation key. If {@code <number>} is specified,
   * the value is applied only when this specific count is passed
   * to {@link #translate(String, Integer, FormatArg...)}.
   */
  private void extractSuffixes() {
    final Map<String, Map<Integer, String>> plurals = new HashMap<>();
    final var iterator = this.resources.getKeys().asIterator();
    while (iterator.hasNext()) {
      final String key = iterator.next();
      final Matcher matcher = PLURAL_SUFFIX_PATTERN.matcher(key);
      if (matcher.matches()) {
        final String baseKey = matcher.group(1);
        if (!plurals.containsKey(baseKey))
          plurals.put(baseKey, new HashMap<>());
        final String number = matcher.group(2);
        final String value = this.resources.getString(key);
        if (number == null || number.isEmpty())
          plurals.get(baseKey).put(DEFAULT_PLURAL_KEY, value);
        else
          plurals.get(baseKey).put(Integer.parseInt(number), value);
      }
    }
    this.plurals.putAll(plurals);
  }

  public String code() {
    return this.code;
  }

  public String name() {
    return this.name;
  }

  public Locale locale() {
    return this.locale;
  }

  public ResourceBundle resources() {
    return this.resources;
  }

  /**
   * Translate a key and format the resulting text.
   *
   * @param key        Resource bundle key to translate.
   * @param formatArgs Format arguments to use to format the translated text.
   * @return The translated and formatted text.
   */
  public String translate(String key, final FormatArg... formatArgs) {
    return this.translate(key, null, formatArgs);
  }

  /**
   * Translate a key and format the resulting text, using the given count as the grammatical number.
   *
   * @param key        Resource bundle key to translate.
   * @param count      The grammatical number. May be null.
   * @param formatArgs Format arguments to use to format the translated text.
   * @return The translated and formatted text.
   */
  public String translate(String key, @Nullable Integer count, final FormatArg... formatArgs) {
    String text = null;
    if (count != null && count > 1) {
      final var p = this.plurals.get(key);
      if (p != null) {
        text = p.get(count);
        if (text == null)
          text = p.get(DEFAULT_PLURAL_KEY);
      }
    }
    if (text == null) {
      try {
        text = this.resources.getString(key);
      } catch (MissingResourceException e) {
        App.LOGGER.warn("Can’t find key {}", key);
        return key;
      }
    }
    if (formatArgs.length != 0)
      return StringUtils.format(text, formatArgs);
    return text;
  }

  /**
   * Check whether a specific key is defined for this language.
   *
   * @param key The key to check.
   * @return True if the key exists, false otherwise.
   */
  public boolean hasKey(String key) {
    return this.resources.containsKey(key);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final var that = (Language) obj;
    return Objects.equals(this.code, that.code) &&
           Objects.equals(this.name, that.name) &&
           Objects.equals(this.locale, that.locale) &&
           Objects.equals(this.resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.code, this.name, this.locale, this.resources);
  }
}
