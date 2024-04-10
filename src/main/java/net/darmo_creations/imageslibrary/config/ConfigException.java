package net.darmo_creations.imageslibrary.config;

/**
 * Exception indicating that a problem occured while loading a configuration file.
 */
public class ConfigException extends Exception {
  public ConfigException(String message) {
    super(message);
  }

  public ConfigException(Throwable cause) {
    super(cause);
  }
}
