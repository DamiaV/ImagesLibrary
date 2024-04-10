package net.darmo_creations.imageslibrary;

import javafx.application.*;
import javafx.stage.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class App extends Application {
  public static final String NAME = "Image Library";
  public static final String VERSION = "1.0-SNAPSHOT";
  public static final Logger LOGGER = LoggerFactory.getLogger("App");

  /**
   * The list of accepted image file extensions.
   */
  @Unmodifiable
  public static final List<String> VALID_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

  /**
   * Jar path to the resources’ root directory.
   */
  public static final String RESOURCES_ROOT = "/net/darmo_creations/imageslibrary/";
  /**
   * Jar path to the images directory.
   */
  public static final String IMAGES_PATH = RESOURCES_ROOT + "images/";

  /**
   * Application’s controller.
   */
  private static AppController controller;
  /**
   * App’s global configuration object.
   */
  private static Config config;

  /**
   * Return the application’s configuration object.
   */
  public static Config config() {
    return config;
  }

  /**
   * Application’s resource bundlo for the currently selected language.
   */
  private static ResourceBundle resourceBundle;

  /**
   * Return the resource bundle of the currently selected language.
   */
  public static ResourceBundle getResourceBundle() {
    if (resourceBundle == null)
      resourceBundle = config().language().resources();
    return resourceBundle;
  }

  /**
   * Update the current configuration object with the given one.
   * <p>
   * Only the options that do <b>not</b> need a restart are copied.
   *
   * @param localConfig Configuration object to copy from.
   */
  public static void updateConfig(final Config localConfig) {
    config.setCaseSensitiveQueriesByDefault(localConfig.caseSensitiveQueriesByDefault());
    config.setMaxImagesShown(localConfig.maxImagesShown());
    controller.onConfigUpdate();
  }

  @Override
  public void start(Stage stage) {
    LOGGER.info("Running %s (v%s)".formatted(NAME, VERSION));
    if (config.isDebug()) {
      // TODO put logger at debug level
      LOGGER.info("Debug mode is ON");
    }
    try {
      controller = new AppController(stage);
      controller.show();
    } catch (DatabaseOperationException e) {
      Alerts.databaseError(e.errorCode());
    }
  }

  public static void main(String[] args) {
    final Args parsedArgs;
    try {
      parsedArgs = parseArgs(args);
      config = Config.loadConfig(parsedArgs.debug());
    } catch (IOException | ParseException | ConfigException e) {
      generateCrashReport(e);
      System.exit(1);
    }
    try {
      launch();
    } catch (Exception e) {
      generateCrashReport(e.getCause()); // JavaFX wraps exceptions into a RuntimeException
      System.exit(2);
    }
  }

  /**
   * Parse the CLI arguments.
   *
   * @param args Raw CLI arguments.
   * @return An object containing parsed arguments.
   * @throws ParseException If arguments could not be parsed.
   */
  private static Args parseArgs(final String[] args) throws ParseException {
    final CommandLineParser parser = new DefaultParser();
    final Options options = new Options();
    options.addOption(Option.builder("d")
        .desc("Run the application in debug mode")
        .longOpt("debug")
        .build());
    final CommandLine commandLine = parser.parse(options, args);
    return new Args(commandLine.hasOption('d'));
  }

  /**
   * Generate a crash report from the given throwable object.
   *
   * @param e The throwable object that caused the unrecoverable crash.
   */
  public static void generateCrashReport(Throwable e) {
    final var date = LocalDateTime.now();
    final var stackTrace = new StringWriter();
    try (final var writer = new PrintWriter(stackTrace)) {
      e.printStackTrace(writer);
    }
    final String template = """
        --- %s (v%s) Crash Report ---
        .
        Time: %s
        Description: %s
        .
        -- Detailled Stack Trace --
        %s
        .
        -- Technical Information --
        System properties:
        %s
        """;
    final String message = template.formatted(
        NAME,
        VERSION,
        date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        e.getMessage(),
        stackTrace,
        getSystemProperties()
    );
    LOGGER.error(message);
    final Path logsDir = Path.of("logs");
    final String fileName = "crash_report_%s.log".formatted(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    try {
      if (!Files.exists(logsDir))
        Files.createDirectory(logsDir);
      Files.writeString(logsDir.resolve(fileName), message);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return a list of some system properties.
   */
  public static String getSystemProperties() {
    final var systemProperties = new StringJoiner("\n");
    System.getProperties().entrySet().stream()
        .filter(entry -> {
          final String key = entry.getKey().toString();
          return !key.startsWith("user.")
                 && !key.startsWith("file.")
                 && !key.startsWith("jdk.")
                 && !key.contains(".path")
                 && !key.contains("path.")
                 && !key.equals("line.separator")
                 && !key.equals("java.home");
        })
        .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
        .forEach(property -> systemProperties.add("%s: %s".formatted(property.getKey(), property.getValue())));
    return systemProperties.toString();
  }

  /**
   * Class holding parsed CLI arguments.
   *
   * @param debug Whether to run the app in debug mode.
   */
  private record Args(boolean debug) {
  }
}
