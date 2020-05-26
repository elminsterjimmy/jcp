package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.statement.control.WhileStatement;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhileEvaluator extends LoopEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(WhileEvaluator.class);

  public WhileEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    WhileStatement whileStatement = (WhileStatement) astNode;
    Statement body = whileStatement.getBody();
    super.addToParent(evalContext);
    while (shouldContinue(evalContext)) {
      super.updateLoopContext(evalContext);
      logger.debug("[WHILE] loop count [{}]", ctx.getLoopTime());
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(body);
      evaluable.eval(evalContext);
    }
    super.clearLoopContext(evalContext);
    return AnyFlowData.EMPTY;
  }

  private boolean shouldContinue(EvalContext evalContext) {
    WhileStatement whileStatement = (WhileStatement) astNode;
    Expression conditionExpression = whileStatement.getConditionExpression();
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(conditionExpression);
    boolean condition = ((FlowData<Boolean>)evaluable.eval(evalContext)).get();
    logger.debug("[WHILE] condition: [{}], break [{}]", condition, isBreak);
    return condition && !isBreak;
  }
}
