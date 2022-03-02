package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.AbstractUnaryExpression;
import com.elminster.jcp.ast.expression.operation.operator.PostfixOperator;

public class Array extends AbstractUnaryExpression {

  public Array(Expression expression) {
    super(expression, PostfixOperator.ARRAY);
  }
}
