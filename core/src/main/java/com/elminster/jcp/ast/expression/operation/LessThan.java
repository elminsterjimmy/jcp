package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.RelationalOperator;

public class LessThan extends CompareExpression {

  public LessThan(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.LESS_THAN);
  }
}
