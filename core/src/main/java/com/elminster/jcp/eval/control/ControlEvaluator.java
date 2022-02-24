package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.ExpressionEvaluator;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

abstract public class ControlEvaluator extends ExpressionEvaluator {

  public ControlEvaluator(Node astNode) {
    super(astNode);
  }

  protected boolean checkCondition(Expression expression, EvalContext context) {
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
    Data<Boolean> condition = evaluable.eval(context);
    return condition.get();
  }
}
