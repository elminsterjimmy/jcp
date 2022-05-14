package com.elminster.jcp.eval.function;

import com.elminster.common.util.AssertException;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.operation.Equal;
import com.elminster.jcp.ast.expression.operation.Plus;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AssertionsTest {

    /**
     * Assertions.assertTrue(true);
     *
     * Assertions.assertTrue(1 + 3 == 2);
     */
    @Test
    public void testAssertTure() {
        Block block = new BlockImpl();
        String functionName = "Assertions.assertTrue";

        block.addStatement(ExpressionStatement.of(new FunctionCallExpression(
                Identifier.fromName(functionName),
                LiteralExpression.of(Literal.of(true)))));
        EvalContext context = new RootEvalContext();
        EvalVisitor visitor = new EvalVisitor(context);
        visitor.visit(block);


        Statement statement = ExpressionStatement.of(new FunctionCallExpression(
                Identifier.fromName(functionName),
                new Equal(
                        new Plus(LiteralExpression.of(Literal.of(1)),
                                LiteralExpression.of(Literal.of(2))),
                        LiteralExpression.of(Literal.of(2))
                )));
        Assertions.assertThrows(AssertException.class, () -> visitor.visit(statement));

    }
}
