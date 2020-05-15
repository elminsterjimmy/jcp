package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.ArithmeticOperator;

public class Divide extends ArithmeticExpression {

  public Divide(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.DIVIDE);
  }
}
