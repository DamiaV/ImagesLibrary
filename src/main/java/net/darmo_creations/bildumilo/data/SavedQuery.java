package net.darmo_creations.bildumilo.data;

import org.jetbrains.annotations.*;

import java.util.*;

public record SavedQuery(@NotNull String name, @NotNull String query) {
  public SavedQuery {
    Objects.requireNonNull(name);
    Objects.requireNonNull(query);
  }
}
