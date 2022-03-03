package com.elminster.jcp.module.system;

import com.elminster.jcp.module.AbstractModuleFunction;

abstract public class SystemModuleFunction extends AbstractModuleFunction {

  public static final String SYSTEM_MODULE_NAME = "system";

  @Override
  public String getModule() {
    return SYSTEM_MODULE_NAME;
  }
}
