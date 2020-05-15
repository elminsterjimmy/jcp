package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.PostfixOperator;

public class MinusMinus extends AbstractUnaryExpression {

  public MinusMinus(Expression expression) {
    super(expression, PostfixOperator.MINUS_MINUS);
  }
}
