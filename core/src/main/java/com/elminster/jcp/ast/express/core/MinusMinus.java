package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.PostfixOperator;
import com.elminster.jcp.eval.context.EvalContext;

public class MinusMinus extends AbstractUnaryExpression {

  public MinusMinus(Expression expression) {
    super(expression, PostfixOperator.MINUS_MINUS);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData data = expression.eval(evalContext);
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
