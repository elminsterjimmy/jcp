package com.elminster.jcp.eval.ast;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.statement.control.BreakStatement;
import com.elminster.jcp.ast.statement.control.ContinueStatement;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.LoopContext;

import java.lang.reflect.Constructor;

public class ContinueEvaluator extends ControlEvaluator {
  public ContinueEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    LoopContext loopContext = evalContext.getLoopContext();
    Assert.notNull(loopContext);
    loopContext.getLoopStatement().doContinue(evalContext);
    return AnyFlowData.EMPTY;
  }
}
