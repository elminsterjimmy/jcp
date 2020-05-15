package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;
import com.elminster.jcp.eval.context.LoopContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class LoopEvaluator extends ControlEvaluator {

  private static Logger logger = LoggerFactory.getLogger(LoopEvaluator.class);

  protected LoopContext ctx;
  protected boolean isBreak;

  public LoopEvaluator(Node astNode) {
    super(astNode);
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
