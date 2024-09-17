package net.darmo_creations.imageslibrary;

import javafx.application.*;
import javafx.stage.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.ui.dialogs.*;
import net.darmo_creations.imageslibrary.utils.*;
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
  public static final String VERSION = "1.0";

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
  @Unmodifiable
  public static final List<String> VALID_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

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

  private static HostServices hostServices;

  /**
   * Open a URL in the user’s default web browser.
   *
   * @param url URL to open.
   */
  public static void openURL(@NotNull String url) {
    hostServices.showDocument(url);
  }

  @Override
  public void start(@NotNull Stage stage) {
    LOGGER.info("Running %s (v%s)".formatted(NAME, VERSION));
    if (config.isDebug())
      LOGGER.info("Debug mode is ON");

    hostServices = this.getHostServices();

    final var splash = new Splash(config);
    splash.show();

    new Thread(() -> {
      final DatabaseConnection db;
      try {
        //noinspection resource
        db = new DatabaseConnection(config.databaseFile());
      } catch (final DatabaseOperationException e) {
        generateCrashReport(e);
        Platform.runLater(() -> {
          splash.hide();
          Alerts.databaseError(
              config,
              e.errorCode(),
              "alert.fatal_error.header",
              "alert.fatal_error.title"
          );
          System.exit(3);
        });
        return;
      }

      Platform.runLater(() -> {
        splash.hide();
        final AppController controller;
        try {
          controller = new AppController(stage, config, db);
        } catch (final DatabaseOperationException e) {
          generateCrashReport(e);
          Alerts.databaseError(
              config,
              e.errorCode(),
              "alert.fatal_error.header",
              "alert.fatal_error.title"
          );
          System.exit(4);
          return;
        } catch (final Exception e) {
          generateCrashReport(e);
          Alerts.error(
              config,
              "alert.fatal_error.header",
              null,
              "alert.fatal_error.title",
              new FormatArg("code", DatabaseErrorCode.UNKNOWN_ERROR)
          );
          System.exit(5);
          return;
        }
        controller.show();
      });
    }, "Database Loader Thread").start();
  }

  public static void main(@NotNull String[] args) {
    final Args parsedArgs;
    try {
      parsedArgs = parseArgs(args);
      config = Config.loadConfig(parsedArgs.debug());
    } catch (final IOException | ParseException | ConfigException e) {
      generateCrashReport(e);
      Alerts.error(
          config,
          "alert.fatal_error.header",
          null,
          "alert.fatal_error.title",
          new FormatArg("code", DatabaseErrorCode.UNKNOWN_ERROR)
      );
      System.exit(1);
    }
    if (config.isDebug())
      // Must be set before calling LoggerFactory.getLogger()
      System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
    LOGGER = LoggerFactory.getLogger("App");
    try {
      launch();
    } catch (final Exception e) {
      generateCrashReport(e.getCause()); // JavaFX wraps exceptions into a RuntimeException
      Alerts.error(
          config,
          "alert.fatal_error.header",
          null,
          "alert.fatal_error.title",
          new FormatArg("code", DatabaseErrorCode.UNKNOWN_ERROR)
      );
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
  private static Args parseArgs(final @NotNull String[] args) throws ParseException {
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
  public static void generateCrashReport(@NotNull Throwable e) {
    final var date = LocalDateTime.now().withNano(0); // Remove nano information
    final var stackTrace = new StringWriter();
    try (final var writer = new PrintWriter(stackTrace)) {
      e.printStackTrace(writer);
    }
    final String template = """
        --- %s (v%s) Crash Report ---
        
        Time: %s
        Description: %s
        
        -- Detailled Stack Trace --
        %s
        
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
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return a list of some system properties.
   */
  public static String getSystemProperties() {
    final StringJoiner systemProperties = new StringJoiner("\n");
    final String userHome = System.getProperty("user.home");
    final String userName = System.getProperty("user.name");
    System.getProperties().entrySet().stream()
        .filter(entry -> {
          final String key = entry.getKey().toString();
          return !key.equals("user.home") && !key.equals("user.name");
        })
        .map(entry -> {
          final Object key = entry.getKey();
          String value = entry.getValue().toString();
          if (value.contains(userHome))
            value = value.replace(userHome, "~");
          if (value.contains(userName))
            value = value.replace(userName, "*USERNAME*");
          if (value.contains("\n"))
            value = value.replace("\n", "\\n");
          if (value.contains("\r"))
            value = value.replace("\n", "\\r");
          return new Pair<>(key, value);
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
