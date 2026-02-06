package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.BooleanLiteral;
import com.elminster.jcp.ast.expression.operation.LogicalAndExpression;
import com.elminster.jcp.ast.expression.operation.LogicalOrExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LogicalEvaluator (And, Or).
 */
class LogicalEvaluatorTest {

    private RootEvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    /**
     * Tests true AND true returns true.
     * <pre>
     * var result: Boolean = true && true  // result = true
     * </pre>
     */
    @Test
    void testAnd_TrueAndTrue() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalAndExpression(
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(true))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(true, context.getVariable("result").get());
    }

    /**
     * Tests true AND false returns false.
     * <pre>
     * var result: Boolean = true && false  // result = false
     * </pre>
     */
    @Test
    void testAnd_TrueAndFalse() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalAndExpression(
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(false))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(false, context.getVariable("result").get());
    }

    /**
     * Tests false AND true returns false (short-circuit).
     * <pre>
     * var result: Boolean = false && true  // result = false
     * </pre>
     */
    @Test
    void testAnd_FalseAndTrue() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalAndExpression(
                LiteralExpression.of(BooleanLiteral.of(false)),
                LiteralExpression.of(BooleanLiteral.of(true))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(false, context.getVariable("result").get());
    }

    /**
     * Tests true OR false returns true.
     * <pre>
     * var result: Boolean = true || false  // result = true
     * </pre>
     */
    @Test
    void testOr_TrueOrFalse() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalOrExpression(
                LiteralExpression.of(BooleanLiteral.of(true)),
                LiteralExpression.of(BooleanLiteral.of(false))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(true, context.getVariable("result").get());
    }

    /**
     * Tests false OR true returns true.
     * <pre>
     * var result: Boolean = false || true  // result = true
     * </pre>
     */
    @Test
    void testOr_FalseOrTrue() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalOrExpression(
                LiteralExpression.of(BooleanLiteral.of(false)),
                LiteralExpression.of(BooleanLiteral.of(true))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(true, context.getVariable("result").get());
    }

    /**
     * Tests false OR false returns false.
     * <pre>
     * var result: Boolean = false || false  // result = false
     * </pre>
     */
    @Test
    void testOr_FalseOrFalse() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.BOOLEAN,
            new LogicalOrExpression(
                LiteralExpression.of(BooleanLiteral.of(false)),
                LiteralExpression.of(BooleanLiteral.of(false))
            )
        ));

        new EvalVisitor(context).visit(program);
        assertEquals(false, context.getVariable("result").get());
    }

    /**
     * Tests AND with non-boolean left operand throws CannotCastException.
     * <pre>
     * var result = 42 && true  // throws CannotCastException
     * </pre>
     */
    @Test
    void testAnd_NonBooleanLeft() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.ANY,
            new LogicalAndExpression(
                LiteralExpression.of(42),  // Int, not Boolean
                LiteralExpression.of(BooleanLiteral.of(true))
            )
        ));

        assertThrows(CannotCastException.class, () ->
            new EvalVisitor(context).visit(program)
        );
    }

    /**
     * Tests OR with non-boolean left operand throws CannotCastException.
     * <pre>
     * var result = "hello" || true  // throws CannotCastException
     * </pre>
     */
    @Test
    void testOr_NonBooleanLeft() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "result",
            SystemDataType.ANY,
            new LogicalOrExpression(
                LiteralExpression.of("hello"),  // String, not Boolean
                LiteralExpression.of(BooleanLiteral.of(true))
            )
        ));

        assertThrows(CannotCastException.class, () ->
            new EvalVisitor(context).visit(program)
        );
    }
}
