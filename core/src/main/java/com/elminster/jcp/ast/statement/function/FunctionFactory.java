package com.elminster.jcp.ast.statement.function;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.eval.data.DataType;

public class FunctionFactory {

  public static Function createFunction(FunctionDef functionDef, Statement... body) {
    return new AbstractFunction(functionDef, body);
  }

  public static Function createFunction(String id, ParameterDef[] parameters, DataType returnType, Statement... body) {
    return new AbstractFunction(new IdentifierExpression(id), parameters, returnType, body);
  }

  public static Function createFunction(String id, DataType returnType, Statement... body) {
    return createFunction(id, new ParameterDef[0], returnType, body);
  }

  public static Function createFunction(String id, Statement... body) {
    return createFunction(id, DataType.SystemDataType.VOID, body);
  }

  public static Function createFunction(String id, ParameterDef[] parameters, Statement... body) {
    return createFunction(id, parameters, DataType.SystemDataType.VOID, body);
  }
}
