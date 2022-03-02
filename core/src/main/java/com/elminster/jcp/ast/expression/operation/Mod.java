package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.ArithmeticOperator;

public class Mod extends ArithmeticExpression {

  public Mod(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.MOD);
  }
}
