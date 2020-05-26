package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;

public class LogicalOrExpression extends LogicalExpression {

  private Expression left;
  private Expression right;

  public LogicalOrExpression(Expression left, Expression right) {
    super(LogicalOperator.OR);
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
