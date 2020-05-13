package com.elminster.jcp.ast.statement.control;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;

public class BreakStatement extends ControlStatement {

  @Override
  public String getName() {
    return "BREAK";
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    LoopContext loopContext = evalContext.getLoopContext();
    Assert.notNull(loopContext);
    loopContext.getLoopStatement().doBreak(evalContext);
    return AnyFlowData.EMPTY;
  }
}
