package net.darmo_creations.imageslibrary.utils;

/**
 * This class provides methods related to reflection.
 */
public final class ReflectionUtils {
  /**
   * Return the name of the method that called the method that called this one.
   */
  public static String getCallingMethodName() {
    return Thread.currentThread().getStackTrace()[3].getMethodName();
  }

  private ReflectionUtils() {
  }
}
