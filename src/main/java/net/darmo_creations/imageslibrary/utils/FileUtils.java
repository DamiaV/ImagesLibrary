package net.darmo_creations.imageslibrary.utils;

import javafx.util.*;
import net.darmo_creations.imageslibrary.*;
import net.darmo_creations.imageslibrary.config.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
}
