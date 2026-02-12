package com.elminster.jcp.debug;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unique identifier for breakpoints.
 *
 * <p>Uses atomic counter for thread-safe ID generation.
 * IDs are unique within a single JVM session.
 *
 * <p>Following GDB/LLDB conventions, breakpoints are identified by
 * numeric IDs for safe removal operations.
 */
public final class BreakpointId {

  private static final AtomicLong counter = new AtomicLong(0);

  private final long id;

  private BreakpointId(long id) {
    this.id = id;
  }

  /**
   * Creates a new unique breakpoint ID.
   *
   * @return new BreakpointId with unique value
   */
  public static BreakpointId next() {
    return new BreakpointId(counter.incrementAndGet());
  }

  /**
   * Returns the numeric ID value.
   *
   * @return the ID value
   */
  public long getValue() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BreakpointId)) {
      return false;
    }
    BreakpointId that = (BreakpointId) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Breakpoint#" + id;
  }
}
