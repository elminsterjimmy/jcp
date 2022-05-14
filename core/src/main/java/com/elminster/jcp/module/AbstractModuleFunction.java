package com.elminster.jcp.module;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.util.FunctionUtils;

import java.util.Collections;
import java.util.List;

abstract public class AbstractModuleFunction implements ModuleFunction {

  protected Data[] arguments;

  @Override
  public void setArguments(Data... arguments) {
    this.arguments = arguments;
  }

  @Override
  public List<Statement> getBody() {
    return Collections.emptyList();
  }

  @Override
  public Block addStatement(Statement statement) {
    // TODO
    return null;
  }

  @Override
  public Data[] getArguments() {
    return this.arguments;
  }

  @Override
  public Data eval(EvalContext evalContext) {
    return doFunction(getArguments(), evalContext);
  }

  @Override
  public String getFullName() {
    // TODO module support
//    return this.getModule().concat("!").concat(FunctionUtils.generateFunctionFullName(getId(), getParameterDefs()));
    return FunctionUtils.generateFunctionFullName(getId(), getParameterDefs());
  }

  protected abstract Data doFunction(Data[] arguments, EvalContext evalContext);
}
