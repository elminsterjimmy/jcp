package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.IntegerFlowData;
import com.elminster.jcp.ast.data.StringFlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.ArithmeticOperator;

public class Plus extends ArithmeticExpression {

  public Plus(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.PLUS);
  }

  @Override
  protected FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand) {
    // int
    if (leftOperand instanceof IntegerFlowData) {
      Integer leftValue = ((IntegerFlowData) leftOperand).get();
      if (rightOperand instanceof IntegerFlowData) {
        // + int
        Integer rightValue = ((IntegerFlowData) rightOperand).get();
        return new IntegerFlowData(leftValue + rightValue);
      } else if (rightOperand instanceof StringFlowData) {
        // + string
        String rightValue = ((StringFlowData)rightOperand).get();
        return new StringFlowData(leftValue + rightValue);
      }
    }
    // string
    if (leftOperand instanceof StringFlowData) {
      String leftValue = ((StringFlowData)leftOperand).get();
      if (rightOperand instanceof IntegerFlowData) {
        // + int
        Integer rightValue = ((IntegerFlowData) rightOperand).get();
        return new StringFlowData(leftValue + rightValue);
      } else if (rightOperand instanceof StringFlowData) {
        // + string
        String rightValue = ((StringFlowData)rightOperand).get();
        return new StringFlowData(leftValue + rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
