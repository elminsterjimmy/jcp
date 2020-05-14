package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.AbstractStatement;
import com.elminster.jcp.eval.context.EvalContext;

abstract public class ControlStatement extends AbstractStatement {

  protected boolean checkCondition(Expression expression, EvalContext context) {
    FlowData<Boolean> condition = expression.eval(context);
    return condition.get();
  }
}
