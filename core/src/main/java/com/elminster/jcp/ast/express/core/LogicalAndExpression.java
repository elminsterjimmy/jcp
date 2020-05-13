package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;
import com.elminster.jcp.eval.context.EvalContext;

public class LogicalAndExpression extends LogicalExpression {

  private Expression left;
  private Expression right;

  public LogicalAndExpression(Expression left, Expression right) {
    super(LogicalOperator.AND);
    this.left = left;
    this.right = right;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    boolean leftValue = evalBoolean(left, evalContext);
    if (!leftValue) {
      return BooleanFlowData.BOOLEAN_FALSE;
    }
    boolean rightValue = evalBoolean(right, evalContext);
    if (!rightValue) {
      return BooleanFlowData.BOOLEAN_FALSE;
    }
    return BooleanFlowData.BOOLEAN_TRUE;
  }
}
