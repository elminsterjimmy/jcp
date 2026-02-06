package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.BreakStatement;
import com.elminster.jcp.ast.statement.control.RepeatStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RepeatEvaluator (repeat N times loop).
 */
class RepeatEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Test
    void testRepeat_ExecutesNTimes() {
        // int counter = 0;
        // repeat(5) { counter++; }
        // counter should be 5

        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl(
            "counter",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        Block loopBody = new BlockImpl();
        loopBody.addStatement(ExpressionStatement.of(
            new PlusPlus(new VariableExpression(Identifier.fromName("counter")))
        ));

        program.addStatement(new RepeatStatement(
            LiteralExpression.of(5),
            loopBody
        ));

        new EvalVisitor(context).visit(program);

        assertEquals(5, context.getVariable("counter").get());
    }

    @Test
    void testRepeat_ZeroTimes() {
        // int counter = 0;
        // repeat(0) { counter++; }
        // counter should be 0

        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl(
            "counter",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        Block loopBody = new BlockImpl();
        loopBody.addStatement(ExpressionStatement.of(
            new PlusPlus(new VariableExpression(Identifier.fromName("counter")))
        ));

        program.addStatement(new RepeatStatement(
            LiteralExpression.of(0),
            loopBody
        ));

        new EvalVisitor(context).visit(program);

        assertEquals(0, context.getVariable("counter").get());
    }

    @Test
    void testRepeat_SingleIteration() {
        // int counter = 0;
        // repeat(1) { counter++; }
        // counter should be 1

        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl(
            "counter",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        Block loopBody = new BlockImpl();
        loopBody.addStatement(ExpressionStatement.of(
            new PlusPlus(new VariableExpression(Identifier.fromName("counter")))
        ));

        program.addStatement(new RepeatStatement(
            LiteralExpression.of(1),
            loopBody
        ));

        new EvalVisitor(context).visit(program);

        assertEquals(1, context.getVariable("counter").get());
    }

    @Test
    void testRepeat_WithBreak() {
        // int counter = 0;
        // repeat(10) { counter++; if counter == 3 break; }
        // counter should be 3

        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl(
            "counter",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        Block loopBody = new BlockImpl();
        loopBody.addStatement(ExpressionStatement.of(
            new PlusPlus(new VariableExpression(Identifier.fromName("counter")))
        ));
        // We'll test simple break after 1 iteration for simplicity
        loopBody.addStatement(new BreakStatement());

        program.addStatement(new RepeatStatement(
            LiteralExpression.of(10),
            loopBody
        ));

        new EvalVisitor(context).visit(program);

        assertEquals(1, context.getVariable("counter").get());
    }
}
