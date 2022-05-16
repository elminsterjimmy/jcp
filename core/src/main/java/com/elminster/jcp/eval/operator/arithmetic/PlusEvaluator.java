package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.IntegerData;
import com.elminster.jcp.eval.data.StringData;

public class PlusEvaluator extends ArithmeticEvaluator {

    public PlusEvaluator(Node astNode) {
        super(astNode);
    }

    @Override
    protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
        // int
        if (DataType.SystemDataType.INT == leftOperand.getDataType()) {
            Integer leftValue = ((Integer) leftOperand.get());
            if (DataType.SystemDataType.INT == rightOperand.getDataType()
                    || rightOperand.getDataType().isCastableTo(DataType.SystemDataType.INT)) {
                // + int
                Integer rightValue = ((Integer) rightOperand.get());
                return new IntegerData(leftValue + rightValue);
            }
        }
        // string
        if (DataType.SystemDataType.STRING == leftOperand.getDataType()) {
            String leftValue = ((StringData) leftOperand).get();
            if (DataType.SystemDataType.INT == rightOperand.getDataType()) {
                // + int
                Integer rightValue = ((Integer) rightOperand.get());
                return new StringData(leftValue + rightValue);
            } else if (DataType.SystemDataType.STRING == leftOperand.getDataType()
                    || rightOperand.getDataType().isCastableTo(DataType.SystemDataType.STRING)) {
                // + string
                String rightValue = String.valueOf(rightOperand.get());
                return new StringData(leftValue + rightValue);
            }
        }
        // TODO: message
        throw new UnsupportedOperationException(String.format("unable to plus %s with %s", leftOperand.getDataType(),
                rightOperand.getDataType()));
    }
}
