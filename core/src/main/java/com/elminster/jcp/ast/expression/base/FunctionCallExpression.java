package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.module.Modulable;

public class FunctionCallExpression extends AbstractExpression implements Modulable {

  private String module;
  private Identifier id;
  private Expression[] arguments;

  public FunctionCallExpression(Identifier id, Expression... arguments) {
    this(id, Modulable.DEFAULT_MODULE, arguments);
  }

  public FunctionCallExpression(Identifier id, String module, Expression... arguments) {
    this.id = id;
    this.module = module;
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

  public String getModule() {
    return module;
  }
}
