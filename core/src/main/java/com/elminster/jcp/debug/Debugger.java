package com.elminster.jcp.debug;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.exception.StackFrame;

import java.util.List;
import java.util.Map;

/**
 * Main debugger interface for JCP interpreter debugging.
 *
 * <p>Provides programmatic debugging capabilities:
 * <ul>
 *   <li>Breakpoint management (set/remove by location or AST node)</li>
 *   <li>Stepping controls (step over, into, out, continue)</li>
 *   <li>Variable inspection at current scope</li>
 *   <li>Stack trace visualization</li>
 * </ul>
 *
 * <p>Thread-safety: All implementations must be thread-safe.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DefaultDebugger debugger = new DefaultDebugger();
 * EvalContext context = new RootEvalContext();
 * DebuggingEvalVisitor visitor = new DebuggingEvalVisitor(context, debugger);
 *
 * // Set breakpoint at line 10
 * Breakpoint bp = debugger.setBreakpoint(10);
 *
 * // Start debugging
 * new Thread(() -> visitor.debug(program)).start();
 *
 * // Wait for breakpoint hit, then inspect
 * while (!debugger.isPaused()) Thread.sleep(10);
 * Map<String, Data<?>> vars = debugger.getVariables();
 *
 * // Step through code
 * debugger.stepOver();
 * }</pre>
 */
public interface Debugger {

  // ========== Breakpoint Management ==========

  /**
   * Sets a breakpoint at the given line.
   *
   * @param line the line number (1-based)
   * @return the created breakpoint
   */
  Breakpoint setBreakpoint(int line);

  /**
   * Sets a breakpoint at the given line and column.
   *
   * @param line   the line number (1-based)
   * @param column the column number (1-based)
   * @return the created breakpoint
   */
  Breakpoint setBreakpoint(int line, int column);

  /**
   * Sets a breakpoint at the given file, line, and column.
   *
   * @param filepath source file path
   * @param line     the line number (1-based)
   * @param column   the column number (1-based)
   * @return the created breakpoint
   */
  Breakpoint setBreakpoint(String filepath, int line, int column);

  /**
   * Sets a breakpoint at the given AST node.
   *
   * @param node the AST node to break on
   * @return the created breakpoint
   */
  Breakpoint setBreakpoint(Node node);

  /**
   * Removes a breakpoint by its ID.
   *
   * @param breakpointId the breakpoint ID to remove
   */
  void removeBreakpoint(long breakpointId);

  /**
   * Removes a breakpoint.
   *
   * @param breakpoint the breakpoint to remove
   */
  void removeBreakpoint(Breakpoint breakpoint);

  /**
   * Returns all registered breakpoints.
   *
   * @return unmodifiable map of breakpoint IDs to breakpoints
   */
  Map<Long, Breakpoint> getBreakpoints();

  /**
   * Returns a breakpoint by its ID.
   *
   * @param breakpointId the breakpoint ID
   * @return the breakpoint, or null if not found
   */
  Breakpoint getBreakpoint(long breakpointId);

  /**
   * Returns all breakpoints at the given line.
   *
   * @param line the line number
   * @return list of breakpoints at the line (empty if none)
   */
  List<Breakpoint> getBreakpointsAt(int line);

  // ========== Execution Control ==========

  /**
   * Step over: execute current statement, pause at next statement
   * at same or lower call depth.
   *
   * @throws IllegalStateException if not paused
   */
  void stepOver();

  /**
   * Step into: pause at next statement at any call depth.
   * Enters function calls.
   *
   * @throws IllegalStateException if not paused
   */
  void stepInto();

  /**
   * Step out: continue until returning from current function,
   * then pause.
   *
   * @throws IllegalStateException if not paused
   */
  void stepOut();

  /**
   * Continue execution until next breakpoint.
   *
   * @throws IllegalStateException if not paused
   */
  void continueExecution();

  /**
   * Stop debugging entirely.
   * Removes all breakpoints and resumes execution.
   */
  void stop();

  /**
   * Detach debugger but keep breakpoints.
   * Can re-attach later.
   */
  void detach();

  // ========== Inspection ==========

  /**
   * Returns variables in the current scope.
   *
   * @return unmodifiable map of variable names to values
   * @throws IllegalStateException if not paused
   */
  Map<String, Data<?>> getVariables();

  /**
   * Returns the current call stack.
   *
   * @return list of stack frames (most recent first)
   */
  List<StackFrame> getStackTrace();

  // ========== State Queries ==========

  /**
   * Checks if the debugger is currently paused.
   *
   * @return true if paused at breakpoint or step
   */
  boolean isPaused();

  /**
   * Checks if the debugger is attached to execution.
   *
   * @return true if attached
   */
  boolean isAttached();

  /**
   * Returns the current debugger state.
   *
   * @return current state
   */
  DebugState getState();

  /**
   * Returns the AST node where execution is paused.
   *
   * @return current node, or null if not paused
   */
  Node getCurrentNode();

  /**
   * Returns the line number where execution is paused.
   *
   * @return current line number, or -1 if not paused or no location info
   */
  int getCurrentLine();

  // ========== Event Listeners ==========

  /**
   * Adds a debug event listener.
   *
   * @param listener the listener to add
   */
  void addListener(DebugEventListener listener);

  /**
   * Removes a debug event listener.
   *
   * @param listener the listener to remove
   */
  void removeListener(DebugEventListener listener);
}
