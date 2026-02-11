package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.statement.function.Function;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.StackFrame;
import com.elminster.jcp.module.base.BaseModuleRegister;
import com.elminster.jcp.util.ClassConverter;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Iterator;
import java.util.List;

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
  public boolean isReturn() {
    return getContextStack().peek().isReturn();
  }

  @Override
  public void setReturn(boolean isReturn) {
      getContextStack().peek().setReturn(isReturn);
  }

  /**
   * Builds a CallStack on-demand by traversing the context stack.
   * Each context's StackFrame is collected to form the complete call stack.
   *
   * @return a new CallStack built from context stack frames
   */
  @Override
  public CallStack getCallStack() {
    CallStack callStack = new CallStack();
    if (getContextStack().isEmpty()) {
      return callStack;
    }
    // Iterate from bottom (oldest) to top (newest) and push each frame
    Iterator<EvalContext> iterator = getContextStack().iterator();
    while (iterator.hasNext()) {
      EvalContext ctx = iterator.next();
      StackFrame frame = ctx.getStackFrame();
      if (frame != null) {
        callStack.push(frame);
      }
    }
    return callStack;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
