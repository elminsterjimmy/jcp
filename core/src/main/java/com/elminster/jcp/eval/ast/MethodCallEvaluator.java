package com.elminster.jcp.eval.ast;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.ast.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;

public class MethodCallEvaluator extends AbstractAstEvaluator {

  public MethodCallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    MethodCallExpression methodCallExpression = (MethodCallExpression) astNode;
    String methodName = methodCallExpression.getMethodName();
    Data data = methodCallExpression.getData();
    Expression[] arguments = methodCallExpression.getArgurements();
    String functionName = data.getDataType().getName() + "." + methodName;
    Function function = evalContext.getFunction(functionName);
    if (null == function) {
      UndeclaredException.throwUndeclaredFunctionException(Identifier.fromName(functionName));
    }
    Data[] parameters = new Data[arguments.length + 1];
    parameters[0] = data;
    int i = 1;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }
    function.setParameters(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    Data rtn = evaluable.eval(evalContext);
    return rtn;
  }
}