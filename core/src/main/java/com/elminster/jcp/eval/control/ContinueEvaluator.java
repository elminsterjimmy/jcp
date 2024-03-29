package com.elminster.jcp.eval.control;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;

public class ContinueEvaluator extends ControlEvaluator {
  public ContinueEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LoopContext loopContext = evalContext.getLoopContext();
    Assert.notNull(loopContext);
    loopContext.getLoopStatement().doContinue(evalContext);
    return AnyData.EMPTY;
  }
}
