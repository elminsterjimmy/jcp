package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.ast.expression.operation.Minus;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.ReturnStatement;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclaration;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecursionTest {

    /**
     * function fibonacci(int n): int
     * if (n == 1) return 1
     * if (n == 2) return 2
     * return fibonacci(n - 1) + fibonacci(n - 2)
     * endfunc
     *
     * int num = fibonacci(10)
     */
    @Test
    public void testFibonacci() {
        String fibonacci = "fibonacci";
        Block functionBody = new BlockImpl();
        functionBody
                .addStatement(
                        new IfElseStatement(new ReturnStatement(LiteralExpression.of(1)), Equal.of(
                                VariableExpression.of("n"), LiteralExpression.of(1)
                        )))
                .addStatement(
                        new IfElseStatement(new ReturnStatement(LiteralExpression.of(2)), Equal.of(
                                VariableExpression.of("n"), LiteralExpression.of(2)
                        )))
                .addStatement(
                        new ReturnStatement(
                                Plus.of(
                                        new FunctionCallExpression(Identifier.fromName(fibonacci),
                                                Minus.of(VariableExpression.of("n"), LiteralExpression.of(1))),
                                        new FunctionCallExpression(Identifier.fromName(fibonacci),
                                                Minus.of(VariableExpression.of("n"), LiteralExpression.of(2)))
                                )
                        )
                );
        FunctionDeclaration functionDeclaration = new FunctionDeclarationImpl(
                Identifier.fromName(fibonacci), DataType.SystemDataType.INT,
                new ParameterDef[]{ParameterDef.of("n", DataType.SystemDataType.INT)},
                functionBody
        );

        Block block = new BlockImpl();
        block.addStatement(functionDeclaration);
        block.addStatement(new VariableDeclarationImpl("num", DataType.SystemDataType.INT,
                new FunctionCallExpression(Identifier.fromName(fibonacci), LiteralExpression.of(10))));

        EvalContext context = new RootEvalContext();
        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);

        Assertions.assertEquals(89, context.getVariable("num").get());
    }
}
