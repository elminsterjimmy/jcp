package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.RelationalOperator;

public class NotEqual extends CompareExpression {

  public NotEqual(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.NOT_EQUAL);
  }
}
