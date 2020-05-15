package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;

abstract public class CompareEvaluator extends AbstractBinaryEvaluator {

  public CompareEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand) {
    Comparable leftValue = (Comparable) leftOperand.get();
    Comparable rightValue = (Comparable) rightOperand.get();
    BooleanFlowData result = new BooleanFlowData(compare(leftValue, rightValue));
    return result;
  }

  protected abstract boolean compare(Comparable leftValue, Comparable rightValue);
}
