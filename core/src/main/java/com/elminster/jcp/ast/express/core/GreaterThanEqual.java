package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class GreaterThanEqual extends CompareExpression {

  public GreaterThanEqual(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.GREATER_THAN_OR_EQUAL);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) >= 0;
  }
}