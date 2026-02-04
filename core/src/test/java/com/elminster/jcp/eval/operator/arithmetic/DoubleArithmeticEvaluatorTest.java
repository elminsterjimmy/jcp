package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DOUBLE type arithmetic operations in eval mode.
 */
class DoubleArithmeticEvaluatorTest {

    @Test
    void testDoubleAddition() {
        // double x = 3.14 + 2.0;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Plus(
                        LiteralExpression.of(DoubleLiteral.of(3.14)),
                        LiteralExpression.of(DoubleLiteral.of(2.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(5.14, (Double) result.get(), 0.001);
    }

    @Test
    void testDoubleSubtraction() {
        // double x = 5.5 - 2.3;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Minus(
                        LiteralExpression.of(DoubleLiteral.of(5.5)),
                        LiteralExpression.of(DoubleLiteral.of(2.3))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(3.2, (Double) result.get(), 0.001);
    }

    @Test
    void testDoubleMultiplication() {
        // double x = 2.5 * 4.0;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Multi(
                        LiteralExpression.of(DoubleLiteral.of(2.5)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(10.0, (Double) result.get(), 0.001);
    }

    @Test
    void testDoubleDivision() {
        // double x = 10.0 / 4.0;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(10.0)),
                        LiteralExpression.of(DoubleLiteral.of(4.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(2.5, (Double) result.get(), 0.001);
    }

    @Test
    void testDoubleModulo() {
        // double x = 10.5 % 3.0;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Mod(
                        LiteralExpression.of(DoubleLiteral.of(10.5)),
                        LiteralExpression.of(DoubleLiteral.of(3.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(1.5, (Double) result.get(), 0.001);
    }

    @Test
    void testMixedIntDoubleAddition() {
        // int a = 5;
        // double b = a + 2.5;  (a should be promoted to double)
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));
        block.addStatement(new VariableDeclarationImpl(
                "b",
                SystemDataType.DOUBLE,
                new Plus(
                        VariableExpression.of("a"),
                        LiteralExpression.of(DoubleLiteral.of(2.5))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("b");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(7.5, (Double) result.get(), 0.001);
    }

    @Test
    void testDoubleDivisionByZero() {
        // double x = 5.0 / 0.0;  (should return Infinity)
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(5.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertTrue(Double.isInfinite((Double) result.get()));
    }

    @Test
    void testDoubleNaN() {
        // double x = 0.0 / 0.0;  (should return NaN)
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
                new Divide(
                        LiteralExpression.of(DoubleLiteral.of(0.0)),
                        LiteralExpression.of(DoubleLiteral.of(0.0))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertTrue(Double.isNaN((Double) result.get()));
    }

    @Test
    void testStringConcatWithDouble() {
        // string s = "value: " + 3.14;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "s",
                SystemDataType.STRING,
                new Plus(
                        LiteralExpression.of("value: "),
                        LiteralExpression.of(DoubleLiteral.of(3.14))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("s");
        assertEquals(SystemDataType.STRING, result.getDataType());
        assertEquals("value: 3.14", result.get());
    }

    @Test
    void testDoubleComparison() {
        // double a = 3.14;
        // double b = 2.0;
        // Note: Comparison evaluators use Comparable, so they should work with Double
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("a", SystemDataType.DOUBLE, LiteralExpression.of(DoubleLiteral.of(3.14))));
        block.addStatement(new VariableDeclarationImpl("b", SystemDataType.DOUBLE, LiteralExpression.of(DoubleLiteral.of(2.0))));
        block.addStatement(new VariableDeclarationImpl(
                "result",
                SystemDataType.BOOLEAN,
                new GreaterThan(
                        VariableExpression.of("a"),
                        VariableExpression.of("b")
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("result");
        assertEquals(SystemDataType.BOOLEAN, result.getDataType());
        assertTrue((Boolean) result.get());
    }

    @Test
    void testComplexDoubleExpression() {
        // double x = (3.0 + 2.0) * 4.0 - 10.0 / 2.0;  = 5.0 * 4.0 - 5.0 = 15.0
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.DOUBLE,
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
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(15.0, (Double) result.get(), 0.001);
    }
}
