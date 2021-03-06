package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.IntegerData;

public class MultiEvaluator extends ArithmeticEvaluator {

  public MultiEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    // integer
    if (leftOperand instanceof IntegerData) {
      // * integer
      Integer leftValue = ((IntegerData) leftOperand).get();
      if (rightOperand instanceof IntegerData) {
        Integer rightValue = ((IntegerData) rightOperand).get();
        return new IntegerData(leftValue * rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
