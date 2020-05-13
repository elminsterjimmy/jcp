package com.elminster.jcp.ast.func.module.system;

import com.elminster.jcp.ast.FunctionDefinition;
import com.elminster.jcp.module.ModuleFunction;

public class SystemModuleFunction extends ModuleFunction {

  public static final String SYSTEM_MODULE_NAME = "system";

  public SystemModuleFunction(FunctionDefinition functionDefinition) {
    super(functionDefinition);
  }

  @Override
  public String getModule() {
    return SYSTEM_MODULE_NAME;
  }
}
