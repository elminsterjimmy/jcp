package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class Equal extends CompareExpression {

  public Equal(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.EQUAL);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return 0 == leftValue.compareTo(rightValue);
  }
}
