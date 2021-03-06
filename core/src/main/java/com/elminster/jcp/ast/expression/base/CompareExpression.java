package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

abstract public class CompareExpression extends AbstractBinaryExpression {

  public CompareExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    super(leftOperand, rightOperand, operator);
  }
}
