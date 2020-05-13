package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

abstract public class ArithmeticExpression extends AbstractBinaryExpression {

  public ArithmeticExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    super(leftOperand, rightOperand, operator);
  }
}
