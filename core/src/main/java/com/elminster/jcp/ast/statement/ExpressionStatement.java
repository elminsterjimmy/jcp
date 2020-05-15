package com.elminster.jcp.ast.statement;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Statement;

public class ExpressionStatement extends AbstractStatement implements Statement {

  private final Expression expression;

  public ExpressionStatement(Expression expression) {
    Assert.notNull(expression);
    this.expression = expression;
  }

  @Override
  public String getName() {
    return "EXPRESSION";
  }

  /**
   * Gets expression.
   *
   * @return value of expression
   */
  public Expression getExpression() {
    return expression;
  }
}

