package com.elminster.jcp.eval.control;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.control.RepeatStatement;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepeatEvaluator extends WhileEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(RepeatEvaluator.class);

  public RepeatEvaluator(Node astNode) {
    super(astNode);
  }

  protected boolean shouldContinue(EvalContext evalContext) {
    RepeatStatement repeatStatement = (RepeatStatement) astNode;
    Expression conditionExpression = repeatStatement.getConditionExpression();
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(conditionExpression);
    Integer count = ((Data<Integer>)evaluable.eval(evalContext)).get();
    logger.debug("[REPEAT] count: [{}], break [{}]", count, isBreak);
    return ctx.getLoopTime() < count && !isBreak;
  }
}
