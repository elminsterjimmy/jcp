package com.elminster.jcp.ast.statement.function;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.util.FunctionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class AbstractFunction extends BlockImpl implements Function {

  private final Identifier id;
  private final ParameterDef[] parameterDefs;
  private Data[] parameters;
  private final DataType resultDataType;
  private final String fullName;

  public AbstractFunction(FunctionDef functionDef, Statement... body) {
    this(functionDef.id, functionDef.parameters, functionDef.returnType, body);
  }

  public AbstractFunction(Identifier id, ParameterDef[] parameterDefs, DataType resultDataType, Statement... body) {
    super(body);
    this.id = id;
    this.parameterDefs = parameterDefs;
    this.resultDataType = resultDataType;
    this.fullName = FunctionUtils.generateFunctionFullName(id, parameterDefs);
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
  public Data[] getArguments() {
    return parameters;
  }

  @Override
  public void setArguments(Data... arguments) {
    this.parameters = arguments;
  }

  @Override
  public DataType getResultDataType() {
    return resultDataType;
  }

  @Override
  public String getFullName() {
    return this.fullName;
  }

  @Override
  public String getName() {
    return "FUNCTION";
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
