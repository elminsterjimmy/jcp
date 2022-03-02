package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operation.operator.BinaryOperator;

abstract public class ArithmeticExpression extends BinaryExpressionImpl {

  public ArithmeticExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    super(leftOperand, rightOperand, operator);
  }
}
