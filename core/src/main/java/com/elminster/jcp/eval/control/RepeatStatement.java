package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.Block;

public class RepeatStatement extends WhileStatement {

  public RepeatStatement(Expression conditionExpression, Block body) {
    super(conditionExpression, body);
  }

  @Override
  public String getName() {
    return "REPEAT";
  }
}
