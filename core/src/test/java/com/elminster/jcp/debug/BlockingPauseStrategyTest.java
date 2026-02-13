package com.elminster.jcp.debug;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BlockingPauseStrategy.
 */
class BlockingPauseStrategyTest {

  @Test
  void signalResume_UnblocksWaitingThread() throws InterruptedException {
    BlockingPauseStrategy strategy = new BlockingPauseStrategy();
    DefaultDebugger debugger = new DefaultDebugger(strategy);
    debugger.attach();

    AtomicBoolean waitCompleted = new AtomicBoolean(false);
    CountDownLatch waitStarted = new CountDownLatch(1);
    CountDownLatch waitFinished = new CountDownLatch(1);

    Thread waitingThread = new Thread(() -> {
      try {
        waitStarted.countDown();
        strategy.waitForResume(debugger);
        waitCompleted.set(true);
        waitFinished.countDown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });

    waitingThread.start();

    // Wait for thread to start waiting
    assertTrue(waitStarted.await(1, TimeUnit.SECONDS));
    Thread.sleep(50); // Give time to enter wait

    // Signal resume
    strategy.signalResume();

    // Wait for completion
    assertTrue(waitFinished.await(1, TimeUnit.SECONDS));
    assertTrue(waitCompleted.get());
  }

  @Test
  void waitForResume_ReturnsImmediately_WhenNotPaused() throws InterruptedException {
    BlockingPauseStrategy strategy = new BlockingPauseStrategy();
    DefaultDebugger debugger = new DefaultDebugger(strategy);
    debugger.attach(); // State is RUNNING, not PAUSED

    long start = System.currentTimeMillis();
    strategy.waitForResume(debugger);
    long elapsed = System.currentTimeMillis() - start;

    // Should return quickly since debugger is not paused
    assertTrue(elapsed < 100);
  }

  @Test
  void waitForResume_ReturnsImmediately_WhenResumedBeforeWait() throws InterruptedException {
    BlockingPauseStrategy strategy = new BlockingPauseStrategy();
    DefaultDebugger debugger = new DefaultDebugger(strategy);

    // Signal resume before wait
    strategy.signalResume();

    long start = System.currentTimeMillis();
    strategy.waitForResume(debugger);
    long elapsed = System.currentTimeMillis() - start;

    // Should return quickly
    assertTrue(elapsed < 100);
  }
}
