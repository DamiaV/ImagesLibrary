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

  private static Logger LOGGER;

  /**
   * This app’s logger.
   */
  public static Logger logger() {
    return LOGGER;
  }

  /**
   * The list of accepted image file extensions.
   */
  // TODO add support for WEBP
  @Unmodifiable
  public static final List<String> VALID_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

  /**
   * Jar path to the resources’ root directory.
   */
  public static final String RESOURCES_ROOT = "/net/darmo_creations/imageslibrary/";
  /**
   * Jar path to the images directory.
   */
  public static final String IMAGES_PATH = RESOURCES_ROOT + "images/";

  /**
   * App’s global configuration object.
   */
  private static Config config;
  /**
   * Application’s resource bundle for the currently selected language.
   */
  private static ResourceBundle resourceBundle;

  /**
   * Return the resource bundle of the currently selected language.
   */
  public static ResourceBundle getResourceBundle() {
    if (resourceBundle == null)
      resourceBundle = config.language().resources();
    return resourceBundle;
  }

  @Override
  public void start(Stage stage) {
    LOGGER.info("Running %s (v%s)".formatted(NAME, VERSION));
    if (config.isDebug())
      LOGGER.info("Debug mode is ON");

    final var splash = new Splash(config);
    splash.show();

    new Thread(() -> {
      final DatabaseConnection db;
      try {
        //noinspection resource
        db = new DatabaseConnection(config.databaseFile());
      } catch (DatabaseOperationException e) {
        generateCrashReport(e);
        Platform.runLater(() -> {
          splash.hide();
          Alerts.databaseError(config, e.errorCode());
        });
        return;
      }

      Platform.runLater(() -> {
        // FIXME never executed if splash is closed before DB is loaded
        splash.hide();
        final AppController controller;
        try {
          controller = new AppController(stage, config, db);
        } catch (DatabaseOperationException e) {
          generateCrashReport(e);
          Alerts.databaseError(config, e.errorCode());
          return;
        }
        controller.show();
      });
    }, "Database Loader Thread").start();
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
    if (config.isDebug())
      // Must be set before calling LoggerFactory.getLogger()
      System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
    LOGGER = LoggerFactory.getLogger("App");
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
    if (LOGGER != null)
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
