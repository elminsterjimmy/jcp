package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.module.Modulable;

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
   * Gets arguments.
   *
   * @return value of arguments
   */
  public Expression[] getArguments() {
    return arguments;
  }
}
