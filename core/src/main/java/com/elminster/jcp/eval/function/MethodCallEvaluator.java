package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.ast.expression.base.MethodCallExpression;
import com.elminster.jcp.eval.data.StructData;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.module.Modulable;
import com.elminster.jcp.util.FunctionUtils;

import java.util.Arrays;

/**
 * Evaluator for method calls: obj.method(args)
 *
 * <p>All methods (user-defined type methods AND module methods) are looked up
 * via the function system using pattern: module::TypeName.methodName#paramTypes
 *
 * <p>For user-defined types, module defaults to "user".
 * For instance methods, 'this' (the instance) is passed as the first argument.
 */
public class MethodCallEvaluator extends AbstractAstEvaluator {

  private static final String USER_MODULE = "user";

  public MethodCallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    MethodCallExpression methodCallExpression = (MethodCallExpression) astNode;
    String methodName = methodCallExpression.getMethodName();
    Expression expression = methodCallExpression.getExpression();

    // Evaluate target object
    Evaluable expressionEval = AstEvaluatorFactory.getEvaluator(expression);
    Data data = expressionEval.eval(evalContext);

    // Build arguments: prepend 'this' (data) as first argument
    Expression[] arguments = methodCallExpression.getArguments();
    Data[] parameters = new Data[arguments.length + 1];
    parameters[0] = data;  // 'this' is first parameter
    for (int i = 0; i < arguments.length; i++) {
      Evaluable evaluable = AstEvaluatorFactory.getEvaluator(arguments[i]);
      parameters[i + 1] = evaluable.eval(evalContext);
    }

    // Determine module
    String moduleName;
    if (data instanceof StructData) {
      moduleName = USER_MODULE;
    } else if (data.getDataType() instanceof com.elminster.jcp.eval.data.ExternalClassType) {
      moduleName = ((com.elminster.jcp.eval.data.ExternalClassType) data.getDataType()).getModule();
    } else {
      moduleName = USER_MODULE;
    }

    // Look up function using full qualified name pattern
    String functionName = FunctionUtils.getModuleFunctionName(
        moduleName, data.getDataType().getFqn(), methodName);
    String functionFullName = FunctionUtils.generateFunctionFullName(
        moduleName, data.getDataType().getFqn(), methodName, parameters);

    Function function = evalContext.getFunction(functionFullName);
    if (null == function) {
      DataType[] dataTypes = Arrays.stream(parameters)
          .map(Data::getDataType)
          .toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(
          Identifier.fromName(functionName), dataTypes);
    }

    function.setArguments(parameters);
    Evaluable evaluable = AstEvaluatorFactory.getEvaluator(function);
    return evaluable.eval(evalContext);
  }
}