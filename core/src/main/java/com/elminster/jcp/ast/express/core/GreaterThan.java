package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class GreaterThan extends CompareExpression {

  public GreaterThan(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.GREATER_THAN);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) > 0;
  }
}
