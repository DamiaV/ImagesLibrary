package net.darmo_creations.imageslibrary.io;

/**
 * Base interface for all file name conflict resolution classes.
 */
public sealed interface FileNameConflictResolution
    permits Skip, Overwrite, Rename {
}
