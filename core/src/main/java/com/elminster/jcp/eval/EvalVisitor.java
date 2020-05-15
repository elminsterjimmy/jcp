package com.elminster.jcp.eval;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.vistor.AstVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class EvalVisitor implements AstVisitor {

  private EvalContext context;

  public EvalVisitor(EvalContext context) {
    this.context = context;
  }

  public EvalContext getContext() {
    return context;
  }

  @Override
  public void visit(Node node) {
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(node);
    evaluable.eval(context);
  }
}
