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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of the Debugger interface.
 *
 * <p>Thread-safe implementation using dual-direction mapping:
 * <ul>
 *   <li>ID to Breakpoint: O(1) lookup by ID for removal</li>
 *   <li>Line to Breakpoints: O(1) lookup by line for matching</li>
 * </ul>
 *
 * <p>Uses Strategy Pattern for pause mechanism to enable testability.
 */
public class DefaultDebugger implements Debugger {

  // Dual-direction breakpoint storage
  private final Map<Long, Breakpoint> breakpointsById = new ConcurrentHashMap<>();
  private final Map<Integer, Set<Breakpoint>> breakpointsByLine = new ConcurrentHashMap<>();

  // State management
  private final Object stateLock = new Object();
  private volatile DebugState state = DebugState.DETACHED;
  private volatile boolean hasBreakpoints = false;

  // Pause management - Strategy Pattern for testability
  private final PauseStrategy pauseStrategy;
  private volatile Node currentNode;
  private volatile EvalContext currentContext;
  private volatile int targetDepth;

  // Listeners
  private final List<DebugEventListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Creates a debugger with blocking pause strategy (default for production).
   */
  public DefaultDebugger() {
    this(new BlockingPauseStrategy());
  }

  /**
   * Creates a debugger with custom pause strategy (for testing).
   *
   * @param pauseStrategy the strategy to use for pausing
   */
  public DefaultDebugger(PauseStrategy pauseStrategy) {
    this.pauseStrategy = pauseStrategy;
  }

  // ========== Breakpoint Management ==========

  @Override
  public Breakpoint setBreakpoint(int line) {
    Breakpoint bp = Breakpoint.at(line);
    addBreakpoint(bp);
    return bp;
  }

  @Override
  public Breakpoint setBreakpoint(int line, int column) {
    Breakpoint bp = Breakpoint.at(line, column);
    addBreakpoint(bp);
    return bp;
  }

  @Override
  public Breakpoint setBreakpoint(String filepath, int line, int column) {
    Breakpoint bp = Breakpoint.at(filepath, line, column);
    addBreakpoint(bp);
    return bp;
  }

  @Override
  public Breakpoint setBreakpoint(Node node) {
    Breakpoint bp = Breakpoint.at(node);
    addBreakpoint(bp);
    return bp;
  }

  private void addBreakpoint(Breakpoint bp) {
    breakpointsById.put(bp.getId(), bp);
    if (bp.hasSourceLocation()) {
      breakpointsByLine
          .computeIfAbsent(bp.getLine(), k -> ConcurrentHashMap.newKeySet())
          .add(bp);
    }
    hasBreakpoints = true;
  }

  @Override
  public void removeBreakpoint(long breakpointId) {
    Breakpoint bp = breakpointsById.remove(breakpointId);
    if (bp != null) {
      removeFromLineIndex(bp);
    }
    hasBreakpoints = !breakpointsById.isEmpty();
  }

  @Override
  public void removeBreakpoint(Breakpoint breakpoint) {
    removeBreakpoint(breakpoint.getId());
  }

  private void removeFromLineIndex(Breakpoint bp) {
    if (bp.hasSourceLocation()) {
      Set<Breakpoint> lineBreakpoints = breakpointsByLine.get(bp.getLine());
      if (lineBreakpoints != null) {
        lineBreakpoints.remove(bp);
        if (lineBreakpoints.isEmpty()) {
          breakpointsByLine.remove(bp.getLine());
        }
      }
    }
  }

  @Override
  public Map<Long, Breakpoint> getBreakpoints() {
    return Collections.unmodifiableMap(new HashMap<>(breakpointsById));
  }

  @Override
  public Breakpoint getBreakpoint(long breakpointId) {
    return breakpointsById.get(breakpointId);
  }

  @Override
  public List<Breakpoint> getBreakpointsAt(int line) {
    Set<Breakpoint> lineBreakpoints = breakpointsByLine.get(line);
    if (lineBreakpoints == null || lineBreakpoints.isEmpty()) {
      return Collections.emptyList();
    }
    return new ArrayList<>(lineBreakpoints);
  }

  // ========== Execution Control ==========

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
    breakpointsById.clear();
    breakpointsByLine.clear();
    hasBreakpoints = false;
    setState(DebugState.DETACHED);
    currentNode = null;
    currentContext = null;
    pauseStrategy.signalResume();
  }

  @Override
  public void detach() {
    setState(DebugState.DETACHED);
    currentNode = null;
    currentContext = null;
    pauseStrategy.signalResume();
  }

  // ========== Inspection ==========

  @Override
  public Map<String, Data<?>> getVariables() {
    requirePaused();
    if (currentContext == null) {
      return Collections.emptyMap();
    }
    Map<String, Data> vars = currentContext.getVariables();
    Map<String, Data<?>> result = new HashMap<>();
    for (Map.Entry<String, Data> entry : vars.entrySet()) {
      result.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(result);
  }

  @Override
  public List<StackFrame> getStackTrace() {
    if (currentContext == null) {
      return Collections.emptyList();
    }
    return currentContext.getCallStack().getFrames();
  }

  // ========== State Queries ==========

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
  public int getCurrentLine() {
    Node node = currentNode;
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        return locatable.getLocation().getStartLine();
      }
    }
    return -1;
  }

  // ========== Event Listeners ==========

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
        // Use <= instead of == to handle returning from nested function calls.
        // When stepping over a function call, we want to pause at the next statement
        // at the SAME depth (==), but also when we've returned from deeper calls
        // and are now at a LOWER depth (<). Example: if we step over at depth 2
        // and the function returns to depth 1, we should still pause there.
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
        Set<Breakpoint> lineBreakpoints = breakpointsByLine.get(line);
        if (lineBreakpoints != null) {
          for (Breakpoint bp : lineBreakpoints) {
            if (bp.matches(node)) {
              return true;
            }
          }
        }
      }
    }

    // Check node-based breakpoints (non-indexed)
    for (Breakpoint bp : breakpointsById.values()) {
      if (!bp.hasSourceLocation() && bp.matches(node)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Pauses execution at the given node.
   * Blocks until resumed via stepping commands or detach.
   *
   * @param node        the current AST node
   * @param context     the current evaluation context
   * @param callDepth   current call depth
   * @param isStepPause true if pausing due to step (not breakpoint)
   */
  public void pause(Node node, EvalContext context, int callDepth, boolean isStepPause) {
    currentNode = node;
    currentContext = context;
    targetDepth = callDepth;
    setState(DebugState.PAUSED);

    // Notify listeners
    if (isStepPause) {
      notifyStepComplete(node);
    } else {
      Breakpoint hitBreakpoint = findMatchingBreakpoint(node);
      notifyBreakpointHit(node, hitBreakpoint);
    }

    // Wait for resume using the strategy
    try {
      pauseStrategy.waitForResume(this);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new JcpException("Debugging interrupted", null, e);
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
    setState(newState);
    pauseStrategy.signalResume();
  }

  private void requirePaused() {
    if (state != DebugState.PAUSED) {
      throw new IllegalStateException("Debugger is not paused");
    }
  }

  private Breakpoint findMatchingBreakpoint(Node node) {
    if (node instanceof Locatable) {
      Locatable locatable = (Locatable) node;
      if (locatable.getLocation() != null) {
        int line = locatable.getLocation().getStartLine();
        Set<Breakpoint> lineBreakpoints = breakpointsByLine.get(line);
        if (lineBreakpoints != null) {
          for (Breakpoint bp : lineBreakpoints) {
            if (bp.matches(node)) {
              return bp;
            }
          }
        }
      }
    }
    for (Breakpoint bp : breakpointsById.values()) {
      if (bp.matches(node)) {
        return bp;
      }
    }
    return null;
  }

  private void notifyBreakpointHit(Node node, Breakpoint breakpoint) {
    for (DebugEventListener listener : listeners) {
      try {
        listener.onBreakpointHit(node, breakpoint);
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
