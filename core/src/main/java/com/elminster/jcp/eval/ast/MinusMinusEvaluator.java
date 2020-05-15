package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.UnaryExpression;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class MinusMinusEvaluator extends UnaryEvaluator {
  public MinusMinusEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    UnaryExpression unaryExpression = (UnaryExpression) astNode;
    Expression expression = unaryExpression.getExpress();
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
    FlowData data = evaluable.eval(evalContext);
    switch (data.getDataType()) {
      case INT:
        Integer i = (Integer) data.get();
        data.set(--i);
        break;
      default:
        throw new UnsupportedOperationException();
    }
    return data;
  }
}
