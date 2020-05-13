package com.elminster.jcp.ast;

import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

public class FunctionDefinition {
  final String id;
  final FlowData[] parameters;
  final DataType returnType;

  public FunctionDefinition(String id, FlowData... parameters) {
    this(id, DataType.ANY, parameters);
  }

  public FunctionDefinition(String id, DataType dataType) {
    this(id, dataType, new FlowData[0]);
  }

  public FunctionDefinition(String id) {
    this(id, DataType.ANY, new FlowData[0]);
  }

  public FunctionDefinition(String id, DataType returnType, FlowData... parameters) {
    this.id = id;
    this.parameters = parameters;
    this.returnType = returnType;
  }
}
