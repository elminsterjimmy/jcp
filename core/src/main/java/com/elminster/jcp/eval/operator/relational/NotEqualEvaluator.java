package com.elminster.jcp.eval.operator.relational;

import com.elminster.jcp.ast.Node;

public class NotEqualEvaluator extends CompareEvaluator {

  public NotEqualEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  protected boolean compare(Comparable leftValue, Comparable rightValue) {
    if (null == leftValue) {
      return null != rightValue;
    } else {
      return null == rightValue ? true : leftValue.compareTo(rightValue) != 0;
    }
  }
}