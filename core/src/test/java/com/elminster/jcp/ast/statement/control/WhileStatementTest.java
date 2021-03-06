package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.*;
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
   * ValueBuffer vb = ValueBuffer.new()
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
   * log(vb.length())
   */
  @Test
  public void testWhileStatement() throws Exception {
    Block block = new BlockImpl();
    VariableDeclaration variableDeclaration =
        new VariableDeclarationImpl(new IdentifierExpression("i"), DataType.SystemDataType.INT.getName(),
            new LiteralExpression(new IntegerData(0)));
    VariableDeclaration vbVariable = new VariableDeclarationImpl(new IdentifierExpression("vb"), "ValueBuffer",
        new FunctionCallExpression(new IdentifierExpression("ValueBuffer.new")));

    Expression whileCondition = new LiteralExpression(BooleanData.BOOLEAN_TRUE);

    Block body = new BlockImpl();
    FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("log"),
        new Expression[]{new Plus(
            new LiteralExpression(StringData.newString("body")),
            new VariableExpression(new IdentifierExpression("i")))});
    Expression ifCondition = new Equal(new VariableExpression(new IdentifierExpression("i")), new LiteralExpression(IntegerData.constInt(3)));
    IfElseStatement ifElseStatement = new IfElseStatement(
        new BlockImpl(new Statement[]{
            new ExpressionStatement(new PlusPlus(new VariableExpression(new IdentifierExpression("i")))),
            new ContinueStatement()
        })
        , ifCondition);
    body.addStatement(ifElseStatement);
    Expression ifCondition2 = new GreaterThan(new VariableExpression(new IdentifierExpression("i")), new LiteralExpression(IntegerData.constInt(5)));
    IfElseStatement ifElseStatement2 = new IfElseStatement(new BreakStatement(), ifCondition2);
    body.addStatement(ifElseStatement);
    body.addStatement(ifElseStatement2);
    body.addStatement(new ExpressionStatement(logCall));
    body.addStatement(new ExpressionStatement(new PlusPlus(new VariableExpression(new IdentifierExpression("i")))));
    WhileStatement whileStatement = new WhileStatement(whileCondition, body);

    block.addStatement(variableDeclaration);
    block.addStatement(vbVariable);
    block.addStatement(whileStatement);

    EvalContext context = new EvalContextImpl();
    context.addFunction(new LogFunction());

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(block);
  }
}
