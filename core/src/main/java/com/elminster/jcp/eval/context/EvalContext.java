package com.elminster.jcp.eval.context;

import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.exception.CallStack;
import com.elminster.jcp.exception.StackFrame;
import com.elminster.jcp.ast.statement.function.Function;

import java.util.Map;

public interface EvalContext {

  Map<String, Data> getVariables();

  Data getVariable(String name);

  void addVariable(Data variable);

  Map<String, Function> getFunctions();

  void addFunction(Function function);

  Function getFunction(String name);

  void addDataType(DataType dataType);

  DataType getDataType(String name);

  /**
   * Look up a registered type by its fully-qualified name (e.g. "java.util.Date").
   * Used during stub registration to avoid ambiguity from simple-name lookup.
   */
  DataType getDataTypeByFqn(String fqn);

  LoopContext getLoopContext();

  void setLoopContext(LoopContext loopContext);

  FastStack<EvalContext> getContextStack();

  boolean isReturn();

  void setReturn(boolean isReturn);

  /**
   * Returns the stack frame associated with this context.
   * Each function call context has its own stack frame for error reporting.
   *
   * @return the stack frame, or null for non-function contexts
   */
  StackFrame getStackFrame();

  /**
   * Sets the stack frame for this context.
   *
   * @param stackFrame the stack frame to associate with this context
   */
  void setStackFrame(StackFrame stackFrame);

  /**
   * Builds and returns the call stack by traversing the context stack.
   * This is computed on-demand from the stack frames stored in each context.
   *
   * @return the call stack built from context stack frames, never null
   */
  CallStack getCallStack();
}
