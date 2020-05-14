package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class NotEqual extends CompareExpression {

  public NotEqual(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.NOT_EQUAL);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return 0 != leftValue.compareTo(rightValue);
  }
}
