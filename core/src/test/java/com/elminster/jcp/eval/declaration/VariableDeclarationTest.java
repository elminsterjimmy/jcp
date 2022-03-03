package com.elminster.jcp.eval.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.*;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.*;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.CannotCastException;
import com.elminster.jcp.eval.test.LogFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VariableDeclarationTest {

    /**
     * int i = 10;
     * log("" + i);
     */
    @Test
    public void testIntVariableDeclaration() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("i"),
                DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(10)));

        FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("log"),
                new Plus(new LiteralExpression(StringLiteral.of("")), new VariableExpression(Identifier.fromName("i"))));

        block.addStatement(variableDeclaration)
                .addStatement(new ExpressionStatement(logCall));

        EvalContext context = new RootEvalContext();
        context.addFunction(new LogFunction());

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);
    }

    /**
     * String str = "str";
     * log(str);
     */
    @Test
    public void testStringVariableDeclaration() {
        Block block = new BlockImpl();

        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("str"),
                DataType.SystemDataType.STRING, new LiteralExpression(StringLiteral.of("str")));

        FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("log"),
                new Plus(new LiteralExpression(StringLiteral.of("")), new VariableExpression(Identifier.fromName("str"))));

        block.addStatement(variableDeclaration)
                .addStatement(new ExpressionStatement(logCall));

        EvalContext context = new RootEvalContext();
        context.addFunction(new LogFunction());

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);
    }

    /**
     * String str = 1;
     * log(str);
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
