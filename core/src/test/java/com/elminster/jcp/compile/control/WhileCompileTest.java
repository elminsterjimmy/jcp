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
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.compile.AbstractCompileTest;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for while statement compilation.
 */
public class WhileCompileTest extends AbstractCompileTest {

    @Test
    void testWhileLoop() throws Exception {
        // int x = 0;
        // while (x < 10) {
        //     x = x + 1;
        // }
        Block program = new BlockImpl();

        // int x = 0;
        program.addStatement(new VariableDeclarationImpl(
                "x",
                SystemDataType.INT,
                LiteralExpression.of(0)
        ));

        // while body: x = x + 1;
        Block whileBody = new BlockImpl();
        whileBody.addStatement(ExpressionStatement.of(new AssignmentExpression(
                Identifier.fromName("x"),
                AssignmentOperator.ASSIGNMENT,
                new Plus(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(1)
                )
        )));

        // while (x < 10)
        program.addStatement(new WhileStatement(
                new LessThan(
                        IdentifierExpression.of("x"),
                        LiteralExpression.of(10)
                ),
                whileBody
        ));

        byte[] bytecode = compiler.compileToBytes(program, uniqueClassName("TestWhile"));
        assertNotNull(bytecode);
    }
}
