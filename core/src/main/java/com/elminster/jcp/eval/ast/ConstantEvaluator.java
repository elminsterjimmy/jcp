package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.ConstantExpression;
import com.elminster.jcp.eval.context.EvalContext;

public class ConstantEvaluator extends AbstractAstEvaluator {

  public ConstantEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    ConstantExpression constantExpression = (ConstantExpression) astNode;
    return constantExpression.getConstData();
  }
}
