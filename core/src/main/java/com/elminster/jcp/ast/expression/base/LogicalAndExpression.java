package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;

public class LogicalAndExpression extends LogicalExpression {

  private Expression left;
  private Expression right;

  public LogicalAndExpression(Expression left, Expression right) {
    super(LogicalOperator.AND);
    this.left = left;
    this.right = right;
  }

  /**
   * Gets left.
   *
   * @return value of left
   */
  public Expression getLeft() {
    return left;
  }

  /**
   * Gets right.
   *
   * @return value of right
   */
  public Expression getRight() {
    return right;
  }
}
