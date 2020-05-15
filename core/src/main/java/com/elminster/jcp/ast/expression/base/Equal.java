package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.RelationalOperator;

public class Equal extends CompareExpression {

  public Equal(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, RelationalOperator.EQUAL);
  }
}
