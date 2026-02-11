package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CallStack}.
 */
class CallStackTest {

  // ==================== Basic Operations Tests ====================

  @Test
  void testEmptyStack() {
    CallStack stack = new CallStack();

    assertTrue(stack.isEmpty());
    assertEquals(0, stack.size());
    assertNull(stack.peek());
    assertNull(stack.pop());
  }

  @Test
  void testPushAndPop() {
    CallStack stack = new CallStack();
    StackFrame frame = StackFrame.of("func", null);

    stack.push(frame);

    assertFalse(stack.isEmpty());
    assertEquals(1, stack.size());
    assertSame(frame, stack.pop());
    assertTrue(stack.isEmpty());
  }

  @Test
  void testPeekDoesNotRemove() {
    CallStack stack = new CallStack();
    StackFrame frame = StackFrame.of("func", null);

    stack.push(frame);

    assertSame(frame, stack.peek());
    assertSame(frame, stack.peek());
    assertEquals(1, stack.size());
  }

  @Test
  void testPushPopMultipleFrames() {
    CallStack stack = new CallStack();
    StackFrame frame1 = StackFrame.of("func1", null);
    StackFrame frame2 = StackFrame.of("func2", null);
    StackFrame frame3 = StackFrame.of("func3", null);

    stack.push(frame1);
    stack.push(frame2);
    stack.push(frame3);

    assertEquals(3, stack.size());
    assertSame(frame3, stack.pop());
    assertSame(frame2, stack.pop());
    assertSame(frame1, stack.pop());
    assertTrue(stack.isEmpty());
  }

  @Test
  void testSize() {
    CallStack stack = new CallStack();

    assertEquals(0, stack.size());

    stack.push(StackFrame.of("func1", null));
    assertEquals(1, stack.size());

    stack.push(StackFrame.of("func2", null));
    assertEquals(2, stack.size());

    stack.pop();
    assertEquals(1, stack.size());
  }

  // ==================== getFrames() Tests ====================

  @Test
  void testGetFramesEmpty() {
    CallStack stack = new CallStack();

    List<StackFrame> frames = stack.getFrames();

    assertTrue(frames.isEmpty());
  }

  @Test
  void testGetFramesReturnsUnmodifiableList() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    List<StackFrame> frames = stack.getFrames();

    assertThrows(UnsupportedOperationException.class, () -> {
      frames.add(StackFrame.of("another", null));
    });
  }

  @Test
  void testGetFramesOrderMostRecentFirst() {
    CallStack stack = new CallStack();
    StackFrame frame1 = StackFrame.of("func1", null);
    StackFrame frame2 = StackFrame.of("func2", null);
    StackFrame frame3 = StackFrame.of("func3", null);

    stack.push(frame1);
    stack.push(frame2);
    stack.push(frame3);

    List<StackFrame> frames = stack.getFrames();

    assertEquals(3, frames.size());
    assertSame(frame3, frames.get(0));
    assertSame(frame2, frames.get(1));
    assertSame(frame1, frames.get(2));
  }

  // ==================== copy() Tests ====================

  @Test
  void testCopyIsIndependent() {
    CallStack original = new CallStack();
    StackFrame frame1 = StackFrame.of("func1", null);
    StackFrame frame2 = StackFrame.of("func2", null);

    original.push(frame1);
    original.push(frame2);

    CallStack copy = original.copy();

    // Verify copy has same content
    assertEquals(2, copy.size());
    assertEquals(original.getFrames(), copy.getFrames());

    // Modify original, verify copy is unaffected
    original.pop();
    assertEquals(1, original.size());
    assertEquals(2, copy.size());
  }

  @Test
  void testCopyEmptyStack() {
    CallStack original = new CallStack();
    CallStack copy = original.copy();

    assertTrue(copy.isEmpty());
  }

  // ==================== formatStackTrace() Tests ====================

  @Test
  void testFormatStackTraceEmpty() {
    CallStack stack = new CallStack();

    assertEquals("", stack.formatStackTrace());
  }

  @Test
  void testFormatStackTraceSingleFrame() {
    CallStack stack = new CallStack();
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    stack.push(StackFrame.of("myFunction", loc));

    String trace = stack.formatStackTrace();

    assertTrue(trace.startsWith("Stack trace:"));
    assertTrue(trace.contains("at myFunction(test.jcp:10:5)"));
  }

  @Test
  void testFormatStackTraceMultipleFrames() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("main", SourceLocation.of("main.jcp", 3, 1)));
    stack.push(StackFrame.of("calculate", SourceLocation.of("main.jcp", 8, 5)));
    stack.push(StackFrame.of("divide", SourceLocation.of("math.jcp", 15, 12)));

    String trace = stack.formatStackTrace();

    assertTrue(trace.contains("Stack trace:"));
    assertTrue(trace.contains("at divide(math.jcp:15:12)"));
    assertTrue(trace.contains("at calculate(main.jcp:8:5)"));
    assertTrue(trace.contains("at main(main.jcp:3:1)"));

    // Verify order: most recent (divide) should appear before others
    int dividePos = trace.indexOf("divide");
    int calculatePos = trace.indexOf("calculate");
    int mainPos = trace.indexOf("at main");

    assertTrue(dividePos < calculatePos);
    assertTrue(calculatePos < mainPos);
  }

  @Test
  void testFormatStackTraceWithoutLocation() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("myFunction", null));

    String trace = stack.formatStackTrace();

    assertTrue(trace.contains("at myFunction"));
    assertFalse(trace.contains("("));
  }

  // ==================== Stack Depth Limit Tests ====================

  @Test
  void testMaxDepthEnforced() {
    CallStack stack = new CallStack();

    // Push up to the limit
    for (int i = 0; i < CallStack.MAX_STACK_DEPTH; i++) {
      stack.push(StackFrame.of("func" + i, null));
    }

    assertEquals(CallStack.MAX_STACK_DEPTH, stack.size());

    // One more should throw
    StackOverflowException ex = assertThrows(StackOverflowException.class, () -> {
      stack.push(StackFrame.of("overflow", null));
    });

    assertTrue(ex.getMessage().contains("Maximum call stack depth exceeded"));
    assertTrue(ex.getMessage().contains(String.valueOf(CallStack.MAX_STACK_DEPTH)));
  }

  @Test
  void testStackOverflowExceptionContainsLastFrame() {
    CallStack stack = new CallStack();

    // Push up to the limit
    for (int i = 0; i < CallStack.MAX_STACK_DEPTH; i++) {
      stack.push(StackFrame.of("func" + i, null));
    }

    StackOverflowException ex = assertThrows(StackOverflowException.class, () -> {
      stack.push(StackFrame.of("lastFunc", SourceLocation.of("test.jcp", 99, 1)));
    });

    assertTrue(ex.getMessage().contains("lastFunc"));
  }

  @Test
  void testPushAfterPopBelowLimit() {
    CallStack stack = new CallStack();

    // Fill to limit
    for (int i = 0; i < CallStack.MAX_STACK_DEPTH; i++) {
      stack.push(StackFrame.of("func" + i, null));
    }

    // Pop one
    stack.pop();

    // Now we can push again
    assertDoesNotThrow(() -> {
      stack.push(StackFrame.of("newFunc", null));
    });
  }
}
