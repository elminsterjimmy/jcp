package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.AbstractExpression;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.context.EvalContext;

public class FunctionCallExpression extends AbstractExpression {

  private String id;
  private Expression[] argurements;

  public FunctionCallExpression(String id, Expression[] argurements) {
    this.id = id;
    this.argurements = argurements;
  }

  @Override
  public String getName() {
    return "FUNCTION_CALL";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    Function function = evalContext.getFunction(id);
    if (null == function) {
      UndeclaredException.throwUndeclaredFunctionException(id);
    }
    FlowData[] parameters = new FlowData[argurements.length];
    int i = 0;
    for (Expression arg : argurements) {
      parameters[i++] = arg.eval(evalContext);
    }
    function.setParameters(parameters);
    FlowData data = function.eval(evalContext);
    return data;
  }
}
