package net.darmo_creations.imageslibrary.data;

import org.jetbrains.annotations.*;

import java.util.*;

public class SavedQueriesManager {
  private final DatabaseConnection db;
  private final Map<String, String> queries = new HashMap<>();
  private final List<String> order = new ArrayList<>();
  private final List<QueriesUpdateListener> queriesUpdateListeners = new LinkedList<>();
  private boolean autoCommit = true;
  private boolean pendingChanges = false;

  // Fields used to save the state of this manager.
  private final Map<String, String> queriesSave = new HashMap<>();
  private final List<String> orderSave = new ArrayList<>();

  /**
   * Create a new manager from the current database.
   *
   * @return A new object.
   */
  @Contract("_ -> new")
  public static SavedQueriesManager load(@NotNull DatabaseConnection db) {
    List<SavedQuery> savedQueries;
    try {
      savedQueries = db.getSavedQueries();
    } catch (final DatabaseOperationException e) {
      savedQueries = List.of();
    }
    return new SavedQueriesManager(db, savedQueries);
  }

  private SavedQueriesManager(@NotNull DatabaseConnection db, final @NotNull List<SavedQuery> entries) {
    this.db = db;
    entries.forEach(pair -> {
      final String name = pair.name();
      this.queries.put(name, pair.query());
      this.order.add(name);
    });
  }

  /**
   * Indicate whether the given query name is saved in this manager.
   *
   * @param name The query name to check.
   * @return True if the name is saved, false otherwise.
   */
  public boolean isNameSaved(String name) {
    return this.queries.containsKey(name);
  }

  /**
   * Indicate whether the given query is saved in this manager.
   *
   * @param query The query to check.
   * @return True if the query is saved, false otherwise.
   */
  public boolean isQuerySaved(String query) {
    return this.queries.containsValue(query);
  }

  /**
   * Get the query with the given name.
   *
   * @param name The query’s name.
   * @return The query.
   * @throws NoSuchElementException If no query matches the given name.
   */
  public String getQuery(@NotNull String name) {
    this.ensureNameExists(name);
    return this.queries.get(name);
  }

  /**
   * Save a query under the given name. If a query is already registered with that name, it will be overwritten.
   *
   * @param name  The query’s name.
   * @param query The query.
   * @throws DatabaseOperationException If any database error occurs.
   * @throws NullPointerException       If the name or query is null.
   */
  public void saveQuery(@NotNull String name, @NotNull String query) throws DatabaseOperationException {
    Objects.requireNonNull(name);
    Objects.requireNonNull(query);
    if (this.autoCommit)
      this.saveState();
    if (!this.queries.containsKey(name))
      this.order.add(name);
    this.queries.put(name, query);
    this.onUpdate();
  }

  /**
   * Remove all saved queries.
   *
   * @throws DatabaseOperationException If any database error occurs.
   */
  public void clear() throws DatabaseOperationException {
    if (this.autoCommit)
      this.saveState();
    this.queries.clear();
    this.order.clear();
    this.onUpdate();
  }

  /**
   * Disable the auto-saving of changes and notifying of listeners
   * until either the {@link #commit()} or {@link #rollback()} method is called.
   * <p>
   * This method is usefull when doing several updates in a series to avoid unecessary operations.
   *
   * @throws IllegalStateException If a transaction has already been started.
   */
  public void startTransaction() {
    if (!this.autoCommit)
      throw new IllegalStateException("Already in a transaction");
    this.saveState();
    this.autoCommit = false;
  }

  /**
   * Commit all pending changes since the last {@link #startTransaction()}.
   * Auto-commits are re-enabled after this operation succeeds.
   *
   * @throws DatabaseOperationException If any database error occurs.
   * @throws IllegalStateException      If no transaction has been started.
   */
  public void commit() throws DatabaseOperationException {
    if (this.autoCommit)
      throw new IllegalStateException("Not in a transaction");
    this.autoCommit = true;
    if (this.pendingChanges)
      this.saveDataAndNotifyListeners();
  }

  /**
   * Rollback all changes made since the last {@link #startTransaction()} call.
   * Auto-commits are re-enabled after this operation succeeds.
   *
   * @throws IllegalStateException If no transaction has been started.
   */
  public void rollback() {
    if (this.autoCommit)
      throw new IllegalStateException("Not in a transaction");
    this.restoreState();
    this.autoCommit = true;
  }

  /**
   * Return a sorted list of this manager’s saved queries.
   *
   * @return An unmodifiable list.
   */
  @Unmodifiable
  @Contract(pure = true, value = "-> new")
  public List<SavedQuery> entries() {
    return this.queries.entrySet().stream()
        .sorted(Comparator.comparing(e -> this.order.indexOf(e.getKey())))
        .map(e -> new SavedQuery(e.getKey(), e.getValue()))
        .toList();
  }

  public void addQueriesUpdateListener(@NotNull QueriesUpdateListener listener) {
    this.queriesUpdateListeners.add(Objects.requireNonNull(listener));
  }

  private void saveState() {
    this.queriesSave.clear();
    this.queriesSave.putAll(this.queries);
    this.orderSave.clear();
    this.orderSave.addAll(this.order);
  }

  private void restoreState() {
    this.queries.clear();
    this.queries.putAll(this.queriesSave);
    this.order.clear();
    this.order.addAll(this.orderSave);
  }

  private void ensureNameExists(String name) {
    if (!this.queries.containsKey(name))
      throw new NoSuchElementException("No query with name: " + name);
  }

  private void onUpdate() throws DatabaseOperationException {
    if (this.autoCommit)
      this.saveDataAndNotifyListeners();
    else
      this.pendingChanges = true;
  }

  private void saveDataAndNotifyListeners() throws DatabaseOperationException {
    try {
      this.db.setSavedQueries(this.entries());
    } catch (final DatabaseOperationException e) {
      if (this.autoCommit)
        this.restoreState();
      throw e;
    }
    this.queriesUpdateListeners.forEach(QueriesUpdateListener::onQueriesUpdate);
    this.pendingChanges = false;
  }

  public interface QueriesUpdateListener {
    void onQueriesUpdate();
  }
}
