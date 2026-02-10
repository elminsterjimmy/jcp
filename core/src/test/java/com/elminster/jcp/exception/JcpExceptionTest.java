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

  // ==================== setLocation() Tests ====================

  @Test
  void testSetLocationOnExceptionWithoutLocation() {
    JcpException ex = new JcpException("error");
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    ex.setLocation(loc);

    assertEquals(loc, ex.getLocation());
    assertEquals("error at test.jcp:10:5", ex.getMessage());
  }

  @Test
  void testSetLocationDoesNotOverrideExisting() {
    SourceLocation loc1 = SourceLocation.of("first.jcp", 1, 1);
    SourceLocation loc2 = SourceLocation.of("second.jcp", 2, 2);
    JcpException ex = new JcpException("error", loc1);

    ex.setLocation(loc2);

    assertEquals(loc1, ex.getLocation());
  }

  @Test
  void testSetLocationNullIsAllowed() {
    JcpException ex = new JcpException("error");

    ex.setLocation(null);

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
}
