package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.operation.LogicalAndExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class AndEvaluator extends LogicalEvaluator {

  public AndEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LogicalAndExpression andExpression = (LogicalAndExpression) astNode;
    boolean leftValue = evalBoolean(andExpression.getLeft(), evalContext);
    if (!leftValue) {
      return BooleanData.BOOLEAN_FALSE;
    }
    boolean rightValue = evalBoolean(andExpression.getRight(), evalContext);
    if (!rightValue) {
      return BooleanData.BOOLEAN_FALSE;
    }
    return BooleanData.BOOLEAN_TRUE;
  }
}
