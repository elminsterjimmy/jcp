package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.ExpressionStatement;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class ExpressionStatementEvaluator extends AbstractAstEvaluator {
  public ExpressionStatementEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    ExpressionStatement expressionStatement = (ExpressionStatement) this.astNode;
    Expression expression = expressionStatement.getExpression();
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(expression);
    return evaluable.eval(evalContext);
  }
}
