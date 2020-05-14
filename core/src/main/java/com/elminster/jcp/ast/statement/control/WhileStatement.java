package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.ast.statement.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhileStatement extends LoopStatement {

  private static final Logger logger = LoggerFactory.getLogger(WhileStatement.class);

  public WhileStatement(Expression conditionExpression, Block body) {
    super(conditionExpression, body);
  }

  @Override
  public String getName() {
    return "WHILE";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    super.addToParent(evalContext);
    while (shouldContinue(evalContext)) {
      super.updateLoopContext(evalContext);
      logger.debug("[WHILE] loop count [{}]", ctx.getLoopTime());
      body.eval(evalContext);
    }
    super.clearLoopContext(evalContext);
    return AnyFlowData.EMPTY;
  }

  private boolean shouldContinue(EvalContext evalContext) {
    boolean condition = ((FlowData<Boolean>)conditionExpression.eval(evalContext)).get();
    logger.debug("[WHILE] condition: [{}], break [{}]", condition, isBreak);
    return condition && !isBreak;
  }
}
