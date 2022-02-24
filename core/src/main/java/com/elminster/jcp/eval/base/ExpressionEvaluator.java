package com.elminster.jcp.eval.base;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class ExpressionEvaluator extends AbstractAstEvaluator {
  public ExpressionEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    Expression expression = (Expression) astNode;
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
    return evaluable.eval(evalContext);
  }
}
