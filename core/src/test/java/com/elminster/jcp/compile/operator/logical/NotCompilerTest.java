package com.elminster.jcp.compile.operator.logical;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.GreaterThan;
import com.elminster.jcp.ast.expression.operation.LogicalNotExpression;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for logical NOT (!) compilation with value verification.
 */
public class NotCompilerTest extends AbstractCompileTest {

    @Test
    void testNotTrue() throws Exception {
        // return !true  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalNotExpression(
                        LiteralExpression.of(BooleanLiteral.of(true))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestNotTrue")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result, "!true should be false");
    }

    @Test
    void testNotFalse() throws Exception {
        // return !false  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalNotExpression(
                        LiteralExpression.of(BooleanLiteral.of(false))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestNotFalse")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "!false should be true");
    }

    @Test
    void testNotComparison() throws Exception {
        // return !(5 > 10)  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalNotExpression(
                        new GreaterThan(
                                LiteralExpression.of(IntLiteral.of(5)),
                                LiteralExpression.of(IntLiteral.of(10))
                        )
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestNotComparison")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "!(5 > 10) should be true");
    }

    @Test
    void testDoubleNegation() throws Exception {
        // return !!true  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalNotExpression(
                        new LogicalNotExpression(
                                LiteralExpression.of(BooleanLiteral.of(true))
                        )
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleNot")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "!!true should be true");
    }
}
