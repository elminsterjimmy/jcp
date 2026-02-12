package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Node;

/**
 * Observer interface for debugger events.
 *
 * <p>Follows Observer pattern to enable UI/CLI integration without polling.
 * Listeners are notified when debugger state changes or breakpoints are hit.
 *
 * <p>Implementations should be thread-safe as events may be fired from
 * different threads.
 */
public interface DebugEventListener {

  /**
   * Called when a breakpoint is hit.
   *
   * @param node     the AST node where execution paused
   * @param location the breakpoint location that was hit
   */
  void onBreakpointHit(Node node, BreakpointLocation location);

  /**
   * Called when a step operation completes.
   *
   * @param node the AST node where execution paused after stepping
   */
  void onStepComplete(Node node);

  /**
   * Called when debugger state changes.
   *
   * @param oldState the previous state
   * @param newState the new state
   */
  void onStateChanged(DebugState oldState, DebugState newState);

  /**
   * Called when an error occurs during debugging.
   *
   * @param error the exception that occurred
   */
  void onError(Exception error);
}
