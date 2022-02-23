package com.elminster.jcp.module;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;

import java.util.Collections;
import java.util.List;

abstract public class AbstractModuleFunction implements ModuleFunction {

  protected Data[] parameters;

  @Override
  public void setParameters(Data... parameters) {
    this.parameters = parameters;
  }

  @Override
  public List<Statement> getBody() {
    return Collections.emptyList();
  }

  @Override
  public void addStatement(Statement statement) {
  }

  @Override
  public Data[] getParameters() {
    return this.parameters;
  }

  @Override
  public Data eval(EvalContext evalContext) {
    return doFunction(getParameters());
  }

  protected abstract Data doFunction(Data[] parameters);
}
