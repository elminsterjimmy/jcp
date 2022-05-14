package com.elminster.jcp.eval.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VariableDeclarationTest {

    /**
     * int i = 10;
     */
    @Test
    public void testIntVariableDeclaration() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("i"),
                DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(10)));

        block.addStatement(variableDeclaration);

        EvalContext context = new RootEvalContext();

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);

        Assertions.assertEquals(10, context.getVariable("i").get());
    }

    /**
     * String str = "str";
     */
    @Test
    public void testStringVariableDeclaration() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("str"),
                DataType.SystemDataType.STRING, new LiteralExpression(StringLiteral.of("str")));

        block.addStatement(variableDeclaration);

        EvalContext context = new RootEvalContext();

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);

        Assertions.assertEquals("str", context.getVariable("str").get());
    }

    /**
     * String str = 1;
     */
    @Test

    public void testStringVariableDeclarationInitWithIntValue() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("str"),
                DataType.SystemDataType.STRING, new LiteralExpression(IntLiteral.of(1)));

        block.addStatement(variableDeclaration);
        EvalContext context = new RootEvalContext();
        EvalVisitor visitor = new EvalVisitor(context);
        assertThrows(CannotCastException.class, () -> visitor.visit(block));
    }
}
