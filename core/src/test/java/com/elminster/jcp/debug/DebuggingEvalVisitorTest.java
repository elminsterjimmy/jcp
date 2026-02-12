package com.elminster.jcp.debug;

import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DebuggingEvalVisitor.
 */
class DebuggingEvalVisitorTest {

  private DefaultDebugger debugger;
  private EvalContext context;
  private DebuggingEvalVisitor visitor;

  @BeforeEach
  void setUp() {
    debugger = new DefaultDebugger();
    context = new RootEvalContext();
    visitor = new DebuggingEvalVisitor(context, debugger);
  }

  @Test
  void constructor_SetsDebugger() {
    assertEquals(debugger, visitor.getDebugger());
  }

  @Test
  void constructor_SetsContext() {
    assertEquals(context, visitor.getContext());
  }

  @Test
  void getCallDepth_Initially_ReturnsZero() {
    assertEquals(0, visitor.getCallDepth());
  }

  @Test
  void getDebugger_ReturnsProvidedDebugger() {
    DefaultDebugger newDebugger = new DefaultDebugger();
    DebuggingEvalVisitor newVisitor = new DebuggingEvalVisitor(context, newDebugger);

    assertSame(newDebugger, newVisitor.getDebugger());
  }
}
