package com.elminster.jcp.eval.declaration;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.test.LogFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VariableScopeTest {

    /**
     * <code>
     *     int i = 0;
     *     function func() {
     *         int i = 1;
     *         int j = 2;
     *         log(i); // should be 1 here
     *         log(j); // should be 2 here
     *     }
     *     func();
     *     log(i); // should be 0 here
     *     log(j); // should be uncleared
     *
     *
     * </code>
     */
    @Test
    public void testVariableScope() {
        VariableDeclaration variableDeclaration = new VariableDeclarationImpl(Identifier.fromName("i"),
                DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(0)));

        Block functionBlock = new BlockImpl();
        functionBlock
                .addStatement(new VariableDeclarationImpl(Identifier.fromName("i"),
                    DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(1))))
                .addStatement(new VariableDeclarationImpl(Identifier.fromName("j"),
                        DataType.SystemDataType.INT, new LiteralExpression(IntLiteral.of(2))))
                .addStatement(new ExpressionStatement(new FunctionCallExpression(new IdentifierExpression("log"),
                        new VariableExpression(Identifier.fromName("i")))))
                .addStatement(new ExpressionStatement(new FunctionCallExpression(new IdentifierExpression("log"),
                        new VariableExpression(Identifier.fromName("j")))));

        FunctionDeclaration functionDeclaration = new FunctionDeclarationImpl(Identifier.fromName("func"),
                DataType.SystemDataType.VOID, new ParameterDef[0], functionBlock);

        Block block = new BlockImpl();
        block.addStatement(variableDeclaration)
                .addStatement(functionDeclaration)
                .addStatement(new ExpressionStatement(new FunctionCallExpression(Identifier.fromName("func"), new Expression[0])))
                .addStatement(new ExpressionStatement(new FunctionCallExpression(new IdentifierExpression("log"),
                        new VariableExpression(Identifier.fromName("i")))));

        EvalContext context = new RootEvalContext();
        context.addFunction(new LogFunction());

        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);


        block.addStatement(new ExpressionStatement(new FunctionCallExpression(new IdentifierExpression("log"),
                new VariableExpression(Identifier.fromName("j")))));


        context = new RootEvalContext();
        context.addFunction(new LogFunction());

        visitor = new EvalVisitor(context);
        EvalVisitor finalVisitor = visitor;
        Assertions.assertThrows(UndeclaredException.VariableUndeclaredException.class,
                () -> finalVisitor.visit(block));

    }
}
