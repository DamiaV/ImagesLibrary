package net.darmo_creations.imageslibrary.ui;

import org.jetbrains.annotations.*;

/**
 * Objects implementing this interface report the progress of a process and can demand for it to be cancelled.
 */
public interface ProgressManager {
  /**
   * Notify this object that the current progress has changed.
   *
   * @param messageKey A translation key to the current progress’ message.
   * @param total      The total number of steps.
   * @param progress   The current step.
   */
  void notifyProgress(@NotNull String messageKey, int total, int progress);

  /**
   * Indicates whether the current process should be cancelled.
   *
   * @return True if cancellation is demanded, false otherwise.
   */
  boolean isCancelled();
}
