package com.elminster.jcp.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultDebugger.
 */
class DefaultDebuggerTest {

  private ImmediatePauseStrategy pauseStrategy;
  private DefaultDebugger debugger;

  @BeforeEach
  void setUp() {
    pauseStrategy = new ImmediatePauseStrategy();
    debugger = new DefaultDebugger(pauseStrategy);
  }

  // ========== Constructor Tests ==========

  @Test
  void defaultConstructor_UsesBlockingStrategy() {
    DefaultDebugger defaultDebugger = new DefaultDebugger();
    // Just verify it creates without error
    assertNotNull(defaultDebugger);
  }

  // ========== Breakpoint Management Tests ==========

  @Test
  void setBreakpoint_ReturnsUniqueId() {
    BreakpointId id1 = debugger.setBreakpoint(BreakpointLocation.at(10));
    BreakpointId id2 = debugger.setBreakpoint(BreakpointLocation.at(20));

    assertNotEquals(id1, id2);
  }

  @Test
  void setBreakpoint_ByNode_ReturnsId() {
    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    BreakpointId id = debugger.setBreakpoint(node);

    assertNotNull(id);
  }

  @Test
  void getBreakpoints_ReturnsAllBreakpoints() {
    BreakpointId id1 = debugger.setBreakpoint(BreakpointLocation.at(10));
    BreakpointId id2 = debugger.setBreakpoint(BreakpointLocation.at(20));

    Map<BreakpointId, BreakpointLocation> breakpoints = debugger.getBreakpoints();

    assertEquals(2, breakpoints.size());
    assertTrue(breakpoints.containsKey(id1));
    assertTrue(breakpoints.containsKey(id2));
  }

  @Test
  void removeBreakpoint_RemovesById() {
    BreakpointId id1 = debugger.setBreakpoint(BreakpointLocation.at(10));
    BreakpointId id2 = debugger.setBreakpoint(BreakpointLocation.at(20));

    debugger.removeBreakpoint(id1);

    Map<BreakpointId, BreakpointLocation> breakpoints = debugger.getBreakpoints();
    assertEquals(1, breakpoints.size());
    assertFalse(breakpoints.containsKey(id1));
    assertTrue(breakpoints.containsKey(id2));
  }

  @Test
  void removeBreakpoint_NonExistent_NoError() {
    BreakpointId id = BreakpointId.next();

    // Should not throw
    debugger.removeBreakpoint(id);
  }

  // ========== State Tests ==========

  @Test
  void initialState_IsDetached() {
    assertEquals(DebugState.DETACHED, debugger.getState());
    assertFalse(debugger.isAttached());
    assertFalse(debugger.isPaused());
  }

  @Test
  void attach_SetsRunningState() {
    debugger.attach();

    assertEquals(DebugState.RUNNING, debugger.getState());
    assertTrue(debugger.isAttached());
    assertFalse(debugger.isPaused());
  }

  @Test
  void stop_ClearsBreakpointsAndDetaches() {
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.setBreakpoint(BreakpointLocation.at(20));
    debugger.attach();

    debugger.stop();

    assertEquals(DebugState.DETACHED, debugger.getState());
    assertTrue(debugger.getBreakpoints().isEmpty());
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  @Test
  void detach_KeepsBreakpoints() {
    BreakpointId id = debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    debugger.detach();

    assertEquals(DebugState.DETACHED, debugger.getState());
    assertEquals(1, debugger.getBreakpoints().size());
    assertTrue(debugger.getBreakpoints().containsKey(id));
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  // ========== shouldPause Tests ==========

  @Test
  void shouldPause_WhenNotAttached_ReturnsFalse() {
    debugger.setBreakpoint(BreakpointLocation.at(10));

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertFalse(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_WhenAttachedWithBreakpoint_ReturnsTrue() {
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertTrue(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_WhenNoBreakpointAtLine_ReturnsFalse() {
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 20, 5));

    assertFalse(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_NodeWithoutLocation_ReturnsFalse() {
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    // No location set

    assertFalse(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_NoBreakpointsSet_ReturnsFalse() {
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertFalse(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_NodeBasedBreakpoint_Matches() {
    IntLiteral node = IntLiteral.of(42);
    debugger.setBreakpoint(node);
    debugger.attach();

    assertTrue(debugger.shouldPause(node, 0));
  }

  // ========== Pause and Resume Tests ==========

  @Test
  void pause_SetsPausedStateAndNotifiesBreakpointHit() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));
    EvalContext context = new RootEvalContext();

    debugger.pause(node, context, 0, false);

    assertTrue(pauseStrategy.wasPauseCalled());
    assertEquals(1, listener.breakpointHitCount);
    assertSame(node, debugger.getCurrentNode());
  }

  @Test
  void pause_WhenStepPause_NotifiesStepComplete() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    EvalContext context = new RootEvalContext();

    debugger.pause(node, context, 0, true);

    assertEquals(0, listener.breakpointHitCount);
    assertEquals(1, listener.stepCompleteCount);
  }

  @Test
  void getCurrentLocation_WhenPaused_ReturnsLocation() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));
    EvalContext context = new RootEvalContext();

    debugger.pause(node, context, 0, true);

    BreakpointLocation location = debugger.getCurrentLocation();
    assertNotNull(location);
  }

  // ========== Stepping Control Tests ==========

  @Test
  void stepOver_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.stepOver());
  }

  @Test
  void stepInto_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.stepInto());
  }

  @Test
  void stepOut_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.stepOut());
  }

  @Test
  void continueExecution_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.continueExecution());
  }

  @Test
  void stepOver_WhenPaused_SetsStepOverStateAndResumes() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    pauseStrategy.reset();
    debugger.stepOver();

    assertEquals(DebugState.STEP_OVER, debugger.getState());
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  @Test
  void stepInto_WhenPaused_SetsStepIntoStateAndResumes() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    pauseStrategy.reset();
    debugger.stepInto();

    assertEquals(DebugState.STEP_INTO, debugger.getState());
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  @Test
  void stepOut_WhenPaused_SetsStepOutStateAndResumes() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    pauseStrategy.reset();
    debugger.stepOut();

    assertEquals(DebugState.STEP_OUT, debugger.getState());
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  @Test
  void continueExecution_WhenPaused_SetsRunningStateAndResumes() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    pauseStrategy.reset();
    debugger.continueExecution();

    assertEquals(DebugState.RUNNING, debugger.getState());
    assertTrue(pauseStrategy.wasResumeCalled());
  }

  // ========== shouldPause Stepping State Tests ==========

  @Test
  void shouldPause_InStepIntoState_ReturnsTrue() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);
    debugger.stepInto();

    IntLiteral nextNode = IntLiteral.of(99);
    assertTrue(debugger.shouldPause(nextNode, 0));
  }

  @Test
  void shouldPause_InStepOverState_AtSameDepth_ReturnsTrue() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 1, true);
    debugger.stepOver();

    IntLiteral nextNode = IntLiteral.of(99);
    assertTrue(debugger.shouldPause(nextNode, 1));
  }

  @Test
  void shouldPause_InStepOverState_AtDeeperDepth_ReturnsFalse() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 1, true);
    debugger.stepOver();

    IntLiteral nextNode = IntLiteral.of(99);
    assertFalse(debugger.shouldPause(nextNode, 2));
  }

  @Test
  void shouldPause_InStepOutState_AtShallowerDepth_ReturnsTrue() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 2, true);
    debugger.stepOut();

    IntLiteral nextNode = IntLiteral.of(99);
    assertTrue(debugger.shouldPause(nextNode, 1));
  }

  @Test
  void shouldPause_InStepOutState_AtSameDepth_ReturnsFalse() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 2, true);
    debugger.stepOut();

    IntLiteral nextNode = IntLiteral.of(99);
    assertFalse(debugger.shouldPause(nextNode, 2));
  }

  // ========== Inspection Tests ==========

  @Test
  void getVariables_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.getVariables());
  }

  @Test
  void getVariables_WhenPaused_ReturnsVariables() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    EvalContext context = new RootEvalContext();

    debugger.pause(node, context, 0, true);

    Map<String, ?> vars = debugger.getVariables();
    assertNotNull(vars);
  }

  @Test
  void getCurrentNode_WhenNotPaused_ReturnsNull() {
    debugger.attach();

    assertNull(debugger.getCurrentNode());
  }

  @Test
  void getCurrentLocation_WhenNotPaused_ReturnsNull() {
    debugger.attach();

    assertNull(debugger.getCurrentLocation());
  }

  // ========== Stack Trace Tests ==========

  @Test
  void getStackTrace_WhenNoContext_ReturnsEmptyList() {
    debugger.attach();

    assertTrue(debugger.getStackTrace().isEmpty());
  }

  // ========== Listener Tests ==========

  @Test
  void addListener_ReceivesStateChangeEvents() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);

    debugger.attach();

    assertEquals(1, listener.stateChangedCount);
    assertEquals(DebugState.DETACHED, listener.lastOldState);
    assertEquals(DebugState.RUNNING, listener.lastNewState);
  }

  @Test
  void removeListener_StopsReceivingEvents() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.removeListener(listener);

    debugger.attach();

    assertEquals(0, listener.stateChangedCount);
  }

  @Test
  void listener_ThatThrowsException_DoesNotStopOtherListeners() {
    TestDebugEventListener listener1 = new TestDebugEventListener() {
      @Override
      public void onStateChanged(DebugState oldState, DebugState newState) {
        throw new RuntimeException("Test exception");
      }
    };
    TestDebugEventListener listener2 = new TestDebugEventListener();

    debugger.addListener(listener1);
    debugger.addListener(listener2);

    debugger.attach();

    // listener2 should still receive the event
    assertEquals(1, listener2.stateChangedCount);
  }

  @Test
  void listener_OnBreakpointHit_ReceivesEvent() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));
    debugger.pause(node, new RootEvalContext(), 0, false);

    assertEquals(1, listener.breakpointHitCount);
  }

  @Test
  void listener_OnStepComplete_ReceivesEvent() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    assertEquals(1, listener.stepCompleteCount);
  }

  @Test
  void listener_ThrowsOnBreakpoint_DoesNotStopOthers() {
    TestDebugEventListener listener1 = new TestDebugEventListener() {
      @Override
      public void onBreakpointHit(com.elminster.jcp.ast.Node node, BreakpointLocation location) {
        throw new RuntimeException("Test exception");
      }
    };
    TestDebugEventListener listener2 = new TestDebugEventListener();

    debugger.addListener(listener1);
    debugger.addListener(listener2);
    debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));
    debugger.pause(node, new RootEvalContext(), 0, false);

    assertEquals(1, listener2.breakpointHitCount);
  }

  @Test
  void listener_ThrowsOnStepComplete_DoesNotStopOthers() {
    TestDebugEventListener listener1 = new TestDebugEventListener() {
      @Override
      public void onStepComplete(com.elminster.jcp.ast.Node node) {
        throw new RuntimeException("Test exception");
      }
    };
    TestDebugEventListener listener2 = new TestDebugEventListener();

    debugger.addListener(listener1);
    debugger.addListener(listener2);
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    assertEquals(1, listener2.stepCompleteCount);
  }

  // ========== Additional Coverage Tests ==========

  @Test
  void getVariables_WhenPausedWithNullContext_ReturnsEmptyMap() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);

    // Pause with null context
    debugger.pause(node, null, 0, true);

    Map<String, ?> vars = debugger.getVariables();
    assertNotNull(vars);
    assertTrue(vars.isEmpty());
  }

  @Test
  void getStackTrace_WhenPausedWithContext_ReturnsFrames() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    EvalContext context = new RootEvalContext();

    debugger.pause(node, context, 0, true);

    // Should not throw and return frames list
    java.util.List<?> frames = debugger.getStackTrace();
    assertNotNull(frames);
  }

  @Test
  void shouldPause_InPausedState_ReturnsFalse() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 0, true);

    // In PAUSED state, shouldPause returns false (default case)
    IntLiteral nextNode = IntLiteral.of(99);
    assertFalse(debugger.shouldPause(nextNode, 0));
  }

  @Test
  void findMatchingBreakpoint_WithNodeWithoutLocation_SearchesAllBreakpoints() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);

    // Set a node-based breakpoint (no source location)
    IntLiteral nodeForBreakpoint = IntLiteral.of(42);
    debugger.setBreakpoint(nodeForBreakpoint);
    debugger.attach();

    // Pause at this node - should find it through non-indexed search
    debugger.pause(nodeForBreakpoint, new RootEvalContext(), 0, false);

    assertEquals(1, listener.breakpointHitCount);
  }

  @Test
  void shouldPause_WithNodeBreakpoint_MatchesSameNode() {
    IntLiteral node = IntLiteral.of(42);
    // No location - this will be a node-only breakpoint
    debugger.setBreakpoint(node);
    debugger.attach();

    // shouldPause should find the node via non-indexed search
    assertTrue(debugger.shouldPause(node, 0));
  }

  @Test
  void shouldPause_WithDifferentNode_ReturnsFalse() {
    IntLiteral node1 = IntLiteral.of(42);
    IntLiteral node2 = IntLiteral.of(99);
    debugger.setBreakpoint(node1);
    debugger.attach();

    // Different node should not match
    assertFalse(debugger.shouldPause(node2, 0));
  }

  @Test
  void shouldPause_InStepOverState_AtShallowerDepth_ReturnsTrue() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 2, true);
    debugger.stepOver();

    IntLiteral nextNode = IntLiteral.of(99);
    // At shallower depth (1 < 2), should still pause
    assertTrue(debugger.shouldPause(nextNode, 1));
  }

  @Test
  void shouldPause_InStepOutState_AtDeeperDepth_ReturnsFalse() {
    debugger.attach();
    IntLiteral node = IntLiteral.of(42);
    debugger.pause(node, new RootEvalContext(), 1, true);
    debugger.stepOut();

    IntLiteral nextNode = IntLiteral.of(99);
    // At deeper depth (2 > 1), should not pause
    assertFalse(debugger.shouldPause(nextNode, 2));
  }

  @Test
  void findMatchingBreakpoint_WithLocationBreakpoint_ReturnsLocation() {
    TestDebugEventListener listener = new TestDebugEventListener();
    debugger.addListener(listener);
    debugger.setBreakpoint(BreakpointLocation.at(10, 5));
    debugger.attach();

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    debugger.pause(node, new RootEvalContext(), 0, false);

    assertEquals(1, listener.breakpointHitCount);
  }

  // ========== Test Helper Classes ==========

  private static class TestDebugEventListener implements DebugEventListener {
    int breakpointHitCount = 0;
    int stepCompleteCount = 0;
    int stateChangedCount = 0;
    int errorCount = 0;
    DebugState lastOldState;
    DebugState lastNewState;

    @Override
    public void onBreakpointHit(com.elminster.jcp.ast.Node node, BreakpointLocation location) {
      breakpointHitCount++;
    }

    @Override
    public void onStepComplete(com.elminster.jcp.ast.Node node) {
      stepCompleteCount++;
    }

    @Override
    public void onStateChanged(DebugState oldState, DebugState newState) {
      stateChangedCount++;
      lastOldState = oldState;
      lastNewState = newState;
    }

    @Override
    public void onError(Exception error) {
      errorCount++;
    }
  }
}
