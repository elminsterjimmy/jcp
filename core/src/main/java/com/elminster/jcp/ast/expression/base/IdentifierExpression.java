package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.expression.Identifier;

public class IdentifierExpression extends AbstractExpression implements Identifier {

  private String id;

  public IdentifierExpression(String id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return "IDENTIFIER";
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return getName() + " [" + getId() + "]";
  }
}