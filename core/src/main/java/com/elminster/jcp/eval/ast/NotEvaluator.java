package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.base.LogicalNotExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class NotEvaluator extends LogicalEvaluator {

  public NotEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    LogicalNotExpression notExpression = (LogicalNotExpression) astNode;
    boolean value = evalBoolean(notExpression.getExpression(), evalContext);
    return new BooleanFlowData(!value);
  }
}
