package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;

public class VariableDeclarationImpl extends AbstractStatement implements VariableDeclaration {

  private Identifier id;
  private Expression initExpress;
  private String dataType;

  public VariableDeclarationImpl(Identifier id, String dataType) {
    this(id, dataType, null);
  }

  public VariableDeclarationImpl(Identifier id, String dataType, Expression initExpress) {
    this.id = id;
    this.dataType = dataType;
    this.initExpress = initExpress;
  }

  @Override
  public Identifier getId() {
    return id;
  }

  @Override
  public String getDataType() {
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
