package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.expression.literal.Literal;
import com.elminster.jcp.ast.expression.literal.StringLiteral;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.ast.statement.control.BreakStatement;
import com.elminster.jcp.ast.statement.control.ContinueStatement;
import com.elminster.jcp.ast.statement.control.IfElseStatement;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.ast.statement.declaration.VariableDeclaration;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.eval.EvalVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataTypeImpl;
import com.elminster.jcp.eval.data.DataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WhileStatementTest {

  @BeforeAll
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
  public void testWhileStatement() {
    Block block = new BlockImpl();
    VariableDeclaration variableDeclaration =
        new VariableDeclarationImpl(new IdentifierExpression("i"), DataType.SystemDataType.INT,
            new LiteralExpression(Literal.of(0)));
    VariableDeclaration vbVariable = new VariableDeclarationImpl(new IdentifierExpression("vb"), new DataTypeImpl("ValueBuffer"),
            new FunctionCallExpression(new IdentifierExpression("ValueBuffer.new")));

    Expression whileCondition = new LiteralExpression(Literal.of(true));

    Block body = new BlockImpl();
    FunctionCallExpression logCall = new FunctionCallExpression(new IdentifierExpression("Logger.log"),
        new Expression[]{new Plus(
            new LiteralExpression(StringLiteral.of("body")),
            new VariableExpression(new IdentifierExpression("i")))});
    Expression ifCondition = new Equal(new VariableExpression(new IdentifierExpression("i")), new LiteralExpression(Literal.of(3)));
    IfElseStatement ifElseStatement = new IfElseStatement(
        new BlockImpl(new Statement[]{
            new ExpressionStatement(new PlusPlus(new VariableExpression(new IdentifierExpression("i")))),
            new ContinueStatement()
        })
        , ifCondition);
    body.addStatement(ifElseStatement);
    Expression ifCondition2 = new GreaterThan(new VariableExpression(new IdentifierExpression("i")), new LiteralExpression(Literal.of(5)));
    IfElseStatement ifElseStatement2 = new IfElseStatement(new BreakStatement(), ifCondition2);
    body.addStatement(ifElseStatement);
    body.addStatement(ifElseStatement2);
    body.addStatement(new ExpressionStatement(logCall));
    body.addStatement(new ExpressionStatement(new PlusPlus(new VariableExpression(new IdentifierExpression("i")))));
    WhileStatement whileStatement = new WhileStatement(whileCondition, body);

    block.addStatement(variableDeclaration);
    block.addStatement(vbVariable);
    block.addStatement(whileStatement);

    EvalContext context = new RootEvalContext();

    EvalVisitor visitor = new EvalVisitor(context);
    visitor.visit(block);
  }
}
