package com.elminster.jcp.ast.statement.function;

import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.operation.IdentifierExpression;
import com.elminster.jcp.eval.data.DataType;

public class FunctionDef {

  final Identifier id;
  final ParameterDef[] parameters;
  final DataType returnType;

  public FunctionDef(String id, ParameterDef... parameters) {
    this(id, DataType.SystemDataType.ANY, parameters);
  }

  public FunctionDef(String id, DataType dataType) {
    this(id, dataType, new ParameterDef[0]);
  }

  public FunctionDef(String id) {
    this(id, DataType.SystemDataType.ANY, new ParameterDef[0]);
  }

  public FunctionDef(String id, DataType returnType, ParameterDef... parameters) {
    this(new IdentifierExpression(id), returnType, parameters);
  }

  public FunctionDef(Identifier id, DataType returnType, ParameterDef... parameters) {
    this.id = id;
    this.parameters = parameters;
    this.returnType = returnType;
  }
}
