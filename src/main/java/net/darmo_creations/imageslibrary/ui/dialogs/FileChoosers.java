package net.darmo_creations.imageslibrary.ui.dialogs;

import javafx.stage.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import net.darmo_creations.imageslibrary.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This class provides methods to open file saver/chooser dialogs.
 */
public final class FileChoosers {
  /**
   * Open a dialog to choose a .reg file.
   *
   * @param stage       The parent stage object.
   * @param defaultName Default file name.
   * @return The selected file.
   */
  public static Optional<Path> showDatabaseFileChooser(
      final Config config,
      final Window stage,
      @Nullable String defaultName
  ) {
    return openFileChooser(
        config,
        stage,
        "database_file_chooser",
        defaultName,
        List.of(DatabaseConnection.DATABASE_FILE_EXT)
    );
  }

  /**
   * Open a dialog to choose an image file.
   *
   * @param config      The current config.
   * @param stage       The parent stage object.
   * @param defaultName Default file name.
   * @return The selected file.
   */
  @Unmodifiable
  public static List<Path> showImagesFileChooser(
      final Config config,
      final Window stage,
      @Nullable String defaultName
  ) {
    return openMultipleFilesChooser(
        config,
        stage,
        "image_file_chooser",
        defaultName,
        App.VALID_EXTENSIONS
    );
  }

  /**
   * Open a single file chooser.
   *
   * @param config      The current config.
   * @param stage       The parent stage object.
   * @param dialogName  Name of the file chooser.
   * @param defaultName Default file name.
   * @param extensions  Allowed file extensions.
   * @return The selected file.
   */
  private static Optional<Path> openFileChooser(
      final Config config,
      Window stage,
      String dialogName,
      @Nullable String defaultName,
      final List<String> extensions
  ) {
    final File file = buildFileChooser(config, dialogName, defaultName, extensions).showOpenDialog(stage);
    if (file == null)
      return Optional.empty();
    final String fileName = file.getName();
    if (extensions.stream().noneMatch(fileName::endsWith))
      return Optional.empty();
    return Optional.of(file.toPath().toAbsolutePath());
  }

  /**
   * Open a multiple file chooser.
   *
   * @param config      The current config.
   * @param stage       The parent stage object.
   * @param dialogName  Name of the file chooser.
   * @param defaultName Default file name.
   * @param extensions  Allowed file extensions.
   * @return The selected files.
   */
  @Unmodifiable
  private static List<Path> openMultipleFilesChooser(
      final Config config,
      Window stage,
      String dialogName,
      @Nullable String defaultName,
      final List<String> extensions
  ) {
    final List<File> file = buildFileChooser(config, dialogName, defaultName, extensions).showOpenMultipleDialog(stage);
    if (file == null)
      return List.of();
    return file.stream()
        .map(File::toPath)
        .filter(path -> extensions.contains(FileUtils.getExtension(path)))
        .toList();
  }

  /**
   * Open a file saver.
   *
   * @param config      The current config.
   * @param stage       The parent stage object.
   * @param dialogName  Name of the file saver.
   * @param defaultName Default file name.
   * @param extensions  Allowed file extensions.
   * @return The selected file.
   */
  private static Optional<Path> openSaveFileChooser(
      final Config config,
      Window stage,
      String dialogName,
      @Nullable String defaultName,
      final List<String> extensions
  ) {
    final File file = buildFileChooser(config, dialogName, defaultName, extensions).showSaveDialog(stage);
    if (file == null)
      return Optional.empty();
    Path path = file.toPath().toAbsolutePath();
    if (!extensions.contains(FileUtils.getExtension(file.toPath())))
      path = path.getParent().resolve(path.getFileName() + extensions.get(0));
    return Optional.of(path);
  }

  private static FileChooser buildFileChooser(
      final Config config,
      String dialogName,
      @Nullable String defaultName,
      final List<String> extensions
  ) {
    final var fileChooser = new FileChooser();
    fileChooser.setTitle(config.language().translate("dialog.%s.title".formatted(dialogName)));
    final List<String> exts = extensions.stream().map(e -> "*." + e).toList();
    final String desc = config.language().translate(
        "dialog.%s.filter_description".formatted(dialogName),
        new FormatArg("exts", String.join(", ", exts))
    );
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, exts));
    if (defaultName != null) {
      if (!defaultName.endsWith(extensions.get(0))) {
        defaultName += extensions.get(0);
      }
      fileChooser.setInitialFileName(defaultName);
    }
    return fileChooser;
  }

  private FileChoosers() {
  }
}
