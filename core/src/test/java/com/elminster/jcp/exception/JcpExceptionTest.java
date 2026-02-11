package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JcpException}.
 */
class JcpExceptionTest {

  // ==================== Construction Tests ====================

  @Test
  void testConstructionWithoutLocation() {
    JcpException ex = new JcpException("error message");

    assertEquals("error message", ex.getMessage());
    assertNull(ex.getLocation());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructionWithLocation() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    JcpException ex = new JcpException("error message", loc);

    assertEquals("error message at test.jcp:10:5", ex.getMessage());
    assertEquals(loc, ex.getLocation());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructionWithLocationAndCause() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    Throwable cause = new RuntimeException("root cause");
    JcpException ex = new JcpException("error message", loc, cause);

    assertEquals("error message at test.jcp:10:5", ex.getMessage());
    assertEquals(loc, ex.getLocation());
    assertSame(cause, ex.getCause());
  }

  @Test
  void testConstructionWithCauseOnly() {
    Throwable cause = new RuntimeException("root cause");
    JcpException ex = new JcpException(cause);

    assertNull(ex.getLocation());
    assertSame(cause, ex.getCause());
  }

  @Test
  void testConstructionWithNullLocation() {
    JcpException ex = new JcpException("error message", null);

    assertEquals("error message", ex.getMessage());
    assertNull(ex.getLocation());
  }

  // ==================== withLocation() Tests ====================

  @Test
  void testWithLocationCreatesNewInstance() {
    JcpException original = new JcpException("error");
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    JcpException withLoc = original.withLocation(loc);

    assertNotSame(original, withLoc);
    assertNull(original.getLocation());
    assertEquals(loc, withLoc.getLocation());
  }

  @Test
  void testWithLocationPreservesMessage() {
    JcpException original = new JcpException("error message");
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    JcpException withLoc = original.withLocation(loc);

    assertEquals("error message at test.jcp:10:5", withLoc.getMessage());
  }

  @Test
  void testWithLocationPreservesCause() {
    Throwable cause = new RuntimeException("root cause");
    JcpException original = new JcpException("error", null, cause);
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    JcpException withLoc = original.withLocation(loc);

    assertSame(cause, withLoc.getCause());
  }

  @Test
  void testWithLocationPreservesStackTrace() {
    JcpException original = new JcpException("error");
    StackTraceElement[] originalTrace = original.getStackTrace();
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    JcpException withLoc = original.withLocation(loc);

    assertArrayEquals(originalTrace, withLoc.getStackTrace());
  }

  @Test
  void testWithLocationDoesNotOverrideExisting() {
    SourceLocation loc1 = SourceLocation.of("first.jcp", 1, 1);
    SourceLocation loc2 = SourceLocation.of("second.jcp", 2, 2);
    JcpException original = new JcpException("error", loc1);

    JcpException withLoc = original.withLocation(loc2);

    assertSame(original, withLoc);
    assertEquals(loc1, withLoc.getLocation());
  }

  @Test
  void testWithLocationNullLocationReturnsNewInstance() {
    JcpException original = new JcpException("error");

    JcpException withNull = original.withLocation(null);

    assertNotSame(original, withNull);
    assertNull(withNull.getLocation());
  }

  // ==================== getMessage() Formatting Tests ====================

  @Test
  void testGetMessageWithoutLocation() {
    JcpException ex = new JcpException("Division by zero");

    assertEquals("Division by zero", ex.getMessage());
  }

  @Test
  void testGetMessageWithLocation() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 12);
    JcpException ex = new JcpException("Division by zero", loc);

    assertEquals("Division by zero at math.jcp:15:12", ex.getMessage());
  }

  @Test
  void testGetMessageWithLocationNoFilepath() {
    SourceLocation loc = SourceLocation.of(null, 15, 12);
    JcpException ex = new JcpException("Division by zero", loc);

    assertEquals("Division by zero at 15:12", ex.getMessage());
  }

  // ==================== getFormattedMessage() Tests ====================

  @Test
  void testGetFormattedMessageWithoutLocation() {
    JcpException ex = new JcpException("Division by zero");

    assertEquals("Division by zero", ex.getFormattedMessage());
  }

  @Test
  void testGetFormattedMessageWithLocation() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 8, "  return a / b;");
    JcpException ex = new JcpException("Division by zero", loc);

    String formatted = ex.getFormattedMessage();

    assertTrue(formatted.contains("Division by zero at math.jcp:15:8"));
    assertTrue(formatted.contains("math.jcp:15:8"));
    assertTrue(formatted.contains("15 |   return a / b;"));
    assertTrue(formatted.contains("^"));
  }

  @Test
  void testGetFormattedMessageWithRangeLocation() {
    SourceLocation loc = SourceLocation.span("math.jcp", 15, 8, 15, 14, "  return a / b;");
    JcpException ex = new JcpException("Division by zero", loc);

    String formatted = ex.getFormattedMessage();

    assertTrue(formatted.contains("^"));
    assertTrue(formatted.contains("~"));
  }

  @Test
  void testGetFormattedMessageWithoutSourceContent() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 8);
    JcpException ex = new JcpException("Division by zero", loc);

    String formatted = ex.getFormattedMessage();

    assertTrue(formatted.contains("Division by zero at math.jcp:15:8"));
    assertTrue(formatted.contains("math.jcp:15:8"));
    assertFalse(formatted.contains(" | "));
  }

  // ==================== Edge Cases ====================

  @Test
  void testNullMessage() {
    JcpException ex = new JcpException((String) null);

    assertNull(ex.getMessage());
    assertEquals(null, ex.getFormattedMessage());
  }

  @Test
  void testNullMessageWithLocation() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    JcpException ex = new JcpException(null, loc);

    assertEquals("null at test.jcp:10:5", ex.getMessage());
  }

  @Test
  void testEmptyMessage() {
    JcpException ex = new JcpException("");

    assertEquals("", ex.getMessage());
  }

  @Test
  void testVeryLongSourceLine() {
    String longLine = "x".repeat(1000);
    SourceLocation loc = SourceLocation.of("test.jcp", 1, 500, longLine);
    JcpException ex = new JcpException("error", loc);

    String formatted = ex.getFormattedMessage();

    assertTrue(formatted.contains(longLine));
  }

  @Test
  void testExceptionInheritance() {
    JcpException ex = new JcpException("error");

    assertTrue(ex instanceof RuntimeException);
  }

  // ==================== CallStack Tests ====================

  @Test
  void testConstructionWithCallStack() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", loc));

    JcpException ex = new JcpException("error", loc, stack);

    assertNotNull(ex.getCallStack());
    assertEquals(1, ex.getCallStack().size());
  }

  @Test
  void testConstructionWithNullCallStack() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    JcpException ex = new JcpException("error", loc, (CallStack) null);

    assertNull(ex.getCallStack());
  }

  @Test
  void testConstructionWithCallStackAndCause() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", loc));
    Throwable cause = new RuntimeException("root cause");

    JcpException ex = new JcpException("error", loc, stack, cause);

    assertNotNull(ex.getCallStack());
    assertSame(cause, ex.getCause());
  }

  @Test
  void testCallStackIsCopiedOnConstruction() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func1", null));

    JcpException ex = new JcpException("error", null, stack);

    // Modify original stack
    stack.push(StackFrame.of("func2", null));

    // Exception's copy should be unaffected
    assertEquals(1, ex.getCallStack().size());
  }

  @Test
  void testWithCallStackMutatesInPlace() {
    JcpException original = new JcpException("error");
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException withStack = original.withCallStack(stack);

    // withCallStack mutates in place to preserve exception type
    assertSame(original, withStack);
    assertNotNull(original.getCallStack());
    assertNotNull(withStack.getCallStack());
  }

  @Test
  void testWithCallStackPreservesJavaStackTrace() {
    JcpException original = new JcpException("error");
    StackTraceElement[] originalTrace = original.getStackTrace();
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException withStack = original.withCallStack(stack);

    // Since it mutates in place, the stack trace should be unchanged
    assertArrayEquals(originalTrace, withStack.getStackTrace());
    assertSame(original, withStack);
  }

  @Test
  void testWithCallStackPreservesCause() {
    Throwable cause = new RuntimeException("root cause");
    JcpException original = new JcpException("error", null, cause);
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException withStack = original.withCallStack(stack);

    assertSame(cause, withStack.getCause());
  }

  @Test
  void testWithCallStackPreservesLocation() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    JcpException original = new JcpException("error", loc);
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException withStack = original.withCallStack(stack);

    assertEquals(loc, withStack.getLocation());
  }

  @Test
  void testWithCallStackDoesNotOverrideExisting() {
    CallStack stack1 = new CallStack();
    stack1.push(StackFrame.of("func1", null));
    CallStack stack2 = new CallStack();
    stack2.push(StackFrame.of("func2", null));

    JcpException original = new JcpException("error", null, stack1);

    JcpException withStack = original.withCallStack(stack2);

    assertSame(original, withStack);
    assertEquals("func1", withStack.getCallStack().peek().getFunctionName());
  }

  // ==================== getFullMessage() Tests ====================

  @Test
  void testGetFullMessageWithCallStack() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 8, "  return a / b;");
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("main", SourceLocation.of("main.jcp", 3, 1)));
    stack.push(StackFrame.of("divide", loc));

    JcpException ex = new JcpException("Division by zero", loc, stack);

    String fullMessage = ex.getFullMessage();

    assertTrue(fullMessage.contains("Division by zero"));
    assertTrue(fullMessage.contains("math.jcp:15:8"));
    assertTrue(fullMessage.contains("Stack trace:"));
    assertTrue(fullMessage.contains("at divide(math.jcp:15:8)"));
    assertTrue(fullMessage.contains("at main(main.jcp:3:1)"));
  }

  @Test
  void testGetFullMessageWithoutCallStack() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 8, "  return a / b;");
    JcpException ex = new JcpException("Division by zero", loc);

    String fullMessage = ex.getFullMessage();

    assertTrue(fullMessage.contains("Division by zero"));
    assertFalse(fullMessage.contains("Stack trace:"));
  }

  @Test
  void testGetFullMessageWithEmptyCallStack() {
    SourceLocation loc = SourceLocation.of("math.jcp", 15, 8);
    CallStack stack = new CallStack();  // empty

    JcpException ex = new JcpException("Division by zero", loc, stack);

    String fullMessage = ex.getFullMessage();

    assertTrue(fullMessage.contains("Division by zero"));
    assertFalse(fullMessage.contains("Stack trace:"));
  }

  @Test
  void testGetFullMessageWithoutLocation() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException ex = new JcpException("error", null, stack);

    String fullMessage = ex.getFullMessage();

    assertTrue(fullMessage.contains("error"));
    assertTrue(fullMessage.contains("Stack trace:"));
    assertTrue(fullMessage.contains("at func"));
  }

  // ==================== Combined withLocation and withCallStack Tests ====================

  @Test
  void testWithLocationPreservesCallStack() {
    CallStack stack = new CallStack();
    stack.push(StackFrame.of("func", null));

    JcpException original = new JcpException("error", null, stack);
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    JcpException withLoc = original.withLocation(loc);

    assertNotNull(withLoc.getCallStack());
    assertEquals(1, withLoc.getCallStack().size());
  }

  @Test
  void testBackwardCompatibilityWithoutCallStack() {
    JcpException ex = new JcpException("error message");

    assertNull(ex.getCallStack());
    assertEquals("error message", ex.getMessage());
    assertEquals("error message", ex.getFormattedMessage());
    assertEquals("error message", ex.getFullMessage());
  }
}
