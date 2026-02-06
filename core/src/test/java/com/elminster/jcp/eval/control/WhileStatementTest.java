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

import static org.junit.jupiter.api.Assertions.*;

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

  /**
   * Tests nested while loops.
   * <pre>
   * int outer = 0
   * int inner = 0
   * while (outer < 2) {
   *   while (inner < 3) {
   *     inner++
   *   }
   *   outer++
   * }
   * // outer = 2, inner = 3
   * </pre>
   */
  @Test
  public void testNestedWhileLoops() {
    Block block = new BlockImpl();

    // int outer = 0
    VariableDeclaration outerVar = new VariableDeclarationImpl(
        new IdentifierExpression("outer"),
        DataType.SystemDataType.INT,
        new LiteralExpression(Literal.of(0))
    );
    block.addStatement(outerVar);

    // int inner = 0
    VariableDeclaration innerVar = new VariableDeclarationImpl(
        new IdentifierExpression("inner"),
        DataType.SystemDataType.INT,
        new LiteralExpression(Literal.of(0))
    );
    block.addStatement(innerVar);

    // Inner while body: inner++
    Block innerBody = new BlockImpl();
    innerBody.addStatement(new ExpressionStatement(
        new PlusPlus(new VariableExpression(new IdentifierExpression("inner")))
    ));

    // Inner while condition: inner < 3
    Expression innerCondition = new LessThan(
        new VariableExpression(new IdentifierExpression("inner")),
        new LiteralExpression(Literal.of(3))
    );

    // Inner while loop
    WhileStatement innerWhile = new WhileStatement(innerCondition, innerBody);

    // Outer while body: inner loop + outer++
    Block outerBody = new BlockImpl();
    outerBody.addStatement(innerWhile);
    outerBody.addStatement(new ExpressionStatement(
        new PlusPlus(new VariableExpression(new IdentifierExpression("outer")))
    ));

    // Outer while condition: outer < 2
    Expression outerCondition = new LessThan(
        new VariableExpression(new IdentifierExpression("outer")),
        new LiteralExpression(Literal.of(2))
    );

    // Outer while loop
    WhileStatement outerWhile = new WhileStatement(outerCondition, outerBody);
    block.addStatement(outerWhile);

    EvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(block);

    assertEquals(2, context.getVariable("outer").get());
    // Inner only runs once because it's reset between outer iterations
    assertEquals(3, context.getVariable("inner").get());
  }

  /**
   * Tests simple while loop with counter.
   * <pre>
   * int count = 0
   * while (count < 5) {
   *   count++
   * }
   * // count = 5
   * </pre>
   */
  @Test
  public void testSimpleWhileLoop() {
    Block block = new BlockImpl();

    // int count = 0
    VariableDeclaration countVar = new VariableDeclarationImpl(
        new IdentifierExpression("count"),
        DataType.SystemDataType.INT,
        new LiteralExpression(Literal.of(0))
    );
    block.addStatement(countVar);

    // while body: count++
    Block whileBody = new BlockImpl();
    whileBody.addStatement(new ExpressionStatement(
        new PlusPlus(new VariableExpression(new IdentifierExpression("count")))
    ));

    // while condition: count < 5
    Expression condition = new LessThan(
        new VariableExpression(new IdentifierExpression("count")),
        new LiteralExpression(Literal.of(5))
    );

    // while loop
    WhileStatement whileStmt = new WhileStatement(condition, whileBody);
    block.addStatement(whileStmt);

    EvalContext context = new RootEvalContext();
    new EvalVisitor(context).visit(block);

    assertEquals(5, context.getVariable("count").get());
  }
}
