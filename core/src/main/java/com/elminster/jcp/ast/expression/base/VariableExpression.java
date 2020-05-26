package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.expression.Identifier;

public class VariableExpression extends AbstractExpression {

  private Identifier id;

  public VariableExpression(Identifier id) {
    this.id = id;
  }

  @Override
  public String getName() {
    return "VARIABLE";
  }

  /**
   * Gets id.
   *
   * @return value of id
   */
  public Identifier getId() {
    return id;
  }

  @Override
  public String toString() {
    return getName() + " [" + getId() + "]";
  }
}
