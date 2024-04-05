package net.darmo_creations.imageslibrary.io;

import java.util.*;

/**
 * This class indicates that the file at the target location has to be overwritten by the file being moved.
 *
 * @param overwrittenTagsHandling If the overwritten file was in the database,
 *                                the way in which the tags of the target file should be handled.
 */
public record Overwrite(OverwrittenTagsHandling overwrittenTagsHandling)
    implements FileNameConflictResolution {
  public Overwrite {
    Objects.requireNonNull(overwrittenTagsHandling);
  }
}
