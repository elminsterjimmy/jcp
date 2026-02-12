package com.elminster.jcp.debug;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BreakpointIndex.
 */
class BreakpointIndexTest {

  private BreakpointIndex index;

  @BeforeEach
  void setUp() {
    index = new BreakpointIndex();
  }

  @Test
  void add_SourceLocation_AddsToIndex() {
    BreakpointId id = BreakpointId.next();
    BreakpointLocation location = BreakpointLocation.at(10, 5);

    index.add(id, location);

    assertTrue(index.hasBreakpointsAt(10));
    assertEquals(1, index.getAt(10).size());
  }

  @Test
  void add_NodeOnlyLocation_NotIndexed() {
    BreakpointId id = BreakpointId.next();
    // Create a node-only location with no source location
    BreakpointLocation location = BreakpointLocation.at(0); // line 0 means no source location
    // Actually need to test this differently - node-only breakpoints have line=0

    // We can't directly create a node-only location without a node,
    // but we can test with a location that has line 0
    index.add(id, location);

    assertFalse(index.hasBreakpointsAt(0));
  }

  @Test
  void add_MultipleBreakpointsSameLine_AllIndexed() {
    BreakpointId id1 = BreakpointId.next();
    BreakpointId id2 = BreakpointId.next();
    BreakpointLocation loc1 = BreakpointLocation.at(10, 1);
    BreakpointLocation loc2 = BreakpointLocation.at(10, 5);

    index.add(id1, loc1);
    index.add(id2, loc2);

    assertEquals(2, index.getAt(10).size());
  }

  @Test
  void remove_ExistingBreakpoint_RemovesFromIndex() {
    BreakpointId id = BreakpointId.next();
    BreakpointLocation location = BreakpointLocation.at(10, 5);

    index.add(id, location);
    index.remove(id, location);

    assertFalse(index.hasBreakpointsAt(10));
    assertTrue(index.isEmpty());
  }

  @Test
  void remove_NonExistent_NoError() {
    BreakpointId id = BreakpointId.next();
    BreakpointLocation location = BreakpointLocation.at(10, 5);

    // Should not throw
    index.remove(id, location);
  }

  @Test
  void remove_OneOfMultiple_LeavesOthers() {
    BreakpointId id1 = BreakpointId.next();
    BreakpointId id2 = BreakpointId.next();
    BreakpointLocation loc1 = BreakpointLocation.at(10, 1);
    BreakpointLocation loc2 = BreakpointLocation.at(10, 5);

    index.add(id1, loc1);
    index.add(id2, loc2);
    index.remove(id1, loc1);

    assertEquals(1, index.getAt(10).size());
    assertFalse(index.isEmpty());
  }

  @Test
  void getAt_NoBreakpoints_ReturnsEmptySet() {
    Set<BreakpointIndex.BreakpointEntry> entries = index.getAt(10);

    assertNotNull(entries);
    assertTrue(entries.isEmpty());
  }

  @Test
  void hasBreakpointsAt_NoBreakpoints_ReturnsFalse() {
    assertFalse(index.hasBreakpointsAt(10));
  }

  @Test
  void isEmpty_Initially_ReturnsTrue() {
    assertTrue(index.isEmpty());
  }

  @Test
  void isEmpty_AfterAdd_ReturnsFalse() {
    index.add(BreakpointId.next(), BreakpointLocation.at(10));

    assertFalse(index.isEmpty());
  }

  @Test
  void clear_RemovesAllBreakpoints() {
    index.add(BreakpointId.next(), BreakpointLocation.at(10));
    index.add(BreakpointId.next(), BreakpointLocation.at(20));

    index.clear();

    assertTrue(index.isEmpty());
    assertFalse(index.hasBreakpointsAt(10));
    assertFalse(index.hasBreakpointsAt(20));
  }

  @Test
  void breakpointEntry_ReturnsIdAndLocation() {
    BreakpointId id = BreakpointId.next();
    BreakpointLocation location = BreakpointLocation.at(10, 5);

    index.add(id, location);

    Set<BreakpointIndex.BreakpointEntry> entries = index.getAt(10);
    assertEquals(1, entries.size());

    BreakpointIndex.BreakpointEntry entry = entries.iterator().next();
    assertEquals(id, entry.getId());
    assertEquals(location, entry.getLocation());
  }

  @Test
  void remove_NodeOnlyLocation_NotAttempted() {
    BreakpointId id = BreakpointId.next();
    // location with line 0 (no source location)
    BreakpointLocation location = BreakpointLocation.at(0);

    // Should not throw - just returns without doing anything
    index.remove(id, location);
  }
}
