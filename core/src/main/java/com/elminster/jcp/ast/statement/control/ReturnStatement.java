package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.ExpressionStatement;

public class ReturnStatement extends ExpressionStatement {

  public ReturnStatement(Expression expression) {
    super(expression);
  }

  @Override
  public String getName() {
    return "RETURN";
  }
}
