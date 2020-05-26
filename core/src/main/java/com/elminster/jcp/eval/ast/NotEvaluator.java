package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.BooleanData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.LogicalNotExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class NotEvaluator extends LogicalEvaluator {

  public NotEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    LogicalNotExpression notExpression = (LogicalNotExpression) astNode;
    boolean value = evalBoolean(notExpression.getExpression(), evalContext);
    return new BooleanData(!value);
  }
}
