package net.darmo_creations.imageslibrary.utils;

import javafx.application.*;
import javafx.embed.swing.*;
import javafx.scene.image.*;
import javafx.util.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import net.darmo_creations.imageslibrary.data.*;
import org.jetbrains.annotations.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * Class providing methods to handle files.
 */
public class FileUtils {
  /**
   * Return the extension of the given file.
   *
   * @param file A file.
   * @return The extension, without the dot, or an empty string if the file has no extension.
   */
  public static String getExtension(final @NotNull Path file) {
    final String fileName = file.getFileName().toString();
    if (!fileName.contains("."))
      return "";
    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }

  /**
   * Open the given file path in the host system’s default file explorer.
   *
   * @param path The path to open.
   */
  public static void openInFileExplorer(@NotNull String path) {
    // Cannot use Desktop.getDesktop().open(File) as it does not work properly outside of Windows
    // Possible values: https://runmodule.com/2020/10/12/possible-values-of-os-dependent-java-system-properties/
    final String osName = System.getProperty("os.name").toLowerCase();
    final String[] command;
    if (osName.contains("linux"))
      command = new String[] { "dbus-send", "--dest=org.freedesktop.FileManager1", "--type=method_call",
          "/org/freedesktop/FileManager1", "org.freedesktop.FileManager1.ShowItems",
          "array:string:file:%s".formatted(path), "string:\"\"" };
    else if (osName.contains("win"))
      command = new String[] { "explorer /select,\"{path}\"" };
    else if (osName.contains("mac"))
      command = new String[] { "open", "-R", path };
    else {
      App.logger().error("Unable to open file system explorer: unsupported operating system {}", osName);
      return;
    }

    try {
      Runtime.getRuntime().exec(command);
    } catch (final IOException e) {
      App.logger().error("Unable to open file system explorer", e);
    }
  }

  /**
   * Format the given size in bytes.
   *
   * @param sizeInBytes The size in bytes.
   * @return A pair containing the value expressed in the closest unit,
   * and the unit itself, without the B at the end.
   * The latter may thus be an empty string if size is less than 1024 bytes.
   * @throws IllegalArgumentException If the size is negative.
   */
  public static Pair<String, String> formatBytesSize(long sizeInBytes, @NotNull Language language) {
    if (sizeInBytes < 0)
      throw new IllegalArgumentException("Size cannot be negative");
    for (final var unit : BYTE_UNITS)
      if (sizeInBytes > unit.getKey()) {
        final String formattedNumber = language.formatNumber(sizeInBytes / unit.getKey(), 1);
        return new Pair<>(formattedNumber, unit.getValue());
      }
    return new Pair<>(String.valueOf(sizeInBytes), "");
  }

  @Unmodifiable
  private static final List<Pair<Double, String>> BYTE_UNITS = List.of(
      new Pair<>(1073741824.0, "Gi"), // 1024³
      new Pair<>(1048576.0, "Mi"), // 1024²
      new Pair<>(1024.0, "Ki")
  );

  /**
   * Format the metada of the given image file: width, height,
   * approximate size (in KiB, MiB or GiB), and full size in bytes
   *
   * @param path   The path to the image.
   * @param image  The image.
   * @param config The app’s config.
   * @return The formatted metadata.
   */
  public static String formatImageMetadata(@NotNull Path path, @NotNull Image image, final @NotNull Config config) {
    long size = -1;
    try {
      size = Files.size(path);
    } catch (final IOException | SecurityException e) {
      App.logger().error("Unable to get size of file {}", path, e);
    }

    final Language language = config.language();
    final var formattedSize = size >= 0 ? formatBytesSize(size, language) : new Pair<>("?", "");
    return language.translate(
        "image_preview.file_metadata.label",
        new FormatArg("width", (int) image.getWidth()),
        new FormatArg("height", (int) image.getHeight()),
        new FormatArg("abbr_bytes", formattedSize.getKey()),
        new FormatArg("unit", formattedSize.getValue()),
        new FormatArg("full_bytes", language.formatNumber(size))
    );
  }

  /**
   * Load the given image. If the file’s format is not supported by JavaFX,
   * but is in the {@link App#VALID_IMAGE_EXTENSIONS} list, the image will be converted in-memory
   * into a format that JavaFX supports.
   *
   * @param path            The path to the image file.
   * @param successCallback Callback called when the image is done loading.
   *                        It takes the {@link Image} as its argument.
   * @param errorCallback   Callback called when an I/O error occurs.
   *                        It takes the {@link Exception} object as its argument.
   * @throws IllegalArgumentException If the file’s extension in not in {@link App#VALID_IMAGE_EXTENSIONS}.
   */
  public static void loadImage(
      @NotNull Path path,
      @NotNull Consumer<Image> successCallback,
      @NotNull Consumer<Exception> errorCallback
  ) {
    final String ext = getExtension(path);

    if (ext.isEmpty())
      throw new IllegalArgumentException("Unsupported image format");
    else if (!App.VALID_IMAGE_EXTENSIONS.contains(ext.toLowerCase()))
      throw new IllegalArgumentException("Unsupported image format: " + ext);

    if (JAVAFX_FILE_EXTENSIONS.contains(ext.toLowerCase())) {
      final Image image = new Image("file://" + path, true);
      image.progressProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue.doubleValue() >= 1) {
          if (image.isError())
            errorCallback.accept(image.getException());
          else
            successCallback.accept(image);
        }
      });
    } else {
      new Thread(() -> {
        // Load with ImageIO then convert to Image
        try {
          final BufferedImage image = ImageIO.read(path.toFile());
          final WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
          Platform.runLater(() -> successCallback.accept(fxImage));
        } catch (final IOException e) {
          Platform.runLater(() -> errorCallback.accept(e));
        }
      }).start();
    }
  }

  /**
   * The list of image formats supported by JavaFX.
   * <p>
   * BMP format is excluded as BMP images with an alpha channel are not rendered by {@link ImageView},
   * see <a href="https://bugs.openjdk.org/browse/JDK-8149621">this JDK bug report</a>.
   */
  @Unmodifiable
  private static final List<String> JAVAFX_FILE_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

  /**
   * Delete the parent directory of the given picture if it is empty.
   *
   * @param picture The picture whose parent directory is to be deleted.
   */
  public static void deleteDirectoryIfEmpty(@NotNull Picture picture) {
    final Path sourceDir = picture.path().getParent();
    try (final var stream = Files.newDirectoryStream(sourceDir)) {
      if (!stream.iterator().hasNext())
        Files.delete(sourceDir);
    } catch (final IOException | SecurityException e) {
      App.logger().error("Failed to delete empty source directory {}", sourceDir, e);
    }
  }
}
