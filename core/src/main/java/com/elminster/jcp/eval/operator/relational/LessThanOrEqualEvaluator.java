package com.elminster.jcp.eval.operator.relational;

import com.elminster.jcp.ast.Node;

public class LessThanOrEqualEvaluator extends CompareEvaluator {

  public LessThanOrEqualEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) <= 0;
  }
}
