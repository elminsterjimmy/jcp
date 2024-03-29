package com.elminster.jcp.eval.context;

import com.elminster.jcp.collection.FastStack;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
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

  LoopContext getLoopContext();

  void setLoopContext(LoopContext loopContext);

  FastStack<EvalContext> getContextStack();

  boolean isReturn();

  void setReturn(boolean isReturn);
}
