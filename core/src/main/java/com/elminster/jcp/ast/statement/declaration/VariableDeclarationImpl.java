package com.elminster.jcp.ast.statement.declaration;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.statement.AbstractStatement;
import com.elminster.jcp.eval.data.DataType;

public class VariableDeclarationImpl extends AbstractStatement implements VariableDeclaration {

  private Identifier id;
  private Expression initExpress;
  private DataType dataType;

  public VariableDeclarationImpl(Identifier id, DataType dataType) {
    this(id, dataType, null);
  }

  public VariableDeclarationImpl(Identifier id, DataType dataType, Expression initExpress) {
    this.id = id;
    this.dataType = dataType;
    this.initExpress = initExpress;
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
  public String getName() {
    return "VARIABLE_DECLARATION";
  }
}
