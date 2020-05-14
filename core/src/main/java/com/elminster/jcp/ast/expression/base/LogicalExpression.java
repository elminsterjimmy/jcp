package com.elminster.jcp.ast.expression.base;

import com.elminster.jcp.ast.data.BooleanFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.AbstractExpression;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.expression.operator.LogicalOperator;
import com.elminster.jcp.eval.context.EvalContext;

abstract public class LogicalExpression extends AbstractExpression {

  protected final LogicalOperator operator;

  public LogicalExpression(LogicalOperator operator) {
    this.operator = operator;
  }

  public LogicalOperator getOperator() {
    return operator;
  }

  protected boolean evalBoolean(Expression expr, EvalContext evalContext) {
    FlowData data = expr.eval(evalContext);
    if (!(data instanceof BooleanFlowData)) {
      throw new CannotCastException(data.getDataType(), DataType.BOOLEAN);
    }
    Boolean value = ((BooleanFlowData)data).get();
    if (null == value) {
      throw new NullPointerException();
    }
    return value.booleanValue();
  }

  @Override
  public String getName() {
    return operator.getName();
  }
}
