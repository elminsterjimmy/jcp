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
 * <p>Uses function lookup with pattern: module::typeFqn#methodName@paramFqn1@...
 * The simple type name from the AST is resolved to its FQN via the type registry
 * before building the key, so same-simple-name types are unambiguous.
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

    // Resolve simple type name to FQN via the type registry so the function key
    // is unambiguous even when two classes share the same simple name.
    DataType resolvedType = evalContext.getDataType(typeName);
    String typeFqn = resolvedType != null ? resolvedType.getFqn() : typeName;

    // Determine module: ExternalClassType carries its registered module name;
    // user/struct types default to "user".
    String moduleName;
    if (resolvedType instanceof com.elminster.jcp.eval.data.ExternalClassType) {
      moduleName = ((com.elminster.jcp.eval.data.ExternalClassType) resolvedType).getModule();
    } else {
      moduleName = USER_MODULE;
    }

    String functionName = FunctionUtils.getModuleFunctionName(moduleName, typeFqn, methodName);
    String functionFullName = FunctionUtils.generateFunctionFullName(moduleName, typeFqn, methodName, args);

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
