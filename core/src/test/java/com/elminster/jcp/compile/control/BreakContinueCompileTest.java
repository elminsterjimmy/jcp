package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.LessThan;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.BreakStatement;
import com.elminster.jcp.ast.statement.control.ContinueStatement;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.compile.exception.CompileException;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for break and continue statement compilation.
 */
public class BreakContinueCompileTest extends AbstractCompileTest {

    @Test
    void testBreakInWhileLoop_CompilesSuccessfully() throws Exception {
        // int x = 0;
        // while (x < 100) {
        //     if (x == 5) {
        //         break;
        //     }
        //     x = x + 1;
        // }

        Block program = new BlockImpl();

        // int x = 0;
        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        // while body
        Block whileBody = new BlockImpl();

        // if (x == 5) { break; }
        Block ifBody = new BlockImpl();
        ifBody.addStatement(new BreakStatement());

        whileBody.addStatement(new IfElseStatement(
            ifBody,
            null,
            new Equal(IdentifierExpression.of("x"), LiteralExpression.of(5))
        ));

        // x = x + 1;
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("x"), LiteralExpression.of(1))
        )));

        // while (x < 100)
        program.addStatement(new WhileStatement(
            new LessThan(IdentifierExpression.of("x"), LiteralExpression.of(100)),
            whileBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestBreakInWhile"));
        assertNotNull(bytecode);
    }

    @Test
    void testContinueInWhileLoop_CompilesSuccessfully() throws Exception {
        // int x = 0;
        // int sum = 0;
        // while (x < 10) {
        //     x = x + 1;
        //     if (x == 5) {
        //         continue;
        //     }
        //     sum = sum + x;
        // }
        // (skips adding 5 to sum)

        Block program = new BlockImpl();

        // int x = 0;
        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        // int sum = 0;
        program.addStatement(new VariableDeclarationImpl(
            "sum",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        // while body
        Block whileBody = new BlockImpl();

        // x = x + 1;
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("x"), LiteralExpression.of(1))
        )));

        // if (x == 5) { continue; }
        Block ifBody = new BlockImpl();
        ifBody.addStatement(new ContinueStatement());

        whileBody.addStatement(new IfElseStatement(
            ifBody,
            null,
            new Equal(IdentifierExpression.of("x"), LiteralExpression.of(5))
        ));

        // sum = sum + x;
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("sum"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("sum"), IdentifierExpression.of("x"))
        )));

        // while (x < 10)
        program.addStatement(new WhileStatement(
            new LessThan(IdentifierExpression.of("x"), LiteralExpression.of(10)),
            whileBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestContinueInWhile"));
        assertNotNull(bytecode);
    }

    @Test
    void testNestedLoopWithBreak_CompilesSuccessfully() throws Exception {
        // int x = 0;
        // while (x < 5) {
        //     int y = 0;
        //     while (y < 5) {
        //         if (y == 3) {
        //             break;
        //         }
        //         y = y + 1;
        //     }
        //     x = x + 1;
        // }

        Block program = new BlockImpl();

        // int x = 0;
        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        // outer while body
        Block outerBody = new BlockImpl();

        // int y = 0;
        outerBody.addStatement(new VariableDeclarationImpl(
            "y",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        // inner while body
        Block innerBody = new BlockImpl();

        // if (y == 3) { break; }
        Block breakBody = new BlockImpl();
        breakBody.addStatement(new BreakStatement());

        innerBody.addStatement(new IfElseStatement(
            breakBody,
            null,
            new Equal(IdentifierExpression.of("y"), LiteralExpression.of(3))
        ));

        // y = y + 1;
        innerBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("y"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("y"), LiteralExpression.of(1))
        )));

        // inner while (y < 5)
        outerBody.addStatement(new WhileStatement(
            new LessThan(IdentifierExpression.of("y"), LiteralExpression.of(5)),
            innerBody
        ));

        // x = x + 1;
        outerBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("x"), LiteralExpression.of(1))
        )));

        // outer while (x < 5)
        program.addStatement(new WhileStatement(
            new LessThan(IdentifierExpression.of("x"), LiteralExpression.of(5)),
            outerBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestNestedLoopBreak"));
        assertNotNull(bytecode);
    }

    @Test
    void testBreakAndContinue_Combined() throws Exception {
        // int x = 0;
        // int count = 0;
        // while (x < 20) {
        //     x = x + 1;
        //     if (x == 5) {
        //         continue;
        //     }
        //     if (x == 15) {
        //         break;
        //     }
        //     count = count + 1;
        // }

        Block program = new BlockImpl();

        program.addStatement(new VariableDeclarationImpl(
            "x",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        program.addStatement(new VariableDeclarationImpl(
            "count",
            SystemDataType.INT,
            LiteralExpression.of(0)
        ));

        Block whileBody = new BlockImpl();

        // x = x + 1;
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("x"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("x"), LiteralExpression.of(1))
        )));

        // if (x == 5) { continue; }
        Block continueBody = new BlockImpl();
        continueBody.addStatement(new ContinueStatement());
        whileBody.addStatement(new IfElseStatement(
            continueBody,
            null,
            new Equal(IdentifierExpression.of("x"), LiteralExpression.of(5))
        ));

        // if (x == 15) { break; }
        Block breakBody = new BlockImpl();
        breakBody.addStatement(new BreakStatement());
        whileBody.addStatement(new IfElseStatement(
            breakBody,
            null,
            new Equal(IdentifierExpression.of("x"), LiteralExpression.of(15))
        ));

        // count = count + 1;
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
            Identifier.fromName("count"),
            AssignmentOperator.ASSIGNMENT,
            new Plus(IdentifierExpression.of("count"), LiteralExpression.of(1))
        )));

        program.addStatement(new WhileStatement(
            new LessThan(IdentifierExpression.of("x"), LiteralExpression.of(20)),
            whileBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestBreakContinueCombined"));
        assertNotNull(bytecode);
    }

    @Test
    void testBreakOutsideLoop_ThrowsCompileException() {
        // break; // Not in loop context - should throw CompileException
        Block program = new BlockImpl();
        program.addStatement(new BreakStatement());

        CompileException exception = assertThrows(CompileException.class, () -> {
            compiler.compileToBytes(program, uniqueClassName("TestBreakOutsideLoop"));
        });

        assertTrue(exception.getMessage().contains("break"));
        assertTrue(exception.getMessage().contains("outside"));
    }

    @Test
    void testContinueOutsideLoop_ThrowsCompileException() {
        // continue; // Not in loop context - should throw CompileException
        Block program = new BlockImpl();
        program.addStatement(new ContinueStatement());

        CompileException exception = assertThrows(CompileException.class, () -> {
            compiler.compileToBytes(program, uniqueClassName("TestContinueOutsideLoop"));
        });

        assertTrue(exception.getMessage().contains("continue"));
        assertTrue(exception.getMessage().contains("outside"));
    }
}
