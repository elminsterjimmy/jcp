package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DoubleData;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.util.DataTypeUtils;

public class DivideEvaluator extends ArithmeticEvaluator {

  public DivideEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    DataType leftType = leftOperand.getDataType();
    DataType rightType = rightOperand.getDataType();

    // Handle DOUBLE operations (including int-to-double promotion)
    // Note: double division by zero returns Infinity/NaN per IEEE 754
    if (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE) {
      double left = DataTypeUtils.toDoubleValue(leftOperand);
      double right = DataTypeUtils.toDoubleValue(rightOperand);
      return new DoubleData(left / right);
    }

    // int / int (throws ArithmeticException on divide by zero)
    if (leftType == SystemDataType.INT) {
      Integer leftValue = (Integer) leftOperand.get();
      if (rightType == SystemDataType.INT || rightType.isCastableTo(SystemDataType.INT)) {
        Integer rightValue = (Integer) rightOperand.get();
        return new IntegerData(leftValue / rightValue);
      }
    }

    throw new UnsupportedOperationException(String.format("unable to divide %s with %s", leftType, rightType));
  }
}
