package com.elminster.jcp.ast.express.core;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;
import com.elminster.jcp.eval.context.EvalContext;

public class LogicalNotExpression extends LogicalExpression {

  private Expression expression;

  public LogicalNotExpression(Expression expression) {
    super(LogicalOperator.NOT);
    this.expression = expression;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    boolean value = evalBoolean(expression, evalContext);
    return new BooleanFlowData(!value);
  }
}
