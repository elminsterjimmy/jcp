package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class FuncallEvaluator extends AbstractAstEvaluator {

  public FuncallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FunctionCallExpression functionCallExpression = (FunctionCallExpression) astNode;
    String id = functionCallExpression.getId();
    Expression[] argurements = functionCallExpression.getArgurements();
    Function function = evalContext.getFunction(id);
    if (null == function) {
      UndeclaredException.throwUndeclaredFunctionException(id);
    }
    FlowData[] parameters = new FlowData[argurements.length];
    int i = 0;
    for (Expression arg : argurements) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }
    function.setParameters(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    FlowData data = evaluable.eval(evalContext);
    return data;
  }
}