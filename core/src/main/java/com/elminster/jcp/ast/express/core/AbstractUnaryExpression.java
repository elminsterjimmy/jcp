package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.UnaryExpression;
import com.elminster.jcp.ast.expression.operator.UnaryOperator;

abstract public class AbstractUnaryExpression implements UnaryExpression {

  protected Expression expression;
  protected UnaryOperator operator;

  public AbstractUnaryExpression(Expression expression, UnaryOperator operator) {
    this.expression = expression;
    this.operator = operator;
  }

  @Override
  public Expression getExpress() {
    return expression;
  }

  @Override
  public UnaryOperator getOperator() {
    return operator;
  }

  @Override
  public String getName() {
    return getOperator().getName();
  }
}
