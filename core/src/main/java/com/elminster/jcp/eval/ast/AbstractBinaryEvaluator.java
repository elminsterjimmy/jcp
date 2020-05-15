package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.expression.base.AbstractBinaryExpression;
import com.elminster.jcp.ast.statement.AbstractFunction;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

abstract public class AbstractBinaryEvaluator extends AbstractAstEvaluator {

  public AbstractBinaryEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    AbstractBinaryExpression binaryExpression = (AbstractBinaryExpression) astNode;
    FlowData left = AstEvaluatorFactory.getEvaluator(binaryExpression.getLeft()).eval(evalContext);
    FlowData right = AstEvaluatorFactory.getEvaluator(binaryExpression.getRight()).eval(evalContext);
    FlowData result = doBinaryOp(left, right);
    return result;
  }

  protected abstract FlowData doBinaryOp(FlowData leftOperand, FlowData rightOperand);
}
