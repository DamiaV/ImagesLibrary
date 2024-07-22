package net.darmo_creations.imageslibrary.data.batch_operations;

/**
 * Instances of classes implementing this interface can be serialized into {@link String}s.
 */
public interface StringSerializable {
  /**
   * Serialize this object into a string representation.
   */
  String serialize();

  /**
   * A key identifying the type of this object.
   * Should be the same for all instances of the same class.
   */
  String key();
}
