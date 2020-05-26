package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.statement.Block;

public class WhileStatement extends LoopStatement {

  public WhileStatement(Expression conditionExpression, Block body) {
    super(conditionExpression, body);
  }

  @Override
  public String getName() {
    return "WHILE";
  }
}
