package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.EvalContext;

public class ConstantExpression extends AbstractExpression {

  private FlowData constData;

  public ConstantExpression(FlowData constData) {
    this.constData = constData;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    return constData;
  }

  @Override
  public String getName() {
    return "CONSTANT_EXPRESSION";
  }
}
