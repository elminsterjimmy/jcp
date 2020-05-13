package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.StringFlowData;
import com.elminster.jcp.ast.express.core.FunctionCallExpression;
import com.elminster.jcp.ast.expression.ConstantExpression;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.func.test.LogFunction;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.EvalContextImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class IfElseBlockTest {

  @BeforeClass
  public static void init() {
  }

  /**
   * if (true) {
   * log(TRUE)
   * }
   */
  @Test
  public void testIfElseBlock() {
    FunctionCallExpression logCall = new FunctionCallExpression("log",
        new Expression[]{new ConstantExpression(StringFlowData.newString("TEST"))});
    Expression condition = new ConstantExpression(BooleanFlowData.BOOLEAN_TRUE);
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