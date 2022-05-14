package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.eval.data.Data;

public class MethodCallExpression extends AbstractExpression {

  private Expression expression;
  private String methodName;
  private Expression[] arguments;

  public MethodCallExpression(Expression expression, String methodName, Expression... arguments) {
    this.expression = expression;
    this.methodName = methodName;
    this.arguments = arguments;
  }

  @Override
  public String getName() {
    return "METHOD_CALL";
  }

  public Expression getExpression() {
    return expression;
  }

  /**
   * Gets methodName.
   *
   * @return value of methodName
   */
  public String getMethodName() {
    return methodName;
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
