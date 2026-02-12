package com.elminster.jcp.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Breakpoint.
 */
class BreakpointTest {

  // ========== ID Tests ==========

  @Test
  void at_ReturnsUniqueIds() {
    Breakpoint bp1 = Breakpoint.at(10);
    Breakpoint bp2 = Breakpoint.at(20);
    Breakpoint bp3 = Breakpoint.at(30);

    assertNotEquals(bp1.getId(), bp2.getId());
    assertNotEquals(bp2.getId(), bp3.getId());
    assertNotEquals(bp1.getId(), bp3.getId());
  }

  @Test
  void getId_ReturnsIncrementingValues() {
    Breakpoint bp1 = Breakpoint.at(10);
    Breakpoint bp2 = Breakpoint.at(20);

    assertTrue(bp2.getId() > bp1.getId());
  }

  // ========== Location Factory Tests ==========

  @Test
  void at_LineOnly_CreatesBreakpoint() {
    Breakpoint bp = Breakpoint.at(10);

    assertEquals(10, bp.getLine());
    assertEquals(1, bp.getColumn());
    assertNull(bp.getFilepath());
    assertTrue(bp.hasSourceLocation());
  }

  @Test
  void at_LineAndColumn_CreatesBreakpoint() {
    Breakpoint bp = Breakpoint.at(10, 5);

    assertEquals(10, bp.getLine());
    assertEquals(5, bp.getColumn());
    assertNull(bp.getFilepath());
    assertTrue(bp.hasSourceLocation());
  }

  @Test
  void at_FileLineColumn_CreatesBreakpoint() {
    Breakpoint bp = Breakpoint.at("test.jcp", 10, 5);

    assertEquals(10, bp.getLine());
    assertEquals(5, bp.getColumn());
    assertEquals("test.jcp", bp.getFilepath());
    assertTrue(bp.hasSourceLocation());
  }

  @Test
  void at_NodeWithLocation_ExtractsLocation() {
    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 15, 3));

    Breakpoint bp = Breakpoint.at(node);

    assertEquals(15, bp.getLine());
    assertEquals(3, bp.getColumn());
    assertEquals("test.jcp", bp.getFilepath());
    assertTrue(bp.hasSourceLocation());
  }

  @Test
  void at_NodeWithoutLocation_CreatesNodeOnlyBreakpoint() {
    IntLiteral node = IntLiteral.of(42);

    Breakpoint bp = Breakpoint.at(node);

    assertEquals(0, bp.getLine());
    assertEquals(0, bp.getColumn());
    assertNull(bp.getFilepath());
    assertFalse(bp.hasSourceLocation());
  }

  // ========== Matching Tests ==========

  @Test
  void matches_SameLine_ReturnsTrue() {
    Breakpoint bp = Breakpoint.at(10);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertTrue(bp.matches(node));
  }

  @Test
  void matches_DifferentLine_ReturnsFalse() {
    Breakpoint bp = Breakpoint.at(10);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 11, 5));

    assertFalse(bp.matches(node));
  }

  @Test
  void matches_ExactColumnMatch_ReturnsTrue() {
    Breakpoint bp = Breakpoint.at(10, 5);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertTrue(bp.matches(node));
  }

  @Test
  void matches_DifferentColumn_ReturnsFalse() {
    Breakpoint bp = Breakpoint.at(10, 5);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 6));

    assertFalse(bp.matches(node));
  }

  @Test
  void matches_Column1MatchesAnyColumn_ReturnsTrue() {
    Breakpoint bp = Breakpoint.at(10, 1);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 15));

    assertTrue(bp.matches(node));
  }

  @Test
  void matches_FilepathMismatch_ReturnsFalse() {
    Breakpoint bp = Breakpoint.at("other.jcp", 10, 1);

    IntLiteral node = IntLiteral.of(42);
    node.setLocation(SourceLocation.of("test.jcp", 10, 5));

    assertFalse(bp.matches(node));
  }

  @Test
  void matches_NodeReference_ReturnsTrue() {
    IntLiteral node = IntLiteral.of(42);
    Breakpoint bp = Breakpoint.at(node);

    assertTrue(bp.matches(node));
  }

  @Test
  void matches_NodeWithoutLocation_ReturnsFalse() {
    Breakpoint bp = Breakpoint.at(10);

    IntLiteral node = IntLiteral.of(42);
    // No location set

    assertFalse(bp.matches(node));
  }

  // ========== Equality Tests ==========

  @Test
  void equals_SameBreakpoint_ReturnsTrue() {
    Breakpoint bp = Breakpoint.at(10);

    assertEquals(bp, bp);
  }

  @Test
  void equals_DifferentBreakpoints_ReturnsFalse() {
    Breakpoint bp1 = Breakpoint.at(10);
    Breakpoint bp2 = Breakpoint.at(10);

    // Different IDs, so not equal
    assertNotEquals(bp1, bp2);
  }

  @Test
  void equals_NullAndOtherType() {
    Breakpoint bp = Breakpoint.at(10);

    assertNotEquals(null, bp);
    assertNotEquals("not a breakpoint", bp);
  }

  @Test
  void hashCode_Consistent() {
    Breakpoint bp = Breakpoint.at(10);

    assertEquals(bp.hashCode(), bp.hashCode());
  }

  // ========== toString Tests ==========

  @Test
  void toString_WithFilepath_ReturnsFormattedString() {
    Breakpoint bp = Breakpoint.at("test.jcp", 10, 5);

    String str = bp.toString();
    assertTrue(str.contains("Breakpoint#"));
    assertTrue(str.contains("test.jcp"));
    assertTrue(str.contains("10"));
    assertTrue(str.contains("5"));
  }

  @Test
  void toString_LineOnly_ReturnsFormattedString() {
    Breakpoint bp = Breakpoint.at(10, 5);

    String str = bp.toString();
    assertTrue(str.contains("Breakpoint#"));
    assertTrue(str.contains("10"));
    assertTrue(str.contains("5"));
  }

  @Test
  void toString_NodeOnly_ContainsNodeName() {
    IntLiteral node = IntLiteral.of(42);
    Breakpoint bp = Breakpoint.at(node);

    String str = bp.toString();
    assertTrue(str.contains("Breakpoint#"));
    assertTrue(str.contains("node:"));
  }
}
