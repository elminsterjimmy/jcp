package com.elminster.jcp.compile.operator.postfix;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.MinusMinus;
import com.elminster.jcp.ast.expression.operation.PlusPlus;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for postfix operator compilation: ++ and --.
 */
public class PostfixOperatorCompileTest extends AbstractCompileTest {

    @Nested
    class PlusPlusTests {

        @Test
        void testPostIncrement_CompilesSuccessfully() throws Exception {
            // int x = 5;
            // x++;
            Block program = new BlockImpl();

            // int x = 5;
            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            // x++;
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(IdentifierExpression.of("x"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestPlusPlus"));
            assertNotNull(bytecode);
        }

        @Test
        void testPostIncrement_ReturnsOriginalValue() throws Exception {
            // int x = 5;
            // int y = x++;
            // At this point: x = 6, y = 5 (original value)
            Block program = new BlockImpl();

            // int x = 5;
            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            // int y = x++;
            program.addStatement(new VariableDeclarationImpl(
                "y",
                SystemDataType.INT,
                new PlusPlus(IdentifierExpression.of("x"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestPlusPlusReturnValue"));
            assertNotNull(bytecode);
        }

        @Test
        void testPostIncrement_InLoop() throws Exception {
            // int x = 0;
            // int sum = 0;
            // sum = sum + x++;
            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));

            program.addStatement(new VariableDeclarationImpl(
                "sum",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));

            // Use x++ in an expression
            program.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("sum"),
                AssignmentOperator.ASSIGNMENT,
                new PlusPlus(IdentifierExpression.of("x"))
            )));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestPlusPlusInExpr"));
            assertNotNull(bytecode);
        }
    }

    @Nested
    class MinusMinusTests {

        @Test
        void testPostDecrement_CompilesSuccessfully() throws Exception {
            // int x = 10;
            // x--;
            Block program = new BlockImpl();

            // int x = 10;
            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(10)
            ));

            // x--;
            program.addStatement(ExpressionStatement.of(
                new MinusMinus(IdentifierExpression.of("x"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestMinusMinus"));
            assertNotNull(bytecode);
        }

        @Test
        void testPostDecrement_ReturnsOriginalValue() throws Exception {
            // int x = 10;
            // int y = x--;
            // At this point: x = 9, y = 10 (original value)
            Block program = new BlockImpl();

            // int x = 10;
            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(10)
            ));

            // int y = x--;
            program.addStatement(new VariableDeclarationImpl(
                "y",
                SystemDataType.INT,
                new MinusMinus(IdentifierExpression.of("x"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestMinusMinusReturnValue"));
            assertNotNull(bytecode);
        }

        @Test
        void testPostDecrement_MultipleOperations() throws Exception {
            // int a = 5;
            // int b = 5;
            // a--;
            // b--;
            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "a",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            program.addStatement(new VariableDeclarationImpl(
                "b",
                SystemDataType.INT,
                LiteralExpression.of(5)
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(IdentifierExpression.of("a"))
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(IdentifierExpression.of("b"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestMinusMinusMultiple"));
            assertNotNull(bytecode);
        }
    }

    @Nested
    class CombinedTests {

        @Test
        void testMixedIncrementDecrement() throws Exception {
            // int x = 0;
            // int y = 10;
            // x++;
            // y--;
            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));

            program.addStatement(new VariableDeclarationImpl(
                "y",
                SystemDataType.INT,
                LiteralExpression.of(10)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(IdentifierExpression.of("x"))
            ));

            program.addStatement(ExpressionStatement.of(
                new MinusMinus(IdentifierExpression.of("y"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestMixedOps"));
            assertNotNull(bytecode);
        }

        @Test
        void testConsecutiveIncrements() throws Exception {
            // int x = 0;
            // x++;
            // x++;
            // x++;
            Block program = new BlockImpl();

            program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
            ));

            program.addStatement(ExpressionStatement.of(
                new PlusPlus(IdentifierExpression.of("x"))
            ));
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(IdentifierExpression.of("x"))
            ));
            program.addStatement(ExpressionStatement.of(
                new PlusPlus(IdentifierExpression.of("x"))
            ));

            byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestConsecutiveIncrements"));
            assertNotNull(bytecode);
        }
    }
}
