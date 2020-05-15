package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class GreaterThan extends CompareExpression {

  public GreaterThan(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.GREATER_THAN);
  }
}
