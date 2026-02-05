package com.elminster.jcp.eval.function;

import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.Evaluable;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.excpetion.UndeclaredException;
import com.elminster.jcp.eval.factory.AstEvaluatorFactory;
import com.elminster.jcp.util.FunctionUtils;

import java.util.Arrays;

/**
 * Evaluator for static method calls: Type.method(args)
 * Static methods do not have 'this' - they're looked up by qualified name only.
 *
 * <p>Uses function lookup with pattern: user::TypeName.methodName#paramTypes
 */
public class StaticMethodCallEvaluator extends AbstractAstEvaluator {

  private static final String USER_MODULE = "user";

  public StaticMethodCallEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    StaticMethodCallExpression expr = (StaticMethodCallExpression) astNode;
    String typeName = expr.getTypeName().getId();
    String methodName = expr.getMethodName();
    Expression[] argExprs = expr.getArguments();

    // Evaluate arguments (NO 'this' for static methods)
    Data[] args = new Data[argExprs.length];
    for (int i = 0; i < argExprs.length; i++) {
      args[i] = AstEvaluatorFactory.getEvaluator(argExprs[i]).eval(evalContext);
    }

    // Look up function using full qualified name pattern
    // For user-defined types, use "user" module
    String functionName = FunctionUtils.getModuleFunctionName(USER_MODULE, typeName, methodName);
    String functionFullName = FunctionUtils.generateFunctionFullName(
        USER_MODULE, typeName, methodName, args);

    Function function = evalContext.getFunction(functionFullName);
    if (null == function) {
      DataType[] types = Arrays.stream(args).map(Data::getDataType).toArray(DataType[]::new);
      UndeclaredException.throwFunctionUndeclaredException(
          Identifier.fromName(functionName), types);
    }

    function.setArguments(args);
    return AstEvaluatorFactory.getEvaluator(function).eval(evalContext);
  }
}
