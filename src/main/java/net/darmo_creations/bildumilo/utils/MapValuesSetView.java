package net.darmo_creations.bildumilo.utils;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * This class is a set view of a {@link Map}’s values.
 *
 * @param <T> Type of the map’s values.
 */
public class MapValuesSetView<T> implements Set<T> {
  private final Collection<T> values;

  /**
   * Create a new values view for the given map.
   *
   * @param map The map to create a values-view for.
   * @throws IllegalArgumentException If the map’s values contain duplicates.
   */
  public MapValuesSetView(final @NotNull Map<?, T> map) {
    this.values = map.values();
    if (new HashSet<>(this.values).size() != map.size())
      throw new IllegalArgumentException("Map values are not unique");
  }

  @Override
  public int size() {
    return this.values.size();
  }

  @Override
  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return this.values.contains(o);
  }

  @Override
  public Iterator<T> iterator() {
    return this.values.iterator();
  }

  @Override
  public Object[] toArray() {
    return this.values.toArray();
  }

  @Override
  public <T1> T1[] toArray(@NotNull T1[] a) {
    return this.values.toArray(a);
  }

  @Override
  @Contract("_ -> fail")
  public boolean add(T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Contract("_ -> fail")
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(@NotNull Collection<?> c) {
    return this.values.containsAll(c);
  }

  @Override
  @Contract("_ -> fail")
  public boolean addAll(Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Contract("_ -> fail")
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Contract("_ -> fail")
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Contract("-> fail")
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object obj) {
    return new HashSet<>(this.values).equals(obj);
  }

  @Override
  public int hashCode() {
    return this.values.hashCode();
  }

  @Override
  public String toString() {
    return this.values.toString();
  }
}
