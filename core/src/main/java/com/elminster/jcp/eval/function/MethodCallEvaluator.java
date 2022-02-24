package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.FunctionUtils;

import java.util.Arrays;

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
    Data[] parameters = new Data[arguments.length + 1];
    parameters[0] = data;
    int i = 1;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }

    String functionName = data.getDataType().getName() + "$" + FunctionUtils
            .generateFunctionFullName(Identifier.fromName(methodName), parameters);
    Function function = evalContext.getFunction(functionName);
    if (null == function) {
      DataType[] dataTypes = Arrays.stream(parameters).map(
              parameter -> parameter.getDataType()
      ).toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(Identifier.fromName(methodName), dataTypes);
    }
    function.setArguments(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    Data rtn = evaluable.eval(evalContext);
    return rtn;
  }
}