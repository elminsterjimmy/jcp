package com.elminster.jcp.eval.operator.postfix;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;

abstract public class UnaryEvaluator extends AbstractAstEvaluator {
  public UnaryEvaluator(Node astNode) {
    super(astNode);
  }
}
