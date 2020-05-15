package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

public class FunctionDeclarationImpl extends BlockImpl implements FunctionDeclaration {

  private String id;
  private DataType returnType;
  private FlowData[] parameterDefines;

  @Override
  public FlowData[] getParameterDefines() {
    return parameterDefines;
  }

  @Override
  public String getId() {
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
