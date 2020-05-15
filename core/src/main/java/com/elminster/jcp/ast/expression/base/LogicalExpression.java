package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;

abstract public class LogicalExpression extends AbstractExpression {

  protected final LogicalOperator operator;

  public LogicalExpression(LogicalOperator operator) {
    this.operator = operator;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  @Override
  public String getName() {
    return operator.getName();
  }
}
