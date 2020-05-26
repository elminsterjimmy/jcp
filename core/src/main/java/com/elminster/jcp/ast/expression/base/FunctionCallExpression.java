package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.Identifier;

public class FunctionCallExpression extends AbstractExpression {

  private Identifier id;
  private Expression[] arguments;

  public FunctionCallExpression(Identifier id, Expression... arguments) {
    this.id = id;
    this.arguments = arguments;
  }

  @Override
  public String getName() {
    return "FUN_CALL";
  }

  /**
   * Gets id.
   *
   * @return value of id
   */
  public Identifier getId() {
    return id;
  }

  /**
   * Gets argurements.
   *
   * @return value of argurements
   */
  public Expression[] getArguments() {
    return arguments;
  }
}
