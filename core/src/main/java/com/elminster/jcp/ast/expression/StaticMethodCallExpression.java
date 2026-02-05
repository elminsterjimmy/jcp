package com.elminster.jcp.ast.expression;

import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;

/**
 * AST node for static method calls: Type.method(args)
 */
public class StaticMethodCallExpression extends AbstractExpression {

  private final Identifier typeName;
  private final String methodName;
  private final Expression[] arguments;

  public StaticMethodCallExpression(Identifier typeName, String methodName, Expression... arguments) {
    this.typeName = typeName;
    this.methodName = methodName;
    this.arguments = arguments;
  }

  public StaticMethodCallExpression(String typeName, String methodName, Expression... arguments) {
    this(Identifier.fromName(typeName), methodName, arguments);
  }

  public Identifier getTypeName() {
    return typeName;
  }

  public String getMethodName() {
    return methodName;
  }

  public Expression[] getArguments() {
    return arguments;
  }

  @Override
  public String getName() {
    return "STATIC_METHOD_CALL";
  }
}
