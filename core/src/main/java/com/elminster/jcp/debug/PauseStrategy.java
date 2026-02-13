package com.elminster.jcp.debug;

/**
 * Strategy interface for debugger pause mechanism.
 *
 * <p>Follows Open-Closed Principle: closed for modification, open for extension.
 * Different implementations can provide blocking, polling, or callback-based pausing.
 *
 * <p>This design enables:
 * <ul>
 *   <li>Easy unit testing with mock implementations</li>
 *   <li>Flexibility in pause mechanism (blocking vs callback)</li>
 *   <li>Separation of concerns</li>
 * </ul>
 */
public interface PauseStrategy {

  /**
   * Waits until the debugger should resume execution.
   *
   * @param debugger the debugger to wait on
   * @throws InterruptedException if the wait is interrupted
   */
  void waitForResume(DefaultDebugger debugger) throws InterruptedException;

  /**
   * Signals that execution should resume.
   */
  void signalResume();
}
