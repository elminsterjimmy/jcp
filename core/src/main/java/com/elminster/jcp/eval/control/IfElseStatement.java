package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Statement;

public class IfElseStatement extends ControlStatement {

  private Statement ifStatement;
  private Statement elseStatement;
  private Expression condition;

  public IfElseStatement(Statement ifStatement, Expression condition) {
    this(ifStatement, null, condition);
  }

  public IfElseStatement(Statement ifStatement, Statement elseStatement, Expression condition) {
    this.ifStatement = ifStatement;
    this.elseStatement = elseStatement;
    this.condition = condition;
  }

  @Override
  public String getName() {
    return "IFELSE";
  }

  /**
   * Gets ifStatement.
   *
   * @return value of ifStatement
   */
  public Statement getIfStatement() {
    return ifStatement;
  }

  /**
   * Gets elseStatement.
   *
   * @return value of elseStatement
   */
  public Statement getElseStatement() {
    return elseStatement;
  }

  /**
   * Gets condition.
   *
   * @return value of condition
   */
  public Expression getCondition() {
    return condition;
  }
}
