package com.elminster.jcp.compile.operator.logical;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.operation.LogicalAndExpression;
import com.elminster.jcp.ast.expression.operation.LogicalOrExpression;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for logical AND (&&) and OR (||) compilation with value verification.
 */
public class LogicalCompilerTest extends AbstractCompileTest {

    @Test
    void testAndBothTrue() throws Exception {
        // return true && true  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalAndExpression(
                        LiteralExpression.of(BooleanLiteral.of(true)),
                        LiteralExpression.of(BooleanLiteral.of(true))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestAndTT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "true && true should be true");
    }

    @Test
    void testAndTrueFalse() throws Exception {
        // return true && false  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalAndExpression(
                        LiteralExpression.of(BooleanLiteral.of(true)),
                        LiteralExpression.of(BooleanLiteral.of(false))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestAndTF")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result, "true && false should be false");
    }

    @Test
    void testAndFalseTrue() throws Exception {
        // return false && true  => false (short-circuit, right not evaluated)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalAndExpression(
                        LiteralExpression.of(BooleanLiteral.of(false)),
                        LiteralExpression.of(BooleanLiteral.of(true))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestAndFT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result, "false && true should be false");
    }

    @Test
    void testAndBothFalse() throws Exception {
        // return false && false  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalAndExpression(
                        LiteralExpression.of(BooleanLiteral.of(false)),
                        LiteralExpression.of(BooleanLiteral.of(false))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestAndFF")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result, "false && false should be false");
    }

    @Test
    void testOrBothTrue() throws Exception {
        // return true || true  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalOrExpression(
                        LiteralExpression.of(BooleanLiteral.of(true)),
                        LiteralExpression.of(BooleanLiteral.of(true))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestOrTT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "true || true should be true");
    }

    @Test
    void testOrTrueFalse() throws Exception {
        // return true || false  => true (short-circuit, right not evaluated)
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalOrExpression(
                        LiteralExpression.of(BooleanLiteral.of(true)),
                        LiteralExpression.of(BooleanLiteral.of(false))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestOrTF")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "true || false should be true");
    }

    @Test
    void testOrFalseTrue() throws Exception {
        // return false || true  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalOrExpression(
                        LiteralExpression.of(BooleanLiteral.of(false)),
                        LiteralExpression.of(BooleanLiteral.of(true))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestOrFT")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "false || true should be true");
    }

    @Test
    void testOrBothFalse() throws Exception {
        // return false || false  => false
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalOrExpression(
                        LiteralExpression.of(BooleanLiteral.of(false)),
                        LiteralExpression.of(BooleanLiteral.of(false))
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestOrFF")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertFalse(result, "false || false should be false");
    }

    @Test
    void testComplexExpression() throws Exception {
        // return (true && false) || (false || true)  => true
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new LogicalOrExpression(
                        new LogicalAndExpression(
                                LiteralExpression.of(BooleanLiteral.of(true)),
                                LiteralExpression.of(BooleanLiteral.of(false))
                        ),
                        new LogicalOrExpression(
                                LiteralExpression.of(BooleanLiteral.of(false)),
                                LiteralExpression.of(BooleanLiteral.of(true))
                        )
                ),
                SystemDataType.BOOLEAN,
                uniqueClassName("TestComplex")
        );

        Method evaluate = clazz.getMethod("evaluate");
        boolean result = (boolean) evaluate.invoke(null);
        assertTrue(result, "(true && false) || (false || true) should be true");
    }
}
