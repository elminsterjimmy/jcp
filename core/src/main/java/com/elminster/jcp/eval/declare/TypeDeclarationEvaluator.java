package com.elminster.jcp.eval.declare;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.Node;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.statement.declaration.MethodDef;
import com.elminster.jcp.ast.statement.declaration.StructDeclaration;
import com.elminster.jcp.ast.statement.function.AbstractFunction;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.base.AbstractAstEvaluator;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.data.AnyData;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.StructType;
import com.elminster.jcp.util.FunctionUtils;

/**
 * Evaluator for type declarations (extended struct with methods).
 * Registers a new type in the evaluation context with its fields and constructor.
 *
 * <p>CRITICAL ARCHITECTURE: Methods are registered as functions using
 * the pattern: user::TypeName.methodName#paramTypes
 *
 * <p>This allows reuse of the existing function resolution system
 * including overload handling via isCastableTo().
 *
 * <p>For instance methods, 'this' is prepended as the first parameter.
 * For static methods, there is no implicit 'this' parameter.
 */
public class TypeDeclarationEvaluator extends AbstractAstEvaluator {

  private static final String USER_MODULE = "user";

  public TypeDeclarationEvaluator(Node astNode) {
    super(astNode);
  }

  @Override
  public Data eval(EvalContext evalContext) {
    StructDeclaration decl = (StructDeclaration) astNode;
    String typeName = decl.getId().getId();

    // 1. Create and register the type (constructor and fields only)
    StructType structType = new StructType(
        typeName,
        decl.getFields(),
        decl.getConstructor()
    );
    evalContext.addDataType(structType);

    // 2. Register instance methods as functions
    // Pattern: user::TypeName.methodName#TypeName@paramTypes
    // Instance methods get 'this' (type) as first parameter
    for (MethodDef method : decl.getInstanceMethods()) {
      registerInstanceMethod(evalContext, typeName, method, structType);
    }

    // 3. Register static methods as functions
    // Pattern: user::TypeName.methodName#paramTypes
    // Static methods have NO 'this' parameter
    for (MethodDef method : decl.getStaticMethods()) {
      registerStaticMethod(evalContext, typeName, method);
    }

    return AnyData.EMPTY;
  }

  /**
   * Register instance method as a function.
   * Instance methods have 'this' (the type) as first parameter.
   * Function name follows pattern: methodName (lookup uses module::type.method)
   */
  private void registerInstanceMethod(EvalContext context, String typeName,
                                       MethodDef method, StructType type) {
    // Build parameter list: prepend 'this' (type) to method params
    ParameterDef[] methodParams = method.getParameters() != null
        ? method.getParameters()
        : new ParameterDef[0];
    ParameterDef[] funcParams = new ParameterDef[methodParams.length + 1];
    funcParams[0] = ParameterDef.of("this", type);  // 'this' is first param
    System.arraycopy(methodParams, 0, funcParams, 1, methodParams.length);

    // Get body statements
    Statement[] bodyStatements = method.getBody().getBody().toArray(new Statement[0]);

    // Use the full qualified name: user::TypeName.methodName
    String qualifiedName = FunctionUtils.getModuleFunctionName(USER_MODULE, typeName, method.getId().getId());

    // Create function using existing AbstractFunction
    AbstractFunction func = new AbstractFunction(
        Identifier.fromName(qualifiedName),
        funcParams,
        method.getReturnType(),
        bodyStatements
    );

    context.addFunction(func);
  }

  /**
   * Register static method as a function.
   * No 'this' parameter for static methods.
   */
  private void registerStaticMethod(EvalContext context, String typeName,
                                     MethodDef method) {
    ParameterDef[] methodParams = method.getParameters() != null
        ? method.getParameters()
        : new ParameterDef[0];

    Statement[] bodyStatements = method.getBody().getBody().toArray(new Statement[0]);

    // Use the full qualified name: user::TypeName.methodName
    String qualifiedName = FunctionUtils.getModuleFunctionName(USER_MODULE, typeName, method.getId().getId());

    AbstractFunction func = new AbstractFunction(
        Identifier.fromName(qualifiedName),
        methodParams,
        method.getReturnType(),
        bodyStatements
    );

    context.addFunction(func);
  }
}
