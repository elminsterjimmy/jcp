package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;

/**
 * Represents a single frame in the call stack.
 * Immutable for thread safety and exception attachment.
 *
 * <p>Function names are interned for memory efficiency in recursive calls.
 */
public final class StackFrame {
  private static final int MAX_NAME_LENGTH_FOR_INTERNING = 64;
  private static final String UNKNOWN = "<unknown>";

  private final String functionName;
  private final SourceLocation location;

  private StackFrame(String functionName, SourceLocation location) {
    if (functionName == null) {
      this.functionName = UNKNOWN;
    } else if (functionName.length() < MAX_NAME_LENGTH_FOR_INTERNING) {
      this.functionName = functionName.intern();
    } else {
      this.functionName = functionName;
    }
    this.location = location;
  }

  /**
   * Creates a stack frame with function name and location.
   * Follows SourceLocation.of() pattern.
   *
   * @param functionName the function name (null becomes "&lt;unknown&gt;")
   * @param location     the source location (may be null)
   * @return new StackFrame instance
   */
  public static StackFrame of(String functionName, SourceLocation location) {
    return new StackFrame(functionName, location);
  }

  /**
   * Returns the function name.
   *
   * @return function name, never null
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Returns the source location.
   *
   * @return source location, or null if not available
   */
  public SourceLocation getLocation() {
    return location;
  }

  /**
   * Format: "functionName(file:line:col)" or "functionName" if no location.
   */
  @Override
  public String toString() {
    if (location == null) {
      return functionName;
    }
    return String.format("%s(%s)", functionName, location);
  }
}
