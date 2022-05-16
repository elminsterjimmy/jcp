package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.IntegerData;

public class MultiEvaluator extends ArithmeticEvaluator {

  public MultiEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    // integer
    if (DataType.SystemDataType.INT == leftOperand.getDataType()) {
      // - integer
      Integer leftValue = ((Integer) leftOperand.get());
      if (DataType.SystemDataType.INT == rightOperand.getDataType()
              || rightOperand.getDataType().isCastableTo(DataType.SystemDataType.INT)) {
        Integer rightValue = ((Integer) rightOperand.get());
        return new IntegerData(leftValue * rightValue);
      }
    }
    // TODO: message
    throw new UnsupportedOperationException();
  }
}
