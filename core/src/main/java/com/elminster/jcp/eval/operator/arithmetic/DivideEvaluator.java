package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.operator.arithmetic.ArithmeticEvaluator;

public class DivideEvaluator extends ArithmeticEvaluator {
  
  public DivideEvaluator(Node astNode) {
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
        return new IntegerData(leftValue / rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
