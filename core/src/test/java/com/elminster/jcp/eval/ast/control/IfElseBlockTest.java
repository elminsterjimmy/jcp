package com.elminster.jcp.eval.ast.control;

import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.IdentifierExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.StringData;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.eval.ast.test.LogFunction;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.EvalContextImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class IfElseBlockTest {

  @BeforeAll
  public static void init() {
  }

  /**
   * if (true) {
   * log(TRUE)
   * }
   */
  @Test
  public void testIfElseBlock() throws Exception {
    FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("log"),
        new Expression[]{new LiteralExpression(StringLiteral.of("TEST"))});
    Expression condition = new LiteralExpression(Literal.of(true));
    Block ifBlock = new BlockImpl();
    ifBlock.addStatement(new ExpressionStatement(logCall));

    IfElseStatement ifElseStatement = new IfElseStatement(
        ifBlock, null, condition);

    EvalContext context = new EvalContextImpl();
    context.addFunction(new LogFunction());

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(ifElseStatement);
  }
}