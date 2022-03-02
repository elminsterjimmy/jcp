package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.PostfixOperator;

public class PlusPlus extends AbstractUnaryExpression {

  public PlusPlus(Expression expression) {
    super(expression, PostfixOperator.PLUS_PLUS);
  }
}
