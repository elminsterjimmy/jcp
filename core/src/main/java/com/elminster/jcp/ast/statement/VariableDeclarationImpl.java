package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.Expression;

public class VariableDeclarationImpl extends AbstractStatement implements VariableDeclaration {

  private String id;
  private Expression initExpress;
  private DataType dataType;

  public VariableDeclarationImpl(String id, DataType dataType) {
    this(id, dataType, null);
  }

  public VariableDeclarationImpl(String id, DataType dataType, Expression initExpress) {
    this.id = id;
    this.dataType = dataType;
    this.initExpress = initExpress;
  }

  @Override
  public String getId() {
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
  public String getName() {
    return "VARIABLE_DECLARATION";
  }
}
