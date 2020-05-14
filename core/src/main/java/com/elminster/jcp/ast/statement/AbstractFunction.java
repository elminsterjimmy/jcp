package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.statement.FunctionDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.Function;

import java.util.HashMap;
import java.util.Map;

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
    return "ABSTRACT_FUNCTION";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    Map<String, FlowData> variables = evalContext.getVariables();
    try {
      evalContext.setVariables(new HashMap<>(parameters.length));
      // TODO: init function variables.
      FlowData result = doFunc(evalContext);
      if (!resultDataType.isCastableTo(result.getDataType())) {
        throw new CannotCastException(result.getDataType(), resultDataType);
      }
      return result;
    } finally {
      evalContext.setVariables(variables);
    }
  }

  protected FlowData doFunc(EvalContext evalContext) {
    return super.eval(evalContext);
  }
}
