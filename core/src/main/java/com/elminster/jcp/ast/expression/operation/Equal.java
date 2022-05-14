package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.RelationalOperator;

public class Equal extends CompareExpression {

  public Equal(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.EQUAL);
  }

  public static Equal of(Expression leftOperand, Expression rightOperand) {
    return new Equal(leftOperand, rightOperand);
  }
}
