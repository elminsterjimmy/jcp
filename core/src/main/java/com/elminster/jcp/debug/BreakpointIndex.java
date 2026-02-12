package com.elminster.jcp.debug;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial index for O(1) breakpoint lookup by line number.
 *
 * <p>Thread-safe implementation using ConcurrentHashMap.
 * Enables fast breakpoint checking during interpretation.
 *
 * <p>Performance target: O(1) lookup instead of O(n) iteration.
 */
public class BreakpointIndex {

  private final Map<Integer, Set<BreakpointEntry>> byLine = new ConcurrentHashMap<>();

  /**
   * Entry associating a breakpoint ID with its location.
   */
  public static class BreakpointEntry {
    private final BreakpointId id;
    private final BreakpointLocation location;

    public BreakpointEntry(BreakpointId id, BreakpointLocation location) {
      this.id = id;
      this.location = location;
    }

    public BreakpointId getId() {
      return id;
    }

    public BreakpointLocation getLocation() {
      return location;
    }
  }

  /**
   * Adds a breakpoint to the index.
   *
   * @param id       the breakpoint ID
   * @param location the breakpoint location
   */
  public void add(BreakpointId id, BreakpointLocation location) {
    if (!location.hasSourceLocation()) {
      return;
    }
    byLine.computeIfAbsent(location.getLine(), k -> ConcurrentHashMap.newKeySet())
        .add(new BreakpointEntry(id, location));
  }

  /**
   * Removes a breakpoint from the index.
   *
   * @param id       the breakpoint ID
   * @param location the breakpoint location
   */
  public void remove(BreakpointId id, BreakpointLocation location) {
    if (!location.hasSourceLocation()) {
      return;
    }
    Set<BreakpointEntry> entries = byLine.get(location.getLine());
    if (entries != null) {
      entries.removeIf(entry -> entry.getId().equals(id));
      if (entries.isEmpty()) {
        byLine.remove(location.getLine());
      }
    }
  }

  /**
   * Gets all breakpoints at the given line.
   *
   * @param line the line number
   * @return set of breakpoint entries at the line (empty if none)
   */
  public Set<BreakpointEntry> getAt(int line) {
    return byLine.getOrDefault(line, Collections.emptySet());
  }

  /**
   * Checks if there are any breakpoints at the given line.
   *
   * @param line the line number
   * @return true if breakpoints exist at the line
   */
  public boolean hasBreakpointsAt(int line) {
    Set<BreakpointEntry> entries = byLine.get(line);
    return entries != null && !entries.isEmpty();
  }

  /**
   * Checks if the index is empty.
   *
   * @return true if no breakpoints are indexed
   */
  public boolean isEmpty() {
    return byLine.isEmpty();
  }

  /**
   * Clears all breakpoints from the index.
   */
  public void clear() {
    byLine.clear();
  }
}
