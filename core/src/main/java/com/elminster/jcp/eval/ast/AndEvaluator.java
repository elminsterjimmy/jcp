package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.base.LogicalAndExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class AndEvaluator extends LogicalEvaluator {

  public AndEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    LogicalAndExpression andExpression = (LogicalAndExpression) astNode;
    boolean leftValue = evalBoolean(andExpression.getLeft(), evalContext);
    if (!leftValue) {
      return BooleanFlowData.BOOLEAN_FALSE;
    }
    boolean rightValue = evalBoolean(andExpression.getRight(), evalContext);
    if (!rightValue) {
      return BooleanFlowData.BOOLEAN_FALSE;
    }
    return BooleanFlowData.BOOLEAN_TRUE;
  }
}
