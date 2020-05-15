package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.IntegerFlowData;
import com.elminster.jcp.ast.data.StringFlowData;

public class PlusEvaluator extends ArithmeticEvaluator {

  public PlusEvaluator(Node astNode) {
    super(astNode);
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
