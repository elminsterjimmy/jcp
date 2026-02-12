package com.elminster.jcp.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BreakpointId.
 */
class BreakpointIdTest {

  @Test
  void next_ReturnsUniqueIds() {
    BreakpointId id1 = BreakpointId.next();
    BreakpointId id2 = BreakpointId.next();
    BreakpointId id3 = BreakpointId.next();

    assertNotEquals(id1, id2);
    assertNotEquals(id2, id3);
    assertNotEquals(id1, id3);
  }

  @Test
  void getValue_ReturnsIncrementingValues() {
    BreakpointId id1 = BreakpointId.next();
    BreakpointId id2 = BreakpointId.next();

    assertTrue(id2.getValue() > id1.getValue());
  }

  @Test
  void equals_SameId() {
    BreakpointId id1 = BreakpointId.next();

    assertEquals(id1, id1);
  }

  @Test
  void equals_DifferentIds() {
    BreakpointId id1 = BreakpointId.next();
    BreakpointId id2 = BreakpointId.next();

    assertNotEquals(id1, id2);
  }

  @Test
  void equals_NullAndOtherType() {
    BreakpointId id1 = BreakpointId.next();

    assertNotEquals(null, id1);
    assertNotEquals("not a breakpoint id", id1);
  }

  @Test
  void hashCode_Consistent() {
    BreakpointId id1 = BreakpointId.next();

    assertEquals(id1.hashCode(), id1.hashCode());
  }

  @Test
  void toString_ContainsId() {
    BreakpointId id1 = BreakpointId.next();

    assertTrue(id1.toString().contains(String.valueOf(id1.getValue())));
    assertTrue(id1.toString().startsWith("Breakpoint#"));
  }
}
