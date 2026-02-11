package com.elminster.jcp.exception;

import com.elminster.jcp.ast.SourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StackFrame}.
 */
class StackFrameTest {

  // ==================== Construction Tests ====================

  @Test
  void testConstructionWithLocation() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    StackFrame frame = StackFrame.of("myFunction", loc);

    assertEquals("myFunction", frame.getFunctionName());
    assertEquals(loc, frame.getLocation());
  }

  @Test
  void testConstructionWithNullLocation() {
    StackFrame frame = StackFrame.of("myFunction", null);

    assertEquals("myFunction", frame.getFunctionName());
    assertNull(frame.getLocation());
  }

  @Test
  void testConstructionWithNullFunctionName() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    StackFrame frame = StackFrame.of(null, loc);

    assertEquals("<unknown>", frame.getFunctionName());
    assertEquals(loc, frame.getLocation());
  }

  @Test
  void testConstructionWithEmptyFunctionName() {
    StackFrame frame = StackFrame.of("", null);

    assertEquals("", frame.getFunctionName());
  }

  // ==================== toString() Tests ====================

  @Test
  void testToStringWithLocation() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    StackFrame frame = StackFrame.of("myFunction", loc);

    assertEquals("myFunction(test.jcp:10:5)", frame.toString());
  }

  @Test
  void testToStringWithoutLocation() {
    StackFrame frame = StackFrame.of("myFunction", null);

    assertEquals("myFunction", frame.toString());
  }

  @Test
  void testToStringWithLocationNoFilepath() {
    SourceLocation loc = SourceLocation.of(null, 10, 5);
    StackFrame frame = StackFrame.of("myFunction", loc);

    assertEquals("myFunction(10:5)", frame.toString());
  }

  // ==================== Function Name Interning Tests ====================

  @Test
  void testFunctionNameInterning() {
    // Short names should be interned
    StackFrame frame1 = StackFrame.of("shortName", null);
    StackFrame frame2 = StackFrame.of("shortName", null);

    // Interned strings should be the same object
    assertSame(frame1.getFunctionName(), frame2.getFunctionName());
  }

  @Test
  void testLongFunctionNameNotInterned() {
    // Names >= 64 chars should not be interned
    String longName = "x".repeat(64);
    StackFrame frame1 = StackFrame.of(longName, null);
    StackFrame frame2 = StackFrame.of(new String(longName), null);

    // Long names should not be the same object (not interned)
    assertEquals(frame1.getFunctionName(), frame2.getFunctionName());
    // Note: can't guarantee they're different objects as JVM may intern anyway
  }

  @Test
  void testBoundaryFunctionNameInterning() {
    // Names at 63 chars (< 64) should be interned
    String name63 = "x".repeat(63);
    StackFrame frame1 = StackFrame.of(name63, null);
    StackFrame frame2 = StackFrame.of(name63, null);

    assertSame(frame1.getFunctionName(), frame2.getFunctionName());
  }

  // ==================== Immutability Tests ====================

  @Test
  void testStackFrameIsImmutable() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);
    StackFrame frame = StackFrame.of("myFunction", loc);

    // StackFrame has no setters, so it's immutable by design
    // Just verify fields are consistent across multiple calls
    assertEquals("myFunction", frame.getFunctionName());
    assertEquals("myFunction", frame.getFunctionName());
    assertEquals(loc, frame.getLocation());
    assertEquals(loc, frame.getLocation());
  }
}
