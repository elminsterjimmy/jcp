package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;

abstract public class CompareEvaluator extends AbstractBinaryEvaluator {

  public CompareEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected Data doBinaryOp(Data leftOperand, Data rightOperand) {
    Comparable leftValue = (Comparable) leftOperand.get();
    Comparable rightValue = (Comparable) rightOperand.get();
    BooleanData result = new BooleanData(compare(leftValue, rightValue));
    return result;
  }

  protected abstract boolean compare(Comparable leftValue, Comparable rightValue);
}
