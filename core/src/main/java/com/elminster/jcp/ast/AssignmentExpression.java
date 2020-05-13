package com.elminster.jcp.ast;

import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.AbstractExpression;
import com.elminster.jcp.eval.context.EvalContext;

/**
 * '=' | '*=' | '/=' | '%=' | '+=' | '-=' | '<<=' | '>>=' | '&=' | '^=' | '|='
 */
public class AssignmentExpression extends AbstractExpression {

  private String name;
  private Object value;

  @Override
  public String getName() {
    return "ASSIGNMENT";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData variable = evalContext.getVariable(name);
    variable.set(value);
    return AnyFlowData.EMPTY;
  }
}
