package com.elminster.jcp.ast;

import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.statement.BlockImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link Locatable} interface and {@link AbstractNode} implementation.
 */
class LocatableTest {

  @Test
  void testSetGetLocationOnExpression() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5, "1 + 2");

    plus.setLocation(loc);

    assertEquals(loc, plus.getLocation());
    assertEquals("test.jcp", plus.getLocation().getFilepath());
    assertEquals(10, plus.getLocation().getStartLine());
  }

  @Test
  void testSetGetLocationOnStatement() {
    WhileStatement whileStmt = new WhileStatement(BooleanLiteral.of(true), new BlockImpl());
    SourceLocation loc = SourceLocation.of("test.jcp", 20, 3, "while (true) {}");

    whileStmt.setLocation(loc);

    assertEquals(loc, whileStmt.getLocation());
    assertEquals(20, whileStmt.getLocation().getStartLine());
  }

  @Test
  void testNullLocationByDefault() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));

    assertNull(plus.getLocation());
  }

  @Test
  void testSetLocationToNull() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    plus.setLocation(loc);
    assertNotNull(plus.getLocation());

    plus.setLocation(null);
    assertNull(plus.getLocation());
  }

  @Test
  void testToStringWithLocation() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));
    SourceLocation loc = SourceLocation.of("test.jcp", 10, 5);

    plus.setLocation(loc);
    String str = plus.toString();

    assertTrue(str.contains("test.jcp:10:5"));
    assertTrue(str.contains(" at "));
  }

  @Test
  void testToStringWithoutLocation() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));

    String str = plus.toString();

    assertFalse(str.contains(" at "));
    assertFalse(str.contains(":"));
  }

  @Test
  void testLocationOnBlockImpl() {
    BlockImpl block = new BlockImpl();
    SourceLocation loc = SourceLocation.span("test.jcp", 5, 1, 10, 1, "{");

    block.setLocation(loc);

    assertEquals(loc, block.getLocation());
    assertTrue(block.getLocation().hasRange());
  }

  @Test
  void testLocationOnLiteral() {
    IntLiteral literal = IntLiteral.of(42);
    SourceLocation loc = SourceLocation.of("test.jcp", 5, 10, "int x = 42;");

    literal.setLocation(loc);

    assertEquals(loc, literal.getLocation());
    assertEquals("test.jcp", literal.getLocation().getFilepath());
    assertEquals(5, literal.getLocation().getStartLine());
    assertEquals(10, literal.getLocation().getStartColumn());
  }

  @Test
  void testLocatableInterfaceImplementation() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));

    // Verify Plus implements Locatable through AbstractNode
    assertTrue(plus instanceof Locatable);
  }

  @Test
  void testLocationWithRangeForSourceFormatting() {
    Plus plus = new Plus(IntLiteral.of(1), IntLiteral.of(2));
    SourceLocation loc = SourceLocation.span("test.jcp", 10, 5, 10, 10, "1 + 2");

    plus.setLocation(loc);

    String formatted = plus.getLocation().formatWithSource();

    assertTrue(formatted.contains("test.jcp:10:5"));
    assertTrue(formatted.contains("1 + 2"));
    assertTrue(formatted.contains("^"));
  }
}
