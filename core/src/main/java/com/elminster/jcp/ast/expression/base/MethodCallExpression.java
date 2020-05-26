package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.eval.data.Data;

public class MethodCallExpression extends AbstractExpression {

  private Data data;
  private String methodName;
  private Expression[] argurements;

  public MethodCallExpression(Data data, String methodName, Expression[] argurements) {
    this.data = data;
    this.methodName = methodName;
    this.argurements = argurements;
  }

  @Override
  public String getName() {
    return "METHOD_CALL";
  }

  /**
   * Gets data.
   *
   * @return value of data
   */
  public Data getData() {
    return data;
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
  public Expression[] getArgurements() {
    return argurements;
  }
}
