package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.PostfixOperator;

public class PlusPlus extends AbstractUnaryExpression {

  public PlusPlus(Expression expression) {
    super(expression, PostfixOperator.PLUS_PLUS);
  }
}
