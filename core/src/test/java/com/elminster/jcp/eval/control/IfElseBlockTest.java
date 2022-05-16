package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IfElseBlockTest {

  @BeforeAll
  public static void init() {
  }

  /**
   * if (true) {
 *     Logger.log(TRUE)
   * } else {
 *     Logger.log(FALSE)
   * }
   */
  @Test
  public void testIfElseBlock() {
    FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("Logger.log"),
        new Expression[]{new LiteralExpression(StringLiteral.of("TRUE"))});
    Expression condition = new LiteralExpression(Literal.of(true));
    Block ifBlock = new BlockImpl();
    ifBlock.addStatement(new ExpressionStatement(logCall));
    Block elseBlock = new BlockImpl();
    elseBlock.addStatement(new ExpressionStatement(new FunctionCallExpression
            (new IdentifierExpression("Logger.log"),
            new Expression[]{new LiteralExpression(StringLiteral.of("FALSE"))})));

    IfElseStatement ifElseStatement = new IfElseStatement(
        ifBlock, elseBlock, condition);

    EvalContext context = new RootEvalContext();

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(ifElseStatement);

    ifElseStatement = new IfElseStatement(
            ifBlock, elseBlock, new LiteralExpression(Literal.of(false)));

    visitor.visit(ifElseStatement);
  }
}