package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.IntegerFlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.ArithmeticOperator;

public class Mod extends ArithmeticExpression {

  public Mod(Expression leftOperand, Expression rightOperand) {
    super(leftOperand, rightOperand, ArithmeticOperator.MOD);
  }

  @Override
  protected FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand) {
    // integer
    if (leftOperand instanceof IntegerFlowData) {
      // * integer
      Integer leftValue = ((IntegerFlowData) leftOperand).get();
      if (rightOperand instanceof IntegerFlowData) {
        Integer rightValue = ((IntegerFlowData) rightOperand).get();
        return new IntegerFlowData(leftValue % rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
