package com.elminster.jcp.ast.statement.control;

import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.eval.context.LoopContextImpl;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The loop block.
 *
 * @author jgu
 * @version 1.0
 */
abstract public class LoopStatement extends ControlStatement {

  private static final Logger logger = LoggerFactory.getLogger(LoopStatement.class);

  protected Expression conditionExpression;
  protected Block body;
  protected LoopContext ctx;
  protected boolean isBreak;

  public LoopStatement(Expression conditionExpression, Block body) {
    this.conditionExpression = conditionExpression;
    this.body = body;
    this.ctx = new LoopContextImpl(this);
  }

  protected void updateLoopContext(EvalContext evalContext) {
    ctx.setBreakBlock(false);
    ctx.increaseLoopTime();
  }

  protected void clearLoopContext(EvalContext evalContext) {
    evalContext.setLoopContext(ctx.getParent());
  }

  protected void addToParent(EvalContext evalContext) {
    LoopContext parent = evalContext.getLoopContext();
    if (null != parent) {
      if (parent != ctx) {
        ctx.addToParent(parent);
      }
    } else {
      evalContext.setLoopContext(ctx);
    }
  }

  /**
   * Break the loop.
   * @param evalContext the flow context
   */
  public void doBreak(EvalContext evalContext) {
    logger.debug("break the loop [{}]", this);
    ctx.setBreakBlock(true);
    this.isBreak = true;
  }

  /**
   * Continue the loop.
   * @param evalContext the flow context
   */
  public void doContinue(EvalContext evalContext) {
    logger.debug("continue the loop [{}]", this);
    ctx.setBreakBlock(true);
  }
}
