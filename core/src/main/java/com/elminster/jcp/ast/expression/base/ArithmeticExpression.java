package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

abstract public class ArithmeticExpression extends BinaryExpressionImpl {

  public ArithmeticExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    super(leftOperand, rightOperand, operator);
  }
}
