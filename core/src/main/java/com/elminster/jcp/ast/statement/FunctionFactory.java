package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

public class FunctionFactory {

  public static Function createFunction(FunctionDef functionDef, Statement... body) {
    return new AbstractFunction(functionDef, body);
  }

  public static Function createFunction(String id, FlowData[] parameters, DataType returnType, Statement... body) {
    return new AbstractFunction(id, parameters, returnType, body);
  }

  public static Function createFunction(String id, DataType returnType, Statement... body) {
    return createFunction(id, new FlowData[0], returnType, body);
  }

  public static Function createFunction(String id, Statement... body) {
    return createFunction(id, DataType.VOID, body);
  }

  public static Function createFunction(String id, FlowData[] parameters, Statement... body) {
    return createFunction(id, parameters, DataType.VOID, body);
  }
}
