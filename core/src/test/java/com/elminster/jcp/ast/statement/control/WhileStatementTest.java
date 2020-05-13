package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.IntegerFlowData;
import com.elminster.jcp.ast.data.StringFlowData;
import com.elminster.jcp.ast.express.core.*;
import com.elminster.jcp.ast.expression.ConstantExpression;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.func.test.LogFunction;
import com.elminster.jcp.ast.statement.*;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.EvalContextImpl;
import org.junit.BeforeClass;
import org.junit.Test;

public class WhileStatementTest {

  @BeforeClass
  public static void init() {
  }

  /**
   * int i = 0
   * while (true) {
   *   if (i == 3) {
   *     ++i
   *     continue
   *   }
   *   if (i > 5) {
   *     break
   *   }
   *   log("body" + i++)
   * }
   */
  @Test
  public void testWhileStatement() {
    Block block = new BlockImpl();
    VariableDeclaration variableDeclaration =
        new VariableDeclarationImpl("i", DataType.INT,
            new ConstantExpression(new IntegerFlowData(0)));

    Expression whileCondition = new ConstantExpression(BooleanFlowData.BOOLEAN_TRUE);

    Block body = new BlockImpl();
    FunctionCallExpression logCall = new FunctionCallExpression("log",
        new Expression[]{new Plus(
            new ConstantExpression(StringFlowData.newString("body")),
            new VariableExpression("i"))});
    Expression ifCondition = new Equal(new VariableExpression("i"), new ConstantExpression(IntegerFlowData.constInt(3)));
    IfElseStatement ifElseStatement = new IfElseStatement(
        new BlockImpl(new Statement[] {
            new ExpressionStatement(new PlusPlus(new VariableExpression("i"))),
            new ContinueStatement()
        })
        , ifCondition);
    body.addStatement(ifElseStatement);
    Expression ifCondition2 = new GreaterThan(new VariableExpression("i"), new ConstantExpression(IntegerFlowData.constInt(5)));
    IfElseStatement ifElseStatement2 = new IfElseStatement(new BreakStatement(), ifCondition2);
    body.addStatement(ifElseStatement);
    body.addStatement(ifElseStatement2);
    body.addStatement(new ExpressionStatement(logCall));
    body.addStatement(new ExpressionStatement(new PlusPlus(new VariableExpression("i"))));
    WhileStatement whileStatement = new WhileStatement(whileCondition, body);

    block.addStatement(variableDeclaration);
    block.addStatement(whileStatement);

    EvalContext context = new EvalContextImpl();
    context.addFunction(new LogFunction());

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(block);
  }
}
