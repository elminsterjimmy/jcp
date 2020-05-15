package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;

abstract public class AbstractAstEvaluator implements AstEvaluator {

  protected Node astNode;

  public AbstractAstEvaluator(Node astNode) {
    this.astNode = astNode;
  }

  @Override
  public Node getAstNode() {
    return astNode;
  }
}
