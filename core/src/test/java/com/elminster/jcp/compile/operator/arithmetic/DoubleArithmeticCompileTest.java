package com.elminster.jcp.compile.operator.arithmetic;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for double arithmetic compilation with value verification.
 */
public class DoubleArithmeticCompileTest extends AbstractCompileTest {

    @Test
    void testDoubleAddition() throws Exception {
        // return 3.14 + 2.0  => 5.14
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Plus(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleAdd")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(5.14, result, 0.001);
    }

    @Test
    void testDoubleSubtraction() throws Exception {
        // return 5.5 - 2.3  => 3.2
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Minus(
                        LiteralExpression.of(DoubleLiteral.of(5.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.3))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleSub")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(3.2, result, 0.001);
    }

    @Test
    void testDoubleMultiplication() throws Exception {
        // return 2.5 * 4.0  => 10.0
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Multi(
                        LiteralExpression.of(DoubleLiteral.of(2.5)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleMul")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(10.0, result, 0.001);
    }

    @Test
    void testDoubleDivision() throws Exception {
        // return 10.0 / 4.0  => 2.5
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(10.0)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleDiv")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(2.5, result, 0.001);
    }

    @Test
    void testDoubleModulo() throws Exception {
        // return 10.5 % 3.0  => 1.5
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Mod(
                        LiteralExpression.of(DoubleLiteral.of(10.5)),
                        LiteralExpression.of(DoubleLiteral.of(3.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleMod")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(1.5, result, 0.001);
    }

    @Test
    void testMixedIntDoubleAddition() throws Exception {
        // int a = 5; return a + 2.5  => 7.5
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new Plus(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestMixedAdd")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(7.5, result, 0.001);
    }

    @Test
    void testMixedDoubleIntAddition() throws Exception {
        // double a = 2.5; return a + 5  => 7.5  (right operand is int)
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("a", SystemDataType.DOUBLE,
            LiteralExpression.of(DoubleLiteral.of(2.5))));

        Class<?> clazz = compiler.compileAndLoadWithReturn(
                program,
                new Plus(
                        IdentifierExpression.of("a"),
                        LiteralExpression.of(5)  // int literal
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestMixedDoubleInt")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(7.5, result, 0.001);
    }

    @Test
    void testComplexDoubleExpression() throws Exception {
        // return (3.0 + 2.0) * 4.0 - 10.0 / 2.0  => 5.0 * 4.0 - 5.0 = 15.0
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Minus(
                        new Multi(
                                new Plus(
                                        LiteralExpression.of(DoubleLiteral.of(3.0)),
                                        LiteralExpression.of(DoubleLiteral.of(2.0))
                                ),
                                LiteralExpression.of(DoubleLiteral.of(4.0))
                        ),
                        new Divide(
                                LiteralExpression.of(DoubleLiteral.of(10.0)),
                                LiteralExpression.of(DoubleLiteral.of(2.0))
                        )
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestComplexDouble")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    void testDoubleDivisionByZero() throws Exception {
        // return 5.0 / 0.0  => Infinity
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(5.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleDivZero")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertTrue(Double.isInfinite(result));
        assertTrue(result > 0);  // Positive infinity
    }

    @Test
    void testDoubleNaN() throws Exception {
        // return 0.0 / 0.0  => NaN
        Class<?> clazz = compiler.compileAndLoadWithReturn(
                null,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(0.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                ),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleNaN")
        );

        Method evaluate = clazz.getMethod("evaluate");
        double result = (double) evaluate.invoke(null);
        assertTrue(Double.isNaN(result));
    }

    @Test
    void testDoubleSpecialConstants() throws Exception {
        // Test DCONST_0 optimization: return 0.0  => 0.0
        Class<?> clazz0 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(DoubleLiteral.of(0.0)),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleConst0")
        );
        assertEquals(0.0, (double) clazz0.getMethod("evaluate").invoke(null), 0.001);

        // Test DCONST_1 optimization: return 1.0  => 1.0
        Class<?> clazz1 = compiler.compileAndLoadWithReturn(
                null,
                LiteralExpression.of(DoubleLiteral.of(1.0)),
                SystemDataType.DOUBLE,
                uniqueClassName("TestDoubleConst1")
        );
        assertEquals(1.0, (double) clazz1.getMethod("evaluate").invoke(null), 0.001);
    }
}
