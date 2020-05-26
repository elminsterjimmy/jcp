package com.elminster.jcp.eval.ast;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;

public class BreakEvaluator extends ControlEvaluator {
  public BreakEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LoopContext loopContext = evalContext.getLoopContext();
    Assert.notNull(loopContext);
    loopContext.getLoopStatement().doBreak(evalContext);
    return AnyData.EMPTY;
  }
}
