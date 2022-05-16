package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.AbstractStatement;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.Modulable;

public class VariableDeclarationImpl extends AbstractStatement implements VariableDeclaration {

  private final Identifier id;
  private final Expression initExpress;
  private final DataType dataType;
  private final String moduleName;

  public VariableDeclarationImpl(Identifier id,
                                 DataType dataType) {
    this(id, Modulable.DEFAULT_MODULE, dataType, null);
  }

  public VariableDeclarationImpl(Identifier id,
                                 DataType dataType,
                                 Expression initExpress) {
    this(id, Modulable.DEFAULT_MODULE, dataType, initExpress);
  }

  public VariableDeclarationImpl(Identifier id,
                                 String moduleName,
                                 DataType dataType,
                                 Expression initExpress) {
    this.id = id;
    this.dataType = dataType;
    this.initExpress = initExpress;
    this.moduleName = moduleName;
  }

  public VariableDeclarationImpl(String id,
                                 DataType dataType) {
    this(id, dataType, null);
  }

  public VariableDeclarationImpl(String id,
                                 DataType dataType, Expression initExpress) {
    this(Identifier.fromName(id), Modulable.DEFAULT_MODULE, dataType, initExpress);
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public Expression getInit() {
    return initExpress;
  }

  @Override
  public String getModule() {
    return moduleName;
  }

  @Override
  public String getName() {
    return "VARIABLE_DECLARATION";
  }
}
