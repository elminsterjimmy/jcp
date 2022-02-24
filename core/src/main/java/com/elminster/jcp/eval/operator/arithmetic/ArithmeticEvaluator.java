package com.elminster.jcp.eval.operator.arithmetic;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.operator.AbstractBinaryEvaluator;

abstract public class ArithmeticEvaluator extends AbstractBinaryEvaluator {

  public ArithmeticEvaluator(Node astNode) {
    super(astNode);
  }
}
