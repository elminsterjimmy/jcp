package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.module.base.Assertions;
import com.elminster.jcp.module.base.BaseModuleRegister;
import com.elminster.jcp.module.base.ValueBuffer;
import com.elminster.jcp.util.ClassConverter;
import com.elminster.jcp.util.ModuleLoader;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RootEvalContext extends DefaultEvalContext {

  public RootEvalContext() {
    super();
    init();
  }

  private void init() {
    this.getContextStack().push(this); // as root context
    registerSystemDataTypes();
    registerSystemFunctions();
  }

  private void registerSystemFunctions() {
    List<Class<?>> classes = BaseModuleRegister.classToRegister();
    classes.forEach(clazz -> ClassConverter.registerClass(clazz, this, "base"));
  }

  private void registerSystemDataTypes() {
    for (DataType.SystemDataType systemDataType : DataType.SystemDataType.values()) {
      addDataType(systemDataType);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getVariable(String name) {
    Iterator<EvalContext> evalContextIterator = this.getContextStack().reverseIterator();
    while (evalContextIterator.hasNext()) {
      EvalContext evalContext = evalContextIterator.next();
      if (evalContext == this) { // hit root
        return super.getVariable(name);
      }
      Data variable = evalContext.getVariable(name);
      if (null != variable) {
        return variable;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Function getFunction(String name) {
    Iterator<EvalContext> evalContextIterator = this.getContextStack().reverseIterator();
    while (evalContextIterator.hasNext()) {
      EvalContext evalContext = evalContextIterator.next();
      if (evalContext == this) { // hit root
        return super.getFunction(name);
      }
      Function function = evalContext.getFunction(name);
      if (null != function) {
        return function;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "RootEvalContext{}";
  }
}
