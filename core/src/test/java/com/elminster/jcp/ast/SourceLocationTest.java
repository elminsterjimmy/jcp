package com.elminster.jcp.ast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SourceLocation}.
 */
class SourceLocationTest {

  // ==================== Construction Tests ====================

  @Test
  void testValidConstruction() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertEquals("test.jcp", loc.getFilepath());
    assertEquals(10, loc.getStartLine());
    assertEquals(5, loc.getStartColumn());
    assertEquals(10, loc.getEndLine());
    assertEquals(5, loc.getEndColumn());
    assertNull(loc.getSourceLineContent());
  }

  @Test
  void testValidConstructionWithSourceContent() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5, "int x = 5;");

    assertEquals("test.jcp", loc.getFilepath());
    assertEquals(10, loc.getStartLine());
    assertEquals(5, loc.getStartColumn());
    assertEquals("int x = 5;", loc.getSourceLineContent());
  }

  @Test
  void testSpanConstruction() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 10, 15, "int x = 5;");

    assertEquals("test.jcp", loc.getFilepath());
    assertEquals(10, loc.getStartLine());
    assertEquals(5, loc.getStartColumn());
    assertEquals(10, loc.getEndLine());
    assertEquals(15, loc.getEndColumn());
    assertEquals("int x = 5;", loc.getSourceLineContent());
  }

  @Test
  void testSpanConstructionMultiLine() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 15, 3, "if (condition) {");

    assertEquals(10, loc.getStartLine());
    assertEquals(5, loc.getStartColumn());
    assertEquals(15, loc.getEndLine());
    assertEquals(3, loc.getEndColumn());
  }

  @Test
  void testNullFilepath() {
    SourceLocation loc = SourceLocation.of(null, 10, 5);

    assertNull(loc.getFilepath());
    assertEquals(10, loc.getStartLine());
    assertEquals(5, loc.getStartColumn());
  }

  @Test
  void testNullSourceContent() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 10, 15, null);

    assertNull(loc.getSourceLineContent());
  }

  // ==================== Validation Failure Tests ====================

  @Test
  void testInvalidStartLineThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        SourceLocation.of("test.jcp", 0, 5)
    );
    assertTrue(ex.getMessage().contains("startLine"));
  }

  @Test
  void testInvalidStartColumnThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        SourceLocation.of("test.jcp", 10, 0)
    );
    assertTrue(ex.getMessage().contains("startColumn"));
  }

  @Test
  void testEndLineBeforeStartLineThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        SourceLocation.span("test.jcp", 10, 5, 5, 10, null)
    );
    assertTrue(ex.getMessage().contains("endLine"));
  }

  @Test
  void testEndColumnBeforeStartColumnOnSameLineThrows() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        SourceLocation.span("test.jcp", 10, 10, 10, 5, null)
    );
    assertTrue(ex.getMessage().contains("endColumn"));
  }

  @Test
  void testEndColumnBeforeStartColumnOnDifferentLineAllowed() {
    // When on different lines, endColumn can be less than startColumn
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 10, 11, 5, null);

    assertEquals(10, loc.getStartColumn());
    assertEquals(5, loc.getEndColumn());
  }

  // ==================== Formatting Tests ====================

  @Test
  void testToStringWithFilepath() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertEquals("test.jcp:10:5", loc.toString());
  }

  @Test
  void testToStringWithoutFilepath() {
    SourceLocation loc = SourceLocation.of(null, 10, 5);

    assertEquals("10:5", loc.toString());
  }

  @Test
  void testFormatWithSourceSinglePosition() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5, "int x = 5;");

    String formatted = loc.formatWithSource();

    assertTrue(formatted.contains("test.jcp:10:5"));
    assertTrue(formatted.contains("10 | int x = 5;"));
    assertTrue(formatted.contains("^"));
  }

  @Test
  void testFormatWithSourceRange() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 10, 10, "int x = 5;");

    String formatted = loc.formatWithSource();

    assertTrue(formatted.contains("test.jcp:10:5"));
    assertTrue(formatted.contains("^"));
    assertTrue(formatted.contains("~"));
  }

  @Test
  void testFormatWithSourceNoContent() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    String formatted = loc.formatWithSource();

    assertEquals("test.jcp:10:5", formatted);
    assertFalse(formatted.contains("\n"));
  }

  // ==================== Helper Method Tests ====================

  @Test
  void testHasRangeTrue() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 10, 15, null);

    assertTrue(loc.hasRange());
  }

  @Test
  void testHasRangeTrueMultiLine() {
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 15, 3, null);

    assertTrue(loc.hasRange());
  }

  @Test
  void testHasRangeFalse() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertFalse(loc.hasRange());
  }

  // ==================== Value Semantics Tests ====================

  @Test
  void testEqualsAndHashCodeSame() {
    SourceLocation loc1 = SourceLocation.span("test.jcp", 10, 5, 10, 15, "int x;");
    SourceLocation loc2 = SourceLocation.span("test.jcp", 10, 5, 10, 15, "int x;");

    assertEquals(loc1, loc2);
    assertEquals(loc1.hashCode(), loc2.hashCode());
  }

  @Test
  void testEqualsDifferentFilepath() {
    SourceLocation loc1 = SourceLocation.of("test1.jcp", 10, 5);
    SourceLocation loc2 = SourceLocation.of("test2.jcp", 10, 5);

    assertNotEquals(loc1, loc2);
  }

  @Test
  void testEqualsDifferentLine() {
    SourceLocation loc1 = SourceLocation.of("test.jcp", 10, 5);
    SourceLocation loc2 = SourceLocation.of("test.jcp", 11, 5);

    assertNotEquals(loc1, loc2);
  }

  @Test
  void testEqualsDifferentColumn() {
    SourceLocation loc1 = SourceLocation.of("test.jcp", 10, 5);
    SourceLocation loc2 = SourceLocation.of("test.jcp", 10, 6);

    assertNotEquals(loc1, loc2);
  }

  @Test
  void testEqualsWithNull() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertNotEquals(loc, null);
  }

  @Test
  void testEqualsWithSelf() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertEquals(loc, loc);
  }

  @Test
  void testEqualsWithDifferentType() {
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    assertNotEquals(loc, "test.jcp:10:5");
  }

  @Test
  void testFilepathInterning() {
    String path1 = new String("test.jcp");
    String path2 = new String("test.jcp");

    // Before interning, they're different objects
    assertNotSame(path1, path2);

    SourceLocation loc1 = SourceLocation.of(path1, 10, 5);
    SourceLocation loc2 = SourceLocation.of(path2, 10, 5);

    // After interning in SourceLocation, they should be the same object
    assertSame(loc1.getFilepath(), loc2.getFilepath());
  }
}
