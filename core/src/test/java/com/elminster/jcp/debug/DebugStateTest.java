package com.elminster.jcp.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DebugState state machine transitions.
 */
class DebugStateTest {

  // ========== DETACHED State Transitions ==========

  @Test
  void detached_CanTransitionToRunning() {
    assertTrue(DebugState.DETACHED.canTransitionTo(DebugState.RUNNING));
  }

  @Test
  void detached_CanTransitionToDetached() {
    assertTrue(DebugState.DETACHED.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void detached_CannotTransitionToPaused() {
    assertFalse(DebugState.DETACHED.canTransitionTo(DebugState.PAUSED));
  }

  @Test
  void detached_CannotTransitionToStepOver() {
    assertFalse(DebugState.DETACHED.canTransitionTo(DebugState.STEP_OVER));
  }

  @Test
  void detached_CannotTransitionToStepInto() {
    assertFalse(DebugState.DETACHED.canTransitionTo(DebugState.STEP_INTO));
  }

  @Test
  void detached_CannotTransitionToStepOut() {
    assertFalse(DebugState.DETACHED.canTransitionTo(DebugState.STEP_OUT));
  }

  // ========== RUNNING State Transitions ==========

  @Test
  void running_CanTransitionToPaused() {
    assertTrue(DebugState.RUNNING.canTransitionTo(DebugState.PAUSED));
  }

  @Test
  void running_CanTransitionToDetached() {
    assertTrue(DebugState.RUNNING.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void running_CannotTransitionToRunning() {
    assertFalse(DebugState.RUNNING.canTransitionTo(DebugState.RUNNING));
  }

  @Test
  void running_CannotTransitionToStepOver() {
    assertFalse(DebugState.RUNNING.canTransitionTo(DebugState.STEP_OVER));
  }

  // ========== PAUSED State Transitions ==========

  @Test
  void paused_CanTransitionToRunning() {
    assertTrue(DebugState.PAUSED.canTransitionTo(DebugState.RUNNING));
  }

  @Test
  void paused_CanTransitionToStepOver() {
    assertTrue(DebugState.PAUSED.canTransitionTo(DebugState.STEP_OVER));
  }

  @Test
  void paused_CanTransitionToStepInto() {
    assertTrue(DebugState.PAUSED.canTransitionTo(DebugState.STEP_INTO));
  }

  @Test
  void paused_CanTransitionToStepOut() {
    assertTrue(DebugState.PAUSED.canTransitionTo(DebugState.STEP_OUT));
  }

  @Test
  void paused_CanTransitionToDetached() {
    assertTrue(DebugState.PAUSED.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void paused_CannotTransitionToPaused() {
    assertFalse(DebugState.PAUSED.canTransitionTo(DebugState.PAUSED));
  }

  // ========== STEP_OVER State Transitions ==========

  @Test
  void stepOver_CanTransitionToPaused() {
    assertTrue(DebugState.STEP_OVER.canTransitionTo(DebugState.PAUSED));
  }

  @Test
  void stepOver_CanTransitionToDetached() {
    assertTrue(DebugState.STEP_OVER.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void stepOver_CannotTransitionToRunning() {
    assertFalse(DebugState.STEP_OVER.canTransitionTo(DebugState.RUNNING));
  }

  // ========== STEP_INTO State Transitions ==========

  @Test
  void stepInto_CanTransitionToPaused() {
    assertTrue(DebugState.STEP_INTO.canTransitionTo(DebugState.PAUSED));
  }

  @Test
  void stepInto_CanTransitionToDetached() {
    assertTrue(DebugState.STEP_INTO.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void stepInto_CannotTransitionToRunning() {
    assertFalse(DebugState.STEP_INTO.canTransitionTo(DebugState.RUNNING));
  }

  // ========== STEP_OUT State Transitions ==========

  @Test
  void stepOut_CanTransitionToPaused() {
    assertTrue(DebugState.STEP_OUT.canTransitionTo(DebugState.PAUSED));
  }

  @Test
  void stepOut_CanTransitionToDetached() {
    assertTrue(DebugState.STEP_OUT.canTransitionTo(DebugState.DETACHED));
  }

  @Test
  void stepOut_CannotTransitionToRunning() {
    assertFalse(DebugState.STEP_OUT.canTransitionTo(DebugState.RUNNING));
  }
}
