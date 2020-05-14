package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

public class FunctionDef {

  final String id;
  final FlowData[] parameters;
  final DataType returnType;

  public FunctionDef(String id, FlowData... parameters) {
    this(id, DataType.ANY, parameters);
  }

  public FunctionDef(String id, DataType dataType) {
    this(id, dataType, new FlowData[0]);
  }

  public FunctionDef(String id) {
    this(id, DataType.ANY, new FlowData[0]);
  }

  public FunctionDef(String id, DataType returnType, FlowData... parameters) {
    this.id = id;
    this.parameters = parameters;
    this.returnType = returnType;
  }
}
