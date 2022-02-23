package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

public class BinaryExpressionImpl extends AbstractExpression implements BinaryExpression {

  protected Expression leftOperand;
  protected Expression rightOperand;
  protected BinaryOperator operator;

  public BinaryExpressionImpl(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
    this.operator = operator;
  }

  @Override
  public Expression getLeft() {
    return leftOperand;
  }

  @Override
  public Expression getRight() {
    return rightOperand;
  }

  @Override
  public BinaryOperator getOperator() {
    return operator;
  }

  @Override
  public String getName() {
    return this.getOperator().getName();
  }
}
