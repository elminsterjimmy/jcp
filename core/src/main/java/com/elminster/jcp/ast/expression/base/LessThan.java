package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class LessThan extends CompareExpression {

  public LessThan(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.LESS_THAN);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) < 0;
  }
}
