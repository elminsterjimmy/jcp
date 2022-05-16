package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.ExpressionStatement;

import javax.annotation.Nullable;

public class ReturnStatement extends ExpressionStatement {

  public ReturnStatement(@Nullable Expression expression) {
    super(expression);
  }

  @Override
  public String getName() {
    return "RETURN";
  }
}
