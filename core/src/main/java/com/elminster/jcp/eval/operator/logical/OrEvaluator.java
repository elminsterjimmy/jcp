package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.LogicalOrExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class OrEvaluator extends LogicalEvaluator {

  public OrEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LogicalOrExpression orExpression = (LogicalOrExpression) astNode;
    boolean leftValue = evalBoolean(orExpression.getLeft(), evalContext);
    if (leftValue) {
      return BooleanData.BOOLEAN_TRUE;
    }
    boolean rightValue = evalBoolean(orExpression.getRight(), evalContext);
    if (rightValue) {
      return BooleanData.BOOLEAN_TRUE;
    }
    return BooleanData.BOOLEAN_FALSE;
  }
}
