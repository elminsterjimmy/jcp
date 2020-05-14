package com.elminster.jcp.module;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;

import java.util.Collections;
import java.util.List;

abstract public class AbstractModuleFunction implements ModuleFunction {

  protected FlowData[] parameters;

  @Override
  public void setParameters(FlowData... parameters) {
    FlowData[] parameterDefs = getParameterDefs();
    if (parameters.length != parameterDefs.length) {
      // TODO
      UndeclaredException.throwUndeclaredFunctionException(getId());
    }
    int i = 0;
    for (FlowData parameter : parameters) {
      FlowData def = parameterDefs[i++];
      if (!parameter.getDataType().isCastableTo(def.getDataType())) {
        throw new CannotCastException(parameter.getDataType(), def.getDataType());
      }
      this.parameters = parameters;
    }
  }

  @Override
  public List<Statement> getBody() {
    return Collections.emptyList();
  }

  @Override
  public void addStatement(Statement statement) {
  }

  @Override
  public FlowData[] getParameters() {
    return this.parameters;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    return doFunction(getParameters());
  }

  protected abstract FlowData doFunction(FlowData[] parameters);
}
