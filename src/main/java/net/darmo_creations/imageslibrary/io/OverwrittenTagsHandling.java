package net.darmo_creations.imageslibrary.io;

/**
 * This enum lists all the ways tags of an overwritten files should be handled.
 */
public enum OverwrittenTagsHandling {
  /**
   * Keep the tags of the overwritten file, ignoring those of the moved file.
   */
  KEEP_ORIGINAL_TAGS,
  /**
   * Merge the tags of both files.
   */
  MERGE_TAGS,
  /**
   * Replace the tags of the target file by those of the move file.
   */
  REPLACE_ORIGINAL_TAGS,
}
