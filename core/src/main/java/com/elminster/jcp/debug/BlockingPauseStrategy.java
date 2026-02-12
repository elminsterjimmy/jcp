package com.elminster.jcp.debug;

/**
 * Blocking implementation of PauseStrategy using wait/notify.
 *
 * <p>This is the default strategy for real debugging sessions.
 * Waits indefinitely until resumed - the user is responsible for
 * managing timeouts or handling stuck debuggers.
 *
 * <p>For testing, use a mock or immediate strategy instead.
 */
public class BlockingPauseStrategy implements PauseStrategy {

  private final Object lock = new Object();
  private volatile boolean resumed = false;

  @Override
  public void waitForResume(DefaultDebugger debugger) throws InterruptedException {
    synchronized (lock) {
      resumed = false;
      while (!resumed && debugger.isPaused()) {
        lock.wait();
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
