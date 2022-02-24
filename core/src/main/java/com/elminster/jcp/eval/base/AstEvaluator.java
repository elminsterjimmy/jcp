package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.Evaluable;

public interface AstEvaluator extends Evaluable {

  Node getAstNode();
}
