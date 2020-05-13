package com.elminster.jcp.module;

import com.elminster.jcp.ast.AbstractFunction;
import com.elminster.jcp.ast.FunctionDefinition;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;

abstract public class ModuleFunction extends AbstractFunction implements Modulable {

  public ModuleFunction(FunctionDefinition functionDefinition) {
    super(functionDefinition);
  }

  public ModuleFunction(String id, FlowData[] parameters, DataType resultDataType) {
    super(id, parameters, resultDataType);
  }
}
