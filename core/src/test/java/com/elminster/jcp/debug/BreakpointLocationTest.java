package com.elminster.jcp.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BreakpointLocation.
 */
class BreakpointLocationTest {

  @Test
  void at_LineOnly_CreatesLocation() {
    BreakpointLocation loc = BreakpointLocation.at(10);

    assertEquals(10, loc.getLine());
    assertEquals(1, loc.getColumn());
    assertNull(loc.getFilepath());
    assertTrue(loc.hasSourceLocation());
  }

  @Test
  void at_LineAndColumn_CreatesLocation() {
    BreakpointLocation loc = BreakpointLocation.at(10, 5);

    assertEquals(10, loc.getLine());
    assertEquals(5, loc.getColumn());
    assertNull(loc.getFilepath());
    assertTrue(loc.hasSourceLocation());
  }

  @Test
  void at_FileLineColumn_CreatesLocation() {
    BreakpointLocation loc = BreakpointLocation.at("test.jcp", 10, 5);

    assertEquals(10, loc.getLine());
    assertEquals(5, loc.getColumn());
    assertEquals("test.jcp", loc.getFilepath());
    assertTrue(loc.hasSourceLocation());
  }

  @Test
  void at_NodeWithLocation_ExtractsLocation() {
    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 15, 3));

    BreakpointLocation loc = BreakpointLocation.at(node);

    assertEquals(15, loc.getLine());
    assertEquals(3, loc.getColumn());
    assertEquals("test.jcp", loc.getFilepath());
    assertTrue(loc.hasSourceLocation());
  }

  @Test
  void at_NodeWithoutLocation_CreatesNodeOnlyBreakpoint() {
    IntLiteral node = IntLiteral.of(42);

    BreakpointLocation loc = BreakpointLocation.at(node);

    assertEquals(0, loc.getLine());
    assertEquals(0, loc.getColumn());
    assertNull(loc.getFilepath());
    assertFalse(loc.hasSourceLocation());
  }

  @Test
  void matches_SameLine_ReturnsTrue() {
    BreakpointLocation loc = BreakpointLocation.at(10);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertTrue(loc.matches(node));
  }

  @Test
  void matches_DifferentLine_ReturnsFalse() {
    BreakpointLocation loc = BreakpointLocation.at(10);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 11, 5));

    assertFalse(loc.matches(node));
  }

  @Test
  void matches_ExactColumnMatch_ReturnsTrue() {
    BreakpointLocation loc = BreakpointLocation.at(10, 5);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertTrue(loc.matches(node));
  }

  @Test
  void matches_DifferentColumn_ReturnsFalse() {
    BreakpointLocation loc = BreakpointLocation.at(10, 5);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 6));

    assertFalse(loc.matches(node));
  }

  @Test
  void matches_Column1MatchesAnyColumn_ReturnsTrue() {
    BreakpointLocation loc = BreakpointLocation.at(10, 1);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 15));

    assertTrue(loc.matches(node));
  }

  @Test
  void matches_FilepathMismatch_ReturnsFalse() {
    BreakpointLocation loc = BreakpointLocation.at("other.jcp", 10, 1);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertFalse(loc.matches(node));
  }

  @Test
  void matches_NodeReference_ReturnsTrue() {
    IntLiteral node = IntLiteral.of(42);
    BreakpointLocation loc = BreakpointLocation.at(node);

    assertTrue(loc.matches(node));
  }

  @Test
  void matches_NodeWithoutLocation_ReturnsFalse() {
    BreakpointLocation loc = BreakpointLocation.at(10);

    IntLiteral node = IntLiteral.of(42);
    // No location set

    assertFalse(loc.matches(node));
  }

  @Test
  void equals_SameLocation_ReturnsTrue() {
    BreakpointLocation loc1 = BreakpointLocation.at(10, 5);
    BreakpointLocation loc2 = BreakpointLocation.at(10, 5);

    assertEquals(loc1, loc2);
  }

  @Test
  void equals_DifferentLocation_ReturnsFalse() {
    BreakpointLocation loc1 = BreakpointLocation.at(10, 5);
    BreakpointLocation loc2 = BreakpointLocation.at(10, 6);

    assertNotEquals(loc1, loc2);
  }

  @Test
  void toString_SourceLocation_ReturnsFormattedString() {
    BreakpointLocation loc = BreakpointLocation.at("test.jcp", 10, 5);

    assertEquals("test.jcp:10:5", loc.toString());
  }

  @Test
  void toString_LineOnly_ReturnsFormattedString() {
    BreakpointLocation loc = BreakpointLocation.at(10, 5);

    assertEquals("10:5", loc.toString());
  }
}
