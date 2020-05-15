package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;

public class NotEqualEvaluator extends CompareEvaluator {

  public NotEqualEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    return leftValue.compareTo(rightValue) != 0;
  }
}