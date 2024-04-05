package net.darmo_creations.imageslibrary.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This class provides file management operations such as moving, renaming and deleting.
 */
public final class FilesManager {
  /**
   * Move a file into the specified directory.
   *
   * @param sourceFile The file to move.
   * @param destDir    The directory into which to move the file.
   * @param resolver   In case of name conflicts, a function that returns a resolution for the two conflicting files.
   * @return The outcome of the operation.
   * @throws FileOperationError If the path is not a file or the destination directory does not exist,
   *                            or if any file system error occurs.
   */
  public FileMovingOutcome moveFile(
      Path sourceFile,
      Path destDir,
      FileNameConflictResolutionProvider resolver
  ) throws FileOperationError {
    if (!Files.exists(sourceFile))
      throw new FileOperationError(FileOperationErrorCode.MISSING_FILE_ERROR);
    if (!Files.isRegularFile(sourceFile))
      throw new FileOperationError(FileOperationErrorCode.IS_NOT_FILE_ERROR);
    if (!Files.exists(destDir))
      throw new FileOperationError(FileOperationErrorCode.MISSING_FILE_ERROR);
    if (!Files.isDirectory(destDir))
      throw new FileOperationError(FileOperationErrorCode.IS_NOT_DIRECTORY_ERROR);

    final Path targetFile = destDir.resolve(sourceFile.getFileName());
    Path destFile = targetFile;

    FileNameConflictResolution resolution = null;
    final List<CopyOption> copyOptions = new LinkedList<>();

    if (Files.exists(targetFile)) {
      boolean resolved = false;
      do {
        resolution = resolver.forFiles(sourceFile, targetFile, FileOperationErrorCode.FILE_ALREADY_EXISTS_ERROR);

        if (resolution instanceof Skip) {
          return new FileMovingOutcome(sourceFile, sourceFile, resolution);

        } else if (resolution instanceof Overwrite) {
          copyOptions.add(StandardCopyOption.REPLACE_EXISTING);
          resolved = true;

        } else if (resolution instanceof Rename rename) {
          final Path newPath = destDir.resolve(rename.newName());
          if (!Files.exists(newPath)) {
            destFile = newPath;
            resolved = true;
          }

        } else {
          // Should never happen
          throw new RuntimeException("Invalid resolution type: " + resolution.getClass().getName());
        }
      } while (!resolved); // Keep asking for a resolution until we have one
    }

    try {
      Files.move(sourceFile, destFile, copyOptions.toArray(CopyOption[]::new));
    } catch (FileAlreadyExistsException e) {
      throw new FileOperationError(FileOperationErrorCode.FILE_ALREADY_EXISTS_ERROR, e);
    } catch (SecurityException e) {
      throw new FileOperationError(FileOperationErrorCode.MISSING_PERMISSIONS_ERROR, e);
    } catch (IOException e) {
      throw new FileOperationError(FileOperationErrorCode.UNKNOWN_ERROR, e);
    }

    return new FileMovingOutcome(sourceFile, destFile, resolution);
  }

  /**
   * Rename the given file.
   *
   * @param file    The file to rename.
   * @param newName The file’s new name.
   * @return The new path.
   * @throws FileOperationError If the path is not a file or a file with the same name
   *                            already exists in the same directory,
   *                            or if any file system error occurs.
   */
  public Path renameFile(Path file, String newName) throws FileOperationError {
    if (!Files.exists(file))
      throw new FileOperationError(FileOperationErrorCode.MISSING_FILE_ERROR);
    if (!Files.isRegularFile(file))
      throw new FileOperationError(FileOperationErrorCode.IS_NOT_FILE_ERROR);

    try {
      return Files.move(file, file.getParent().resolve(newName));
    } catch (FileAlreadyExistsException e) {
      throw new FileOperationError(FileOperationErrorCode.FILE_ALREADY_EXISTS_ERROR, e);
    } catch (SecurityException e) {
      throw new FileOperationError(FileOperationErrorCode.MISSING_PERMISSIONS_ERROR, e);
    } catch (IOException e) {
      throw new FileOperationError(FileOperationErrorCode.UNKNOWN_ERROR, e);
    }
  }

  /**
   * Delete the given file.
   *
   * @param file The file to delete.
   * @throws FileOperationError If the path is not a file or if any file system error occurs.
   */
  public void deleteFile(Path file) throws FileOperationError {
    if (!Files.isRegularFile(file))
      throw new FileOperationError(FileOperationErrorCode.IS_NOT_FILE_ERROR);
    try {
      Files.delete(file);
    } catch (NoSuchFileException e) {
      throw new FileOperationError(FileOperationErrorCode.MISSING_FILE_ERROR, e);
    } catch (SecurityException e) {
      throw new FileOperationError(FileOperationErrorCode.MISSING_PERMISSIONS_ERROR, e);
    } catch (IOException e) {
      throw new FileOperationError(FileOperationErrorCode.UNKNOWN_ERROR, e);
    }
  }

  /**
   * A function the returns how the name collision
   * for the two given source and target files should be resolved.
   */
  @FunctionalInterface
  public interface FileNameConflictResolutionProvider {
    /**
     * Get the conflict resolution for the given files.
     *
     * @param source The file being moved.
     * @param target The conflicting file at the target location.
     * @param reason The reason for the conflict.
     * @return A {@link FileNameConflictResolution} indicating how the conflict should be resolved.
     */
    FileNameConflictResolution forFiles(Path source, Path target, FileOperationErrorCode reason);
  }
}
