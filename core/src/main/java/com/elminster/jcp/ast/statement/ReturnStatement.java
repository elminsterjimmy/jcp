package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Expression;

public class ReturnStatement extends ExpressionStatement {

  public ReturnStatement(Expression expression) {
    super(expression);
  }

  @Override
  public String getName() {
    return "RETURN";
  }
}
