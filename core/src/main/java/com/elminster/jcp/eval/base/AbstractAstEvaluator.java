package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Locatable;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.SourceLocation;

abstract public class AbstractAstEvaluator implements AstEvaluator {

  protected Node astNode;

  public AbstractAstEvaluator(Node astNode) {
    this.astNode = astNode;
  }

  @Override
  public Node getAstNode() {
    return astNode;
  }

  /**
   * Returns the source location of the current AST node being evaluated.
   *
   * @return source location, or null if the node doesn't have location info
   */
  protected SourceLocation getSourceLocation() {
    return astNode instanceof Locatable ? ((Locatable) astNode).getLocation() : null;
  }
}
