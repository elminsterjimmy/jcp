package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType;

public class FunctionDeclarationImpl extends BlockImpl implements FunctionDeclaration {

  private Identifier id;
  private DataType returnType;
  private ParameterDef[] parameterDefines;

  public FunctionDeclarationImpl(Identifier id, DataType returnType, ParameterDef[] parameterDefines, Statement... statements) {
    super(statements);
    this.id = id;
    this.returnType = returnType;
    this.parameterDefines = parameterDefines;
  }

  public FunctionDeclarationImpl(Identifier id, DataType returnType, ParameterDef[] parameterDefines, Block block) {
    super(block.getBody().toArray(new Statement[block.getBody().size()]));
    this.id = id;
    this.returnType = returnType;
    this.parameterDefines = parameterDefines;
  }

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
