package com.elminster.jcp.compile.control;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.AssignmentExpression;
import com.elminster.jcp.ast.expression.operation.GreaterThan;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for if-else statement compilation.
 */
public class IfElseCompileTest extends AbstractCompileTest {

    @Test
    void testIfElseStatement() throws Exception {
        // int x = 10;
        // if (x > 5) {
        //     x = 1;
        // } else {
        //     x = 0;
        // }
        Block program = new BlockImpl();

        // int x = 10;
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(10)
        ));

        // if block: x = 1
        Block ifBlock = new BlockImpl();
        ifBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(1)
        )));

        // else block: x = 0
        Block elseBlock = new BlockImpl();
        elseBlock.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                LiteralExpression.of(0)
        )));

        // if (x > 5)
        program.addStatement(new IfElseStatement(
                ifBlock,
                elseBlock,
                new GreaterThan(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(5)
                )
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestIfElse"));
        assertNotNull(bytecode);
    }
}
