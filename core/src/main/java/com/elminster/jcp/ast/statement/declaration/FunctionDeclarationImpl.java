package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
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
