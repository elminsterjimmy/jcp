package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.BinaryOperator;

abstract public class CompareExpression extends AbstractBinaryExpression {

  public CompareExpression(Expression leftOperand, Expression rightOperand, BinaryOperator operator) {
    super(leftOperand, rightOperand, operator);
  }

  @Override
  protected FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand) {
    Comparable leftValue = (Comparable) leftOperand.get();
    Comparable rightValue = (Comparable) rightOperand.get();
    BooleanFlowData result = new BooleanFlowData(compare(leftValue, rightValue));
    return result;
  }

  protected abstract boolean compare(Comparable leftValue, Comparable rightValue);
}
