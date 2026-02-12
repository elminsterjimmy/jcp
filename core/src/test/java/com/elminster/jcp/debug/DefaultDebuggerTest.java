package com.elminster.jcp.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultDebugger.
 */
class DefaultDebuggerTest {

  private DefaultDebugger debugger;

  @BeforeEach
  void setUp() {
    debugger = new DefaultDebugger();
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
  }

  @Test
  void detach_KeepsBreakpoints() {
    BreakpointId id = debugger.setBreakpoint(BreakpointLocation.at(10));
    debugger.attach();

    debugger.detach();

    assertEquals(DebugState.DETACHED, debugger.getState());
    assertEquals(1, debugger.getBreakpoints().size());
    assertTrue(debugger.getBreakpoints().containsKey(id));
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

  // ========== Inspection Tests ==========

  @Test
  void getVariables_WhenNotPaused_ThrowsException() {
    debugger.attach();

    assertThrows(IllegalStateException.class, () -> debugger.getVariables());
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

  // ========== shouldPause Stepping Tests ==========

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
