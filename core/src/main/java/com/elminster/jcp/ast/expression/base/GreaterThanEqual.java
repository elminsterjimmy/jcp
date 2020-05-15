package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class GreaterThanEqual extends CompareExpression {

  public GreaterThanEqual(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.GREATER_THAN_OR_EQUAL);
  }
}