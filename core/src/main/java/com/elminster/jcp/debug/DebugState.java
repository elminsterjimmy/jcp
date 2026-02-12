package com.elminster.jcp.debug;

/**
 * State machine for debugger execution control.
 *
 * <p>Defines valid state transitions to prevent invalid debugger states.
 * Each state represents the debugger's current execution mode.
 */
public enum DebugState {

  /**
   * Not attached to any execution.
   */
  DETACHED,

  /**
   * Attached and running until next breakpoint.
   */
  RUNNING,

  /**
   * Pause at next statement at same or lower call depth.
   */
  STEP_OVER,

  /**
   * Pause at next statement at any call depth.
   */
  STEP_INTO,

  /**
   * Pause after returning from current function.
   */
  STEP_OUT,

  /**
   * Currently paused at breakpoint or step.
   */
  PAUSED;

  /**
   * Checks if transitioning to the given state is valid.
   *
   * <p>Valid transitions:
   * <ul>
   *   <li>DETACHED -> RUNNING, DETACHED</li>
   *   <li>RUNNING -> PAUSED, DETACHED</li>
   *   <li>PAUSED -> RUNNING, STEP_OVER, STEP_INTO, STEP_OUT, DETACHED</li>
   *   <li>STEP_* -> PAUSED, DETACHED</li>
   * </ul>
   *
   * @param next the target state
   * @return true if transition is valid
   */
  public boolean canTransitionTo(DebugState next) {
    switch (this) {
      case DETACHED:
        return next == RUNNING || next == DETACHED;
      case RUNNING:
        return next == PAUSED || next == DETACHED;
      case PAUSED:
        return next == RUNNING || next == STEP_OVER || next == STEP_INTO
            || next == STEP_OUT || next == DETACHED;
      case STEP_OVER:
      case STEP_INTO:
      case STEP_OUT:
        return next == PAUSED || next == DETACHED;
      default:
        return false;
    }
  }
}
