package com.elminster.jcp.ast;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.Function;

import java.util.HashMap;
import java.util.Map;

public class AbstractFunction extends BlockImpl implements Function {

  private final String id;
  private final FlowData[] parameters;
  private final DataType resultDataType;

  public AbstractFunction(FunctionDefinition functionDefinition) {
    this.id = functionDefinition.id;
    this.parameters = functionDefinition.parameters;
    this.resultDataType = functionDefinition.returnType;
  }

  public AbstractFunction(String id, FlowData[] parameters, DataType resultDataType) {
    this.id = id;
    this.parameters = parameters;
    this.resultDataType = resultDataType;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public FlowData[] getParameters() {
    return parameters;
  }

  @Override
  public void setParameters(FlowData... values) {
    if (values.length != parameters.length) {
      // TODO
      UndeclaredException.throwUndeclaredFunctionException(id);
    }
    int i = 0;
    for (FlowData value : values) {
      FlowData parameter =this.parameters[i++];
      if (!value.getDataType().isCastableTo(parameter.getDataType())) {
        throw new CannotCastException(value.getDataType(), parameter.getDataType());
      }
      parameter.set(value.get());
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
