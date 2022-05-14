package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.module.Modulable;
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
    Expression expression = methodCallExpression.getExpression();
    Evaluable expressionEval = AstEvaluatorFactory.getEvaluator(expression);
    Data data = expressionEval.eval(evalContext);
    Expression[] arguments = methodCallExpression.getArguments();
    Data[] parameters = new Data[arguments.length + 1];
    parameters[0] = data;
    int i = 1;
    for (Expression arg : arguments) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arg);
      parameters[i++] = evaluable.eval(evalContext);
    }

    // module supports
    String moduleName = "";
    if (data.getDataType() instanceof Modulable) {
      moduleName = ((Modulable) data.getDataType()).getModule();
    }

    String functionName = FunctionUtils.getModuleFunctionName(
            moduleName, data.getDataType().getName(), methodName);

    String functionFullName = FunctionUtils.generateFunctionFullName(moduleName,
                    data.getDataType().getName(),
                    methodName, parameters);
    Function function = evalContext.getFunction(functionFullName);
    if (null == function) {
      DataType[] dataTypes = Arrays.stream(parameters).map(
              parameter -> parameter.getDataType()
      ).toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(Identifier.fromName(functionName), dataTypes);
    }
    function.setArguments(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    Data rtn = evaluable.eval(evalContext);
    return rtn;
  }
}