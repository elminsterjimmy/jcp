package com.elminster.jcp.eval.operator.relational;

import com.elminster.jcp.ast.Node;

public class LessThanEvaluator extends CompareEvaluator {

  public LessThanEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) < 0;
  }
}
