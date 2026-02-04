package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import com.elminster.jcp.eval.data.DoubleData;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;
import com.elminster.jcp.util.DataTypeUtils;

public class PlusEvaluator extends ArithmeticEvaluator {

    public PlusEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
        DataType leftType = leftOperand.getDataType();
        DataType rightType = rightOperand.getDataType();

        // String concatenation - check first since strings can concatenate with anything
        if (leftType == SystemDataType.STRING) {
            String leftValue = (String) leftOperand.get();
            String rightValue = String.valueOf(rightOperand.get());
            return new StringData(leftValue + rightValue);
        }

        // Handle DOUBLE operations (including int-to-double promotion)
        if (leftType == SystemDataType.DOUBLE || rightType == SystemDataType.DOUBLE) {
            double left = DataTypeUtils.toDoubleValue(leftOperand);
            double right = DataTypeUtils.toDoubleValue(rightOperand);
            return new DoubleData(left + right);
        }

        // int + int
        if (leftType == SystemDataType.INT) {
            Integer leftValue = (Integer) leftOperand.get();
            if (rightType == SystemDataType.INT || rightType.isCastableTo(SystemDataType.INT)) {
                Integer rightValue = (Integer) rightOperand.get();
                return new IntegerData(leftValue + rightValue);
            }
        }

        throw new UnsupportedOperationException(String.format("unable to plus %s with %s", leftType, rightType));
    }
}
