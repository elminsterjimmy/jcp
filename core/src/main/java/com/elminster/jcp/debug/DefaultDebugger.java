package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.exception.JcpException;
import com.elminster.jcp.exception.StackFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of the Debugger interface.
 *
 * <p>Thread-safe implementation using dual storage:
 * <ul>
 *   <li>Primary: Map of IDs to locations for safe removal</li>
 *   <li>Index: BreakpointIndex for O(1) line-based lookup</li>
 * </ul>
 *
 * <p>Uses double-checked locking for fast-path when no breakpoints.
 */
public class DefaultDebugger implements Debugger {

  /**
   * Default pause timeout in milliseconds to prevent deadlock.
   */
  private static final long PAUSE_TIMEOUT_MS = 30_000;

  // Breakpoint storage
  private final Map<BreakpointId, BreakpointLocation> breakpoints = new ConcurrentHashMap<>();
  private final BreakpointIndex breakpointIndex = new BreakpointIndex();

  // State management
  private final Object stateLock = new Object();
  private volatile DebugState state = DebugState.DETACHED;
  private volatile boolean hasBreakpoints = false;

  // Pause management
  private final Object pauseLock = new Object();
  private volatile Node currentNode;
  private volatile EvalContext currentContext;
  private volatile int targetDepth;

  // Listeners
  private final List<DebugEventListener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public BreakpointId setBreakpoint(BreakpointLocation location) {
    BreakpointId id = BreakpointId.next();
    breakpoints.put(id, location);
    breakpointIndex.add(id, location);
    hasBreakpoints = true;
    return id;
  }

  @Override
  public BreakpointId setBreakpoint(Node node) {
    return setBreakpoint(BreakpointLocation.at(node));
  }

  @Override
  public void removeBreakpoint(BreakpointId id) {
    BreakpointLocation location = breakpoints.remove(id);
    if (location != null) {
      breakpointIndex.remove(id, location);
    }
    hasBreakpoints = !breakpoints.isEmpty();
  }

  @Override
  public Map<BreakpointId, BreakpointLocation> getBreakpoints() {
    return Collections.unmodifiableMap(new HashMap<>(breakpoints));
  }

  @Override
  public void stepOver() {
    requirePaused();
    resume(DebugState.STEP_OVER);
  }

  @Override
  public void stepInto() {
    requirePaused();
    resume(DebugState.STEP_INTO);
  }

  @Override
  public void stepOut() {
    requirePaused();
    resume(DebugState.STEP_OUT);
  }

  @Override
  public void continueExecution() {
    requirePaused();
    resume(DebugState.RUNNING);
  }

  @Override
  public void stop() {
    synchronized (pauseLock) {
      breakpoints.clear();
      breakpointIndex.clear();
      hasBreakpoints = false;
      setState(DebugState.DETACHED);
      currentNode = null;
      currentContext = null;
      pauseLock.notifyAll();
    }
  }

  @Override
  public void detach() {
    synchronized (pauseLock) {
      setState(DebugState.DETACHED);
      currentNode = null;
      currentContext = null;
      pauseLock.notifyAll();
    }
  }

  @Override
  public Map<String, Data<?>> getVariables() {
    requirePaused();
    if (currentContext == null) {
      return Collections.emptyMap();
    }
    synchronized (pauseLock) {
      Map<String, Data> vars = currentContext.getVariables();
      Map<String, Data<?>> result = new HashMap<>();
      for (Map.Entry<String, Data> entry : vars.entrySet()) {
        result.put(entry.getKey(), entry.getValue());
      }
      return Collections.unmodifiableMap(result);
    }
  }

  @Override
  public List<StackFrame> getStackTrace() {
    if (currentContext == null) {
      return Collections.emptyList();
    }
    return currentContext.getCallStack().getFrames();
  }

  @Override
  public boolean isPaused() {
    return state == DebugState.PAUSED;
  }

  @Override
  public boolean isAttached() {
    return state != DebugState.DETACHED;
  }

  @Override
  public DebugState getState() {
    return state;
  }

  @Override
  public Node getCurrentNode() {
    return currentNode;
  }

  @Override
  public BreakpointLocation getCurrentLocation() {
    Node node = currentNode;
    if (node == null) {
      return null;
    }
    return BreakpointLocation.at(node);
  }

  @Override
  public void addListener(DebugEventListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(DebugEventListener listener) {
    listeners.remove(listener);
  }

  // ========== Internal Methods for DebuggingEvalVisitor ==========

  /**
   * Attaches the debugger to start debugging.
   * Called by DebuggingEvalVisitor at start of execution.
   */
  public void attach() {
    setState(DebugState.RUNNING);
  }

  /**
   * Checks if the debugger should pause at the given node.
   * Fast-path optimization: returns false immediately if no breakpoints.
   *
   * @param node      the current AST node
   * @param callDepth current call depth
   * @return true if should pause
   */
  public boolean shouldPause(Node node, int callDepth) {
    // Fast-path: no synchronization when inactive
    if (!isAttached()) {
      return false;
    }

    // Check stepping state
    DebugState currentState = state;
    switch (currentState) {
      case STEP_INTO:
        return true;
      case STEP_OVER:
        if (callDepth <= targetDepth) {
          return true;
        }
        break;
      case STEP_OUT:
        if (callDepth < targetDepth) {
          return true;
        }
        break;
      case RUNNING:
        // Check breakpoints
        break;
      default:
        return false;
    }

    // Fast-path: no breakpoints
    if (!hasBreakpoints) {
      return false;
    }

    // Check breakpoints at node's line
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        int line = locatable.getLocation().getStartLine();
        for (BreakpointIndex.BreakpointEntry entry : breakpointIndex.getAt(line)) {
          if (entry.getLocation().matches(node)) {
            return true;
          }
        }
      }
    }

    // Check node-based breakpoints (non-indexed)
    for (BreakpointLocation location : breakpoints.values()) {
      if (!location.hasSourceLocation() && location.matches(node)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Pauses execution at the given node.
   * Blocks until resumed via stepping commands or detach.
   *
   * @param node       the current AST node
   * @param context    the current evaluation context
   * @param callDepth  current call depth
   * @param isStepPause true if pausing due to step (not breakpoint)
   */
  public void pause(Node node, EvalContext context, int callDepth, boolean isStepPause) {
    synchronized (pauseLock) {
      currentNode = node;
      currentContext = context;
      targetDepth = callDepth;
      setState(DebugState.PAUSED);

      // Notify listeners
      if (isStepPause) {
        notifyStepComplete(node);
      } else {
        BreakpointLocation hitLocation = findMatchingBreakpoint(node);
        notifyBreakpointHit(node, hitLocation);
      }

      // Wait for resume
      long deadline = System.currentTimeMillis() + PAUSE_TIMEOUT_MS;
      while (state == DebugState.PAUSED) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
          throw new JcpException("Debugger pause timeout - possible deadlock");
        }
        try {
          pauseLock.wait(remaining);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new JcpException("Debugging interrupted", null, e);
        }
      }
    }
  }

  // ========== Private Helpers ==========

  private void setState(DebugState newState) {
    synchronized (stateLock) {
      DebugState oldState = this.state;
      if (oldState != newState) {
        if (!oldState.canTransitionTo(newState)) {
          throw new IllegalStateException(
              String.format("Cannot transition from %s to %s", oldState, newState));
        }
        this.state = newState;
        notifyStateChanged(oldState, newState);
      }
    }
  }

  private void resume(DebugState newState) {
    synchronized (pauseLock) {
      setState(newState);
      pauseLock.notifyAll();
    }
  }

  private void requirePaused() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Debugger is not paused");
    }
  }

  private BreakpointLocation findMatchingBreakpoint(Node node) {
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        int line = locatable.getLocation().getStartLine();
        for (BreakpointIndex.BreakpointEntry entry : breakpointIndex.getAt(line)) {
          if (entry.getLocation().matches(node)) {
            return entry.getLocation();
          }
        }
      }
    }
    for (BreakpointLocation location : breakpoints.values()) {
      if (location.matches(node)) {
        return location;
      }
    }
    return null;
  }

  private void notifyBreakpointHit(Node node, BreakpointLocation location) {
    for (DebugEventListener listener : listeners) {
      try {
        listener.onBreakpointHit(node, location);
      } catch (Exception e) {
        // Log but don't propagate listener errors
      }
    }
  }

  private void notifyStepComplete(Node node) {
    for (DebugEventListener listener : listeners) {
      try {
        listener.onStepComplete(node);
      } catch (Exception e) {
        // Log but don't propagate listener errors
      }
    }
  }

  private void notifyStateChanged(DebugState oldState, DebugState newState) {
    for (DebugEventListener listener : listeners) {
      try {
        listener.onStateChanged(oldState, newState);
      } catch (Exception e) {
        // Log but don't propagate listener errors
      }
    }
  }
}
