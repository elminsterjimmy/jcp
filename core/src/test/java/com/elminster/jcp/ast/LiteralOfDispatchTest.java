package com.elminster.jcp.ast;

import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.NullLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link Literal#of(Object)} dispatches to the correct typed subclass.
 */
class LiteralOfDispatchTest {

    @Test
    void integerDispatchesToIntLiteral() {
        Literal<?> lit = Literal.of(42);
        assertInstanceOf(IntLiteral.class, lit);
        assertEquals(42, lit.getValue());
    }

    @Test
    void doubleDispatchesToDoubleLiteral() {
        Literal<?> lit = Literal.of(3.14);
        assertInstanceOf(DoubleLiteral.class, lit);
        assertEquals(3.14, (double) lit.getValue(), 0.0001);
    }

    @Test
    void booleanDispatchesToBooleanLiteral() {
        Literal<?> lit = Literal.of(true);
        assertInstanceOf(BooleanLiteral.class, lit);
        assertEquals(true, lit.getValue());
    }

    @Test
    void stringDispatchesToStringLiteral() {
        Literal<?> lit = Literal.of("hello");
        assertInstanceOf(StringLiteral.class, lit);
        assertEquals("hello", lit.getValue());
    }

    @Test
    void nullDispatchesToNullLiteral() {
        Literal<?> lit = Literal.of(null);
        assertInstanceOf(NullLiteral.class, lit);
        assertNull(lit.getValue());
        assertSame(NullLiteral.INSTANCE, lit);
    }
}
