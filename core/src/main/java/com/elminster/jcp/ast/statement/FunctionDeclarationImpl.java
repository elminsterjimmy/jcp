package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.expression.Identifier;
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
  public String getDataType() {
    return returnType.getName();
  }

  @Override
  public String getName() {
    return "FUNCTION_DECLARATION";
  }
}
