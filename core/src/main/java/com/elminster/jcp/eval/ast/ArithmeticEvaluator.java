package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;

abstract public class ArithmeticEvaluator extends AbstractBinaryEvaluator {
  public ArithmeticEvaluator(Node astNode) {
    super(astNode);
  }
}
