package com.elminster.jcp.ast.expression.operation;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Identifier;

public class IdentifierExpression extends AbstractExpression implements Identifier {

  private String id;

  public IdentifierExpression(String id) {
    this.id = id;
  }

  public static IdentifierExpression of(String id) {
    return new IdentifierExpression(id);
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