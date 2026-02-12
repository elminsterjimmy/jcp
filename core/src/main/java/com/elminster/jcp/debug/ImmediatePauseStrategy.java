package com.elminster.jcp.debug;

/**
 * Immediate (non-blocking) implementation of PauseStrategy for testing.
 *
 * <p>This strategy doesn't actually block - it immediately resumes.
 * Useful for unit testing the debugger without threading complexity.
 */
public class ImmediatePauseStrategy implements PauseStrategy {

  private volatile boolean pauseCalled = false;
  private volatile boolean resumeCalled = false;

  @Override
  public void waitForResume(DefaultDebugger debugger) throws InterruptedException {
    pauseCalled = true;
    // Immediately return - no blocking
  }

  @Override
  public void signalResume() {
    resumeCalled = true;
  }

  /**
   * @return true if waitForResume was called
   */
  public boolean wasPauseCalled() {
    return pauseCalled;
  }

  /**
   * @return true if signalResume was called
   */
  public boolean wasResumeCalled() {
    return resumeCalled;
  }

  /**
   * Resets the state for reuse in tests.
   */
  public void reset() {
    pauseCalled = false;
    resumeCalled = false;
  }
}
