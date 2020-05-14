package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class VariableExpression extends AbstractExpression {

  private String id;

  public VariableExpression(String id) {
    this.id = id;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData variable = evalContext.getVariable(id);
    if (null == variable) {
      UndeclaredException.throwUndeclaredVariableException(id);
    }
    return variable;
  }

  @Override
  public String getName() {
    return "VARIABLE";
  }
}
