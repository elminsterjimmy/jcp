package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;

public class AbstractFunction extends BlockImpl implements Function {

  private final String id;
  private final FlowData[] parameterDefs;
  private FlowData[] parameters;
  private final DataType resultDataType;

  public AbstractFunction(FunctionDef functionDef, Statement... body) {
    this(functionDef.id, functionDef.parameters, functionDef.returnType, body);
  }

  public AbstractFunction(String id, FlowData[] parameterDefs, DataType resultDataType, Statement... body) {
    super(body);
    this.id = id;
    this.parameterDefs = parameterDefs;
    this.resultDataType = resultDataType;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public FlowData[] getParameterDefs() {
    return parameterDefs;
  }

  @Override
  public FlowData[] getParameters() {
    return parameters;
  }

  @Override
  public void setParameters(FlowData... parameters) {
    if (parameters.length != parameterDefs.length) {
      // TODO
      UndeclaredException.throwUndeclaredFunctionException(id);
    }
    int i = 0;
    for (FlowData parameter : parameters) {
      FlowData def = this.parameterDefs[i++];
      if (!parameter.getDataType().isCastableTo(def.getDataType())) {
        throw new CannotCastException(parameter.getDataType(), def.getDataType());
      }
      this.parameters = parameters;
    }
  }

  @Override
  public DataType getResultDataType() {
    return resultDataType;
  }

  @Override
  public String getName() {
    return "FUNCTION";
  }
}
