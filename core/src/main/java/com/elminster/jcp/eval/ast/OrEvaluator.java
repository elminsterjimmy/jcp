package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.base.LogicalOrExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class OrEvaluator extends LogicalEvaluator {

  public OrEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    LogicalOrExpression orExpression = (LogicalOrExpression) astNode;
    boolean leftValue = evalBoolean(orExpression.getLeft(), evalContext);
    if (leftValue) {
      return BooleanFlowData.BOOLEAN_TRUE;
    }
    boolean rightValue = evalBoolean(orExpression.getRight(), evalContext);
    if (rightValue) {
      return BooleanFlowData.BOOLEAN_TRUE;
    }
    return BooleanFlowData.BOOLEAN_FALSE;
  }
}
