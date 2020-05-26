package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;

public class FunctionCallExpression extends AbstractExpression {

  private String id;
  private Expression[] argurements;

  public FunctionCallExpression(String id, Expression[] argurements) {
    this.id = id;
    this.argurements = argurements;
  }

  @Override
  public String getName() {
    return "FUNCALL";
  }

  /**
   * Gets id.
   *
   * @return value of id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets argurements.
   *
   * @return value of argurements
   */
  public Expression[] getArgurements() {
    return argurements;
  }
}
