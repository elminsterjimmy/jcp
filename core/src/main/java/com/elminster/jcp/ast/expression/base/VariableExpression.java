package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Identifier;

public class VariableExpression extends AbstractExpression {

  private Identifier id;

  public VariableExpression(Identifier id) {
    this.id = id;
  }

  public static VariableExpression of(Identifier id) {
    return new VariableExpression(id);
  }

  public static VariableExpression of(String name) {
    return new VariableExpression(Identifier.fromName(name));
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
