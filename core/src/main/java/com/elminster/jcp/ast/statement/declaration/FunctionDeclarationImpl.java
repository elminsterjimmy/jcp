package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.Modulable;

public class FunctionDeclarationImpl extends BlockImpl implements FunctionDeclaration {

  private final Identifier id;
  private final DataType returnType;
  private final ParameterDef[] parameterDefines;
  private final String moduleName;

  public FunctionDeclarationImpl(Identifier id,
                                 DataType returnType,
                                 ParameterDef[] parameterDefines,
                                 Statement... statements) {
    this(id, Modulable.DEFAULT_MODULE, returnType, parameterDefines, statements);
  }

  public FunctionDeclarationImpl(Identifier id,
                                 String moduleName,
                                 DataType returnType,
                                 ParameterDef[] parameterDefines,
                                 Statement... statements) {
    super(statements);
    this.id = id;
    this.moduleName = moduleName;
    this.returnType = returnType;
    this.parameterDefines = parameterDefines;
  }

  public FunctionDeclarationImpl(Identifier id,
                                 String moduleName,
                                 DataType returnType,
                                 ParameterDef[] parameterDefines,
                                 Block block) {
    super(block.getBody().toArray(new Statement[block.getBody().size()]));
    this.id = id;
    this.returnType = returnType;
    this.moduleName = moduleName;
    this.parameterDefines = parameterDefines;
  }

  public FunctionDeclarationImpl(Identifier id,
                                 DataType returnType,
                                 ParameterDef[] parameterDefines,
                                 Block block) {
    this(id, Modulable.DEFAULT_MODULE, returnType, parameterDefines, block);
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

  public String getModule() {
    return moduleName;
  }

  @Override
  public String getName() {
    return "FUNCTION_DECLARATION";
  }
}
