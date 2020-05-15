package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;

public class VariableExpression extends AbstractExpression {

  private String id;

  public VariableExpression(String id) {
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
  public String getId() {
    return id;
  }
}
