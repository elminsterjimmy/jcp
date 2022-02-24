package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.DataType;

public class FunctionDeclarationImpl extends BlockImpl implements FunctionDeclaration {

  private Identifier id;
  private DataType returnType;
  private ParameterDef[] parameterDefines;

  @Override
  public ParameterDef[] getParameterDefines() {
    return parameterDefines;
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    return returnType;
  }

  @Override
  public String getName() {
    return "FUNCTION_DECLARATION";
  }
}
