package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.IntegerFlowData;

public class ModEvaluator extends ArithmeticEvaluator {
  
  public ModEvaluator(Node astNode) {
    super(astNode);
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
