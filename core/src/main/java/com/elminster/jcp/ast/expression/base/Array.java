package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.PostfixOperator;

public class Array extends AbstractUnaryExpression {

  public Array(Expression expression) {
    super(expression, PostfixOperator.ARRAY);
  }
}
