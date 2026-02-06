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

    /**
     * Tests int-to-double promotion on left operand.
     * <pre>
     * return 5 < 3.14  // => false (5.0 is not < 3.14)
     * </pre>
     */
    @Test
    void testMixedIntDoubleComparison_IntLeftDoubleRight() throws Exception {
        // return 5 (int) < 3.14 (double)  => false (promotes int to double)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(5),  // int
                        LiteralExpression.of(DoubleLiteral.of(3.14))  // double
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestMixedIntLeftDoubleLT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result);  // 5.0 < 3.14 is false
    }

    /**
     * Tests int-to-double promotion on right operand.
     * <pre>
     * return 3.14 < 5  // => true (3.14 is < 5.0)
     * </pre>
     */
    @Test
    void testMixedIntDoubleComparison_DoubleLeftIntRight() throws Exception {
        // return 3.14 (double) < 5 (int)  => true (promotes int to double)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LessThan(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),  // double
                        LiteralExpression.of(5)  // int
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestMixedDoubleLeftIntLT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result);  // 3.14 < 5.0 is true
    }
}
