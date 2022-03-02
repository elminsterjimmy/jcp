package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.LogicalOperator;

public class LogicalNotExpression extends LogicalExpression {

  private Expression expression;

  public LogicalNotExpression(Expression expression) {
    super(LogicalOperator.NOT);
    this.expression = expression;
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
