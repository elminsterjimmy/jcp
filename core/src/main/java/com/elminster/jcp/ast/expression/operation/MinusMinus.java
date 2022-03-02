package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.PostfixOperator;

public class MinusMinus extends AbstractUnaryExpression {

  public MinusMinus(Expression expression) {
    super(expression, PostfixOperator.MINUS_MINUS);
  }
}
