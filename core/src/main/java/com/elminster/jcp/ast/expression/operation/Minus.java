package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.ArithmeticOperator;

public class Minus extends ArithmeticExpression {

  public Minus(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.MINUS);
  }
}
