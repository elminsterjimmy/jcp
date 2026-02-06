package com.elminster.jcp.eval.operator.postfix;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.MinusMinus;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for postfix operator evaluators: ++ and -- (eval mode).
 */
class PostfixOperatorEvaluatorTest {

    private EvalContext context;

    @BeforeEach
    void setUp() {
        context = new RootEvalContext();
    }

    @Nested
    class PlusPlusEvaluatorTests {

        @Test
        void testPostIncrement_IncrementsVariable() {
            // int x = 5;
            // x++;
            // x should be 6

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(6, context.getVariable("x").get());
        }

        @Test
        void testPostIncrement_MultipleIncrements() {
            // int x = 0;
            // x++;
            // x++;
            // x++;
            // x should be 3

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(3, context.getVariable("x").get());
        }

        @Test
        void testPostIncrement_NegativeValue() {
            // int x = -5;
            // x++;
            // x should be -4

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(-5)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(-4, context.getVariable("x").get());
        }
    }

    @Nested
    class MinusMinusEvaluatorTests {

        @Test
        void testPostDecrement_DecrementsVariable() {
            // int x = 10;
            // x--;
            // x should be 9

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(10)
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(9, context.getVariable("x").get());
        }

        @Test
        void testPostDecrement_MultipleDecrements() {
            // int x = 5;
            // x--;
            // x--;
            // x--;
            // x should be 2

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(2, context.getVariable("x").get());
        }

        @Test
        void testPostDecrement_ToNegative() {
            // int x = 1;
            // x--;
            // x--;
            // x should be -1

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(1)
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(-1, context.getVariable("x").get());
        }
    }

    @Nested
    class CombinedTests {

        @Test
        void testMixedIncrementDecrement() {
            // int x = 5;
            // x++;
            // x--;
            // x++;
            // x should be 6

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("x")))
            ));
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("x")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(6, context.getVariable("x").get());
        }

        @Test
        void testMultipleVariables() {
            // int a = 0;
            // int b = 10;
            // a++;
            // b--;
            // a should be 1, b should be 9

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "a",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));
            program.addStatement(new VariableDeclarationImpl(
                "b",
                SystemDataType.INT,
                LiteralExpression.of(10)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("a")))
            ));
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("b")))
            ));

            new EvalVisitor(context).visit(program);

            assertEquals(1, context.getVariable("a").get());
            assertEquals(9, context.getVariable("b").get());
        }
    }

    @Nested
    class ErrorCaseTests {

        @Test
        void testPlusPlus_NonInteger_ThrowsException() {
            // String s = "hello";
            // s++;  // Should throw UnsupportedOperationException

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "s",
                com.elminster.jcp.eval.data.DataType.SystemDataType.STRING,
                com.elminster.jcp.ast.expression.LiteralExpression.of("hello")
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("s")))
            ));

            assertThrows(UnsupportedOperationException.class, () -> {
                new EvalVisitor(context).visit(program);
            });
        }

        @Test
        void testMinusMinus_NonInteger_ThrowsException() {
            // String s = "hello";
            // s--;  // Should throw UnsupportedOperationException

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "s",
                com.elminster.jcp.eval.data.DataType.SystemDataType.STRING,
                com.elminster.jcp.ast.expression.LiteralExpression.of("hello")
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(new VariableExpression(Identifier.fromName("s")))
            ));

            assertThrows(UnsupportedOperationException.class, () -> {
                new EvalVisitor(context).visit(program);
            });
        }

        @Test
        void testPlusPlus_Boolean_ThrowsException() {
            // boolean b = true;
            // b++;  // Should throw UnsupportedOperationException

            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "b",
                com.elminster.jcp.eval.data.DataType.SystemDataType.BOOLEAN,
                com.elminster.jcp.ast.expression.LiteralExpression.of(true)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(new VariableExpression(Identifier.fromName("b")))
            ));

            assertThrows(UnsupportedOperationException.class, () -> {
                new EvalVisitor(context).visit(program);
            });
        }
    }
}
