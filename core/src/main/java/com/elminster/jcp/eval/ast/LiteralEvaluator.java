package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;

public class ConstantEvaluator extends AbstractAstEvaluator {

  public ConstantEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LiteralExpression literalExpression = (LiteralExpression) astNode;
    return literalExpression.getValue();
  }
}
