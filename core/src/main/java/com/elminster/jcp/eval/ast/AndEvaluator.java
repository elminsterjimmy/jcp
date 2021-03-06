package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.LogicalAndExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class AndEvaluator extends LogicalEvaluator {

  public AndEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
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
