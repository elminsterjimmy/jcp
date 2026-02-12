package com.elminster.jcp.debug;

import com.elminster.jcp.exception.JcpException;

/**
 * Blocking implementation of PauseStrategy using wait/notify.
 *
 * <p>This is the default strategy for real debugging sessions.
 * For testing, use a mock or immediate strategy instead.
 */
public class BlockingPauseStrategy implements PauseStrategy {

  private static final long DEFAULT_TIMEOUT_MS = 30_000;

  private final Object lock = new Object();
  private final long timeoutMs;
  private volatile boolean resumed = false;

  public BlockingPauseStrategy() {
    this(DEFAULT_TIMEOUT_MS);
  }

  public BlockingPauseStrategy(long timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  @Override
  public void waitForResume(DefaultDebugger debugger) throws InterruptedException {
    synchronized (lock) {
      resumed = false;
      long deadline = System.currentTimeMillis() + timeoutMs;

      while (!resumed && debugger.isPaused()) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
          throw new JcpException("Debugger pause timeout - possible deadlock");
        }
        lock.wait(remaining);
      }
    }
  }

  @Override
  public void signalResume() {
    synchronized (lock) {
      resumed = true;
      lock.notifyAll();
    }
  }
}
