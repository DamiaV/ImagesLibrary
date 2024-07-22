package net.darmo_creations.imageslibrary.data;

import net.darmo_creations.imageslibrary.data.batch_operations.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class manages the operation batches saved by users.
 */
public class BatchOperationsManager {
  private final DatabaseConnection db;
  private final Map<String, List<? extends Operation>> operationBatches = new HashMap<>();

  /**
   * Create a new manager from the current database.
   *
   * @return A new object.
   */
  @Contract("_ -> new")
  public static BatchOperationsManager load(@NotNull DatabaseConnection db) {
    Map<String, List<Operation>> savedBatches;
    try {
      savedBatches = db.getSavedBatchOperations();
    } catch (final DatabaseOperationException e) {
      savedBatches = Map.of();
    }
    return new BatchOperationsManager(db, savedBatches);
  }

  private BatchOperationsManager(
      @NotNull DatabaseConnection db,
      @NotNull Map<String, List<Operation>> operationBatches
  ) {
    this.db = db;
    this.operationBatches.putAll(operationBatches);
  }

  /**
   * Return an unmodifiable view of this manager’s saved operation batches.
   *
   * @return An unmodifiable map view.
   */
  @UnmodifiableView
  @Contract(pure = true, value = "-> new")
  public Map<String, List<? extends Operation>> entries() {
    return Collections.unmodifiableMap(this.operationBatches);
  }

  /**
   * Save the given list of operations under the given name.
   * <p>
   * If a batch with the same name already exists, it will be overwritten.
   *
   * @param name       The batch’s name.
   * @param operations The list of operations to save.
   * @throws DatabaseOperationException If any database error occurs.
   */
  public void saveOperationBatch(@NotNull String name, @NotNull List<? extends Operation> operations)
      throws DatabaseOperationException {
    this.operationBatches.put(Objects.requireNonNull(name), Objects.requireNonNull(operations));
    this.save();
  }

  /**
   * Delete the batch with the given name.
   *
   * @param name Name of the batch to delete.
   * @throws DatabaseOperationException If any database error occurs.
   * @throws NoSuchElementException     If no batch with the given name exist in this manager.
   */
  public void deleteOperationBatch(@NotNull String name) throws DatabaseOperationException {
    if (!this.operationBatches.containsKey(name))
      throw new NoSuchElementException("No operation batch with name " + name);
    this.operationBatches.remove(name);
    this.save();
  }

  /**
   * Rename the batch with the given name.
   *
   * @param oldName Current name of the batch to rename.
   * @param newName The batch’s new name.
   * @throws DatabaseOperationException If any database error occurs.
   * @throws NoSuchElementException     If no batch with the given name exist in this manager.
   */
  public void renameOperationBatch(@NotNull String oldName, @NotNull String newName) throws DatabaseOperationException {
    final List<? extends Operation> operations = this.operationBatches.remove(oldName);
    if (operations == null)
      throw new NoSuchElementException("No operation batch with name " + oldName);
    this.operationBatches.put(newName, operations);
    this.save();
  }

  private void save() throws DatabaseOperationException {
    this.db.setSavedBatchOperations(this.operationBatches);
  }
}
