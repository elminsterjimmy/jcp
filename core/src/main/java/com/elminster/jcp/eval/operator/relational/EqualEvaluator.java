package com.elminster.jcp.eval.operator.relational;

import com.elminster.jcp.ast.Node;

public class EqualEvaluator extends CompareEvaluator {

  public EqualEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) == 0;
  }
}