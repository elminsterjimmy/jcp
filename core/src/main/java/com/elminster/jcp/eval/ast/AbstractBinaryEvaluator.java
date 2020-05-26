package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.AbstractBinaryExpression;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

abstract public class AbstractBinaryEvaluator extends AbstractAstEvaluator {

  public AbstractBinaryEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) throws Exception {
    AbstractBinaryExpression binaryExpression = (AbstractBinaryExpression) astNode;
    Data left = AstEvaluatorFactory.getEvaluator(binaryExpression.getLeft()).eval(evalContext);
    Data right = AstEvaluatorFactory.getEvaluator(binaryExpression.getRight()).eval(evalContext);
    Data result = doBinaryOp(left, right);
    return result;
  }

  protected abstract Data doBinaryOp(Data leftOperand, Data rightOperand);
}
