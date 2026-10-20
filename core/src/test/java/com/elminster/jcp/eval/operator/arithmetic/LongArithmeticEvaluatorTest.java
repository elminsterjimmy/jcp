package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.DoubleLiteral;
import com.elminster.jcp.ast.expression.literal.LongLiteral;
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

class LongArithmeticEvaluatorTest {

    @Test
    void testLongAddition() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x", SystemDataType.LONG,
                new Plus(
                        LiteralExpression.of(LongLiteral.of(3000000000L)),
                        LiteralExpression.of(LongLiteral.of(2000000000L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(5000000000L, (Long) result.get());
    }

    @Test
    void testLongSubtraction() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x", SystemDataType.LONG,
                new Minus(
                        LiteralExpression.of(LongLiteral.of(5000000000L)),
                        LiteralExpression.of(LongLiteral.of(2000000000L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(3000000000L, (Long) result.get());
    }

    @Test
    void testLongMultiplication() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x", SystemDataType.LONG,
                new Multi(
                        LiteralExpression.of(LongLiteral.of(100000L)),
                        LiteralExpression.of(LongLiteral.of(50000L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(5000000000L, (Long) result.get());
    }

    @Test
    void testLongDivision() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x", SystemDataType.LONG,
                new Divide(
                        LiteralExpression.of(LongLiteral.of(10000000000L)),
                        LiteralExpression.of(LongLiteral.of(4L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(2500000000L, (Long) result.get());
    }

    @Test
    void testLongModulo() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl(
                "x", SystemDataType.LONG,
                new Mod(
                        LiteralExpression.of(LongLiteral.of(10L)),
                        LiteralExpression.of(LongLiteral.of(3L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(1L, (Long) result.get());
    }

    @Test
    void testMixedIntLongAddition() {
        // int a = 5; long b = a + 3000000000L; (a promoted to long)
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));
        block.addStatement(new VariableDeclarationImpl(
                "b", SystemDataType.LONG,
                new Plus(
                        VariableExpression.of("a"),
                        LiteralExpression.of(LongLiteral.of(3000000000L))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("b");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(3000000005L, (Long) result.get());
    }

    @Test
    void testMixedLongDoubleAddition() {
        // long a = 100L; double b = a + 0.5; (a promoted to double)
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("a", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(100L))));
        block.addStatement(new VariableDeclarationImpl(
                "b", SystemDataType.DOUBLE,
                new Plus(
                        VariableExpression.of("a"),
                        LiteralExpression.of(DoubleLiteral.of(0.5))
                )
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("b");
        assertEquals(SystemDataType.DOUBLE, result.getDataType());
        assertEquals(100.5, (Double) result.get(), 0.001);
    }

    @Test
    void testLongComparison() {
        // long a = 3000000000L; long b = 2000000000L; boolean c = a > b;
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("a", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(3000000000L))));
        block.addStatement(new VariableDeclarationImpl("b", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(2000000000L))));
        block.addStatement(new VariableDeclarationImpl(
                "result", SystemDataType.BOOLEAN,
                new GreaterThan(VariableExpression.of("a"), VariableExpression.of("b"))
        ));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("result");
        assertEquals(SystemDataType.BOOLEAN, result.getDataType());
        assertTrue((Boolean) result.get());
    }

    @Test
    void testLongLiteralAssignment() {
        Block block = new BlockImpl();
        block.addStatement(new VariableDeclarationImpl("x", SystemDataType.LONG,
                LiteralExpression.of(LongLiteral.of(9223372036854775807L))));

        EvalContext context = new RootEvalContext();
        new EvalVisitor(context).visit(block);

        Data result = context.getVariable("x");
        assertEquals(SystemDataType.LONG, result.getDataType());
        assertEquals(Long.MAX_VALUE, (Long) result.get());
    }
}
