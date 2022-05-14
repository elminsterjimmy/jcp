package com.elminster.jcp.eval;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.vistor.AstVisitor;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvalVisitor implements AstVisitor {

  private static final Logger logger = LoggerFactory.getLogger(EvalVisitor.class);

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
    Data eval = evaluable.eval(context);
    afterEval(eval);
  }

  protected void afterEval(Data eval) {
    if (logger.isDebugEnabled()) {
      logger.debug(eval.toString());
    }
  }
}
