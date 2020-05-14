package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;
import com.elminster.jcp.eval.context.EvalContext;

public class LogicalOrExpression extends LogicalExpression {

  private Expression left;
  private Expression right;

  public LogicalOrExpression(Expression left, Expression right) {
    super(LogicalOperator.OR);
    this.left = left;
    this.right = right;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    boolean leftValue = evalBoolean(left, evalContext);
    if (leftValue) {
      return BooleanFlowData.BOOLEAN_TRUE;
    }
    boolean rightValue = evalBoolean(right, evalContext);
    if (rightValue) {
      return BooleanFlowData.BOOLEAN_TRUE;
    }
    return BooleanFlowData.BOOLEAN_FALSE;
  }
}
