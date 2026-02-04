package com.elminster.jcp.compile.operator.relational;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for double comparison compilation with value verification.
 */
public class DoubleComparisonCompileTest extends AbstractCompileTest {

    @Test
    void testDoubleComparisonLessThan() throws Exception {
        // return 1.5 < 2.5  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(DoubleLiteral.of(1.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleLT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonLessThanFalse() throws Exception {
        // return 3.5 < 2.5  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(DoubleLiteral.of(3.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleLTFalse")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);
    }

    @Test
    void testDoubleComparisonGreaterThan() throws Exception {
        // return 3.14 > 2.0  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThan(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleGT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonGreaterThanFalse() throws Exception {
        // return 1.0 > 2.0  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new GreaterThan(
                        LiteralExpression.of(DoubleLiteral.of(1.0)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleGTFalse")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);
    }

    @Test
    void testDoubleComparisonEqual() throws Exception {
        // return 3.14 == 3.14  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Equal(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(3.14))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleEQ")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonEqualFalse() throws Exception {
        // return 3.14 == 2.0  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Equal(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleEQFalse")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);
    }

    @Test
    void testDoubleComparisonNotEqual() throws Exception {
        // return 3.14 != 2.0  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new NotEqual(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleNE")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);
    }

    @Test
    void testDoubleComparisonNotEqualFalse() throws Exception {
        // return 3.14 != 3.14  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new NotEqual(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(3.14))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestDoubleNEFalse")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);
    }
}
