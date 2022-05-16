package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.ArithmeticOperator;

public class Plus extends ArithmeticExpression {

  public Plus(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.PLUS);
  }

  public static Plus of(Expression leftOperand, Expression rightOperand) {
    return new Plus(leftOperand, rightOperand);
  }
}
