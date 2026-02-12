package com.elminster.jcp.debug;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImmediatePauseStrategy.
 */
class ImmediatePauseStrategyTest {

  private ImmediatePauseStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new ImmediatePauseStrategy();
  }

  @Test
  void waitForResume_DoesNotBlock() throws InterruptedException {
    DefaultDebugger debugger = new DefaultDebugger(strategy);

    long start = System.currentTimeMillis();
    strategy.waitForResume(debugger);
    long elapsed = System.currentTimeMillis() - start;

    // Should return immediately (less than 100ms)
    assertTrue(elapsed < 100);
    assertTrue(strategy.wasPauseCalled());
  }

  @Test
  void signalResume_SetsFlag() {
    assertFalse(strategy.wasResumeCalled());

    strategy.signalResume();

    assertTrue(strategy.wasResumeCalled());
  }

  @Test
  void reset_ClearsAllFlags() throws InterruptedException {
    DefaultDebugger debugger = new DefaultDebugger(strategy);
    strategy.waitForResume(debugger);
    strategy.signalResume();

    assertTrue(strategy.wasPauseCalled());
    assertTrue(strategy.wasResumeCalled());

    strategy.reset();

    assertFalse(strategy.wasPauseCalled());
    assertFalse(strategy.wasResumeCalled());
  }

  @Test
  void initialState_AllFlagsAreFalse() {
    assertFalse(strategy.wasPauseCalled());
    assertFalse(strategy.wasResumeCalled());
  }
}
