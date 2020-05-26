package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataFactory;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.util.DataTypeUtil;

public class LiteralEvaluator extends AbstractAstEvaluator {

  public LiteralEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    LiteralExpression literalExpression = (LiteralExpression) astNode;
    return (Data) literalExpression.getValue();
  }
}
