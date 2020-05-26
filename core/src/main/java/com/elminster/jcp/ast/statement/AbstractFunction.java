package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.Identifier;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.eval.ast.excpetion.CannotCastException;

public class AbstractFunction extends BlockImpl implements Function {

  private final Identifier id;
  private final ParameterDef[] parameterDefs;
  private Data[] parameters;
  private final DataType resultDataType;

  public AbstractFunction(FunctionDef functionDef, Statement... body) {
    this(functionDef.id, functionDef.parameters, functionDef.returnType, body);
  }

  public AbstractFunction(Identifier id, ParameterDef[] parameterDefs, DataType resultDataType, Statement... body) {
    super(body);
    this.id = id;
    this.parameterDefs = parameterDefs;
    this.resultDataType = resultDataType;
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public ParameterDef[] getParameterDefs() {
    return parameterDefs;
  }

  @Override
  public Data[] getParameters() {
    return parameters;
  }

  @Override
  public void setParameters(Data... parameters) {
    this.parameters = parameters;
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
