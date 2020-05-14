package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.eval.context.EvalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IfElseStatement extends ControlStatement {

  private static final Logger logger = LoggerFactory.getLogger(IfElseStatement.class);

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
    return "IF-ELSE";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    if (checkCondition(condition, evalContext)) {
      logger.debug("[{}] condition [TRUE]", this.getName());
      ifStatement.eval(evalContext);
    } else {
      logger.debug("[{}] condition [FALSE]", this.getName());
      if (null != elseStatement) {
        elseStatement.eval(evalContext);
      }
    }
    return AnyFlowData.EMPTY;
  }
}
