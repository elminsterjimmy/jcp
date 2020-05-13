package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.AbstractExpression;
import com.elminster.jcp.ast.expression.BinaryExpression;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;
import com.elminster.jcp.eval.context.EvalContext;


abstract public class AbstractBinaryExpression extends AbstractExpression implements BinaryExpression {

  protected Expression leftOperand;
  protected Expression rightOperand;
  protected BinaryOperator operator;

  public AbstractBinaryExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
    this.operator = operator;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData left = leftOperand.eval(evalContext);
    FlowData right = rightOperand.eval(evalContext);
    FlowData result = doBinaryOp(left, right);
    return result;
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

  protected abstract FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand);

  @Override
  public String getName() {
    return this.getOperator().getName();
  }
}
