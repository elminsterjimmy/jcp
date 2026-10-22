package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.ast.statement.function.ParameterDef;
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
import java.util.List;
import java.util.stream.Collectors;

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
    String signature = FunctionUtils.generateSignature(
        moduleName, data.getDataType().getFqn(), methodName, parameters);

    Function function = evalContext.getFunction(signature);
    if (null == function) {
      // Exact signature miss — fall back to compatibility-based search.
      // This handles cases like put(Object, Object) called with (String, String)
      // where the registered signature uses ANY but the call site has concrete types.
      String baseName = FunctionUtils.getModuleFunctionName(
          moduleName, data.getDataType().getFqn(), methodName);
      List<Function> candidates = evalContext.getFunctions().values().stream()
          .filter(f -> f.getSignature() != null && f.getSignature().startsWith(baseName + FunctionUtils.PARAM_SPLITTER))
          .filter(f -> isParameterCompatible(parameters, f.getParameterDefs()))
          .collect(Collectors.toList());
      if (candidates.size() == 1) {
        function = candidates.get(0);
      } else if (candidates.size() > 1) {
        // Prefer exact match over widening
        List<Function> exact = candidates.stream()
            .filter(f -> DataType.allExactMatch(
                Arrays.stream(parameters).map(Data::getDataType).toArray(DataType[]::new),
                Arrays.stream(f.getParameterDefs()).map(ParameterDef::getDataType).toArray(DataType[]::new)))
            .collect(Collectors.toList());
        if (exact.size() == 1) {
          function = exact.get(0);
        } else {
          DataType[] dataTypes = Arrays.stream(parameters)
              .map(Data::getDataType)
              .toArray(DataType[]::new);
          UndeclaredException.throwFunctionUndeclaredException(
              Identifier.fromName(functionName), dataTypes);
        }
      }
    }
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

  private boolean isParameterCompatible(Data[] arguments, ParameterDef[] parameterDefs) {
    if (parameterDefs == null || parameterDefs.length != arguments.length) {
      return false;
    }
    for (int i = 0; i < parameterDefs.length; i++) {
      DataType argType = arguments[i].getDataType();
      DataType paramType = parameterDefs[i].getDataType();
      if (!argType.isCompatibleWith(paramType)) {
        return false;
      }
    }
    return true;
  }
}
