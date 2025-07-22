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
   * Open a dialog to choose a {@code .sqlite3} file.
   *
   * @param config The app’s config.
   * @param stage  The parent stage object.
   * @return The selected file.
   */
  public static Optional<Path> showDatabaseFileChooser(
      final @NotNull Config config,
      final @NotNull Window stage
  ) {
    return openFileChooser(
        config,
        stage,
        "database_file_chooser",
        List.of(DatabaseConnection.DATABASE_FILE_EXT)
    );
  }

  /**
   * Open a dialog to choose media files.
   *
   * @param config The app’s config.
   * @param stage  The parent stage object.
   * @return The selected file.
   */
  @Unmodifiable
  public static List<Path> showMediaFilesChooser(
      final @NotNull Config config,
      final @NotNull Window stage
  ) {
    return openMultipleFilesChooser(
        config,
        stage,
        "image_files_chooser",
        null,
        App.VALID_FILE_EXTENSIONS
    );
  }

  /**
   * Open a dialog to choose a directory.
   *
   * @param config The app’s config.
   * @param stage  The parent stage object.
   * @return The selected directory.
   */
  public static Optional<Path> showDirectoryChooser(
      final @NotNull Config config,
      @NotNull Window stage
  ) {
    final var fileChooser = new DirectoryChooser();
    fileChooser.setTitle(config.language().translate("dialog.directory_chooser.title"));
    return Optional.ofNullable(fileChooser.showDialog(stage)).map(File::toPath);
  }

  /**
   * Open a single file chooser.
   *
   * @param config     The app’s config.
   * @param stage      The parent stage object.
   * @param dialogName Name of the file chooser.
   * @param extensions Allowed file extensions.
   * @return The selected file.
   */
  private static Optional<Path> openFileChooser(
      final @NotNull Config config,
      @NotNull Window stage,
      @NotNull String dialogName,
      final @NotNull List<String> extensions
  ) {
    final File file = buildFileChooser(config, dialogName, null, extensions).showOpenDialog(stage);
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
   * @param config      The app’s config.
   * @param stage       The parent stage object.
   * @param dialogName  Name of the file chooser.
   * @param defaultName Default file name.
   * @param extensions  Allowed file extensions.
   * @return The selected files.
   */
  @Unmodifiable
  private static List<Path> openMultipleFilesChooser(
      final @NotNull Config config,
      @NotNull Window stage,
      @NotNull String dialogName,
      String defaultName,
      final @NotNull List<String> extensions
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
   * @param config      The app’s config.
   * @param stage       The parent stage object.
   * @param dialogName  Name of the file saver.
   * @param defaultName Default file name.
   * @param extensions  Allowed file extensions.
   * @return The selected file.
   */
  private static Optional<Path> openSaveFileChooser(
      final @NotNull Config config,
      @NotNull Window stage,
      @NotNull String dialogName,
      String defaultName,
      final @NotNull List<String> extensions
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
      final @NotNull Config config,
      @NotNull String dialogName,
      String defaultName,
      final @NotNull List<String> extensions
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
