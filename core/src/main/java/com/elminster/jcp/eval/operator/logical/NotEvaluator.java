package com.elminster.jcp.eval.operator.logical;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.operation.LogicalNotExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class NotEvaluator extends LogicalEvaluator {

  public NotEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LogicalNotExpression notExpression = (LogicalNotExpression) astNode;
    boolean value = evalBoolean(notExpression.getExpression(), evalContext);
    return new BooleanData(!value);
  }
}
