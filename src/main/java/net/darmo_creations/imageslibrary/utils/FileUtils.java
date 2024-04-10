package net.darmo_creations.imageslibrary.utils;

import java.nio.file.*;

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
  public static String getExtension(final Path file) {
    final String fileName = file.getFileName().toString();
    if (!fileName.contains("."))
      return "";
    return fileName.substring(fileName.lastIndexOf('.') + 1);
  }
}
