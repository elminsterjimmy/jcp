package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;

public class PlusEvaluator extends ArithmeticEvaluator {

  public PlusEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    // int
    if (leftOperand instanceof IntegerData) {
      Integer leftValue = ((IntegerData) leftOperand).get();
      if (rightOperand instanceof IntegerData) {
        // + int
        Integer rightValue = ((IntegerData) rightOperand).get();
        return new IntegerData(leftValue + rightValue);
      } else if (rightOperand instanceof StringData) {
        // + string
        String rightValue = ((StringData)rightOperand).get();
        return new StringData(leftValue + rightValue);
      }
    }
    // string
    if (leftOperand instanceof StringData) {
      String leftValue = ((StringData)leftOperand).get();
      if (rightOperand instanceof IntegerData) {
        // + int
        Integer rightValue = ((IntegerData) rightOperand).get();
        return new StringData(leftValue + rightValue);
      } else if (rightOperand instanceof StringData) {
        // + string
        String rightValue = ((StringData)rightOperand).get();
        return new StringData(leftValue + rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
