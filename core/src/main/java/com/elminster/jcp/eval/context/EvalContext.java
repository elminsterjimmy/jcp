package com.elminster.jcp.eval.context;

import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.statement.Function;

import java.util.Map;

public interface EvalContext {

  Map<String, FlowData> getVariables();

  FlowData getVariable(String name);

  void addVariable(FlowData variable);

  void setVariables(Map<String, FlowData> varaibles);

  Map<String, Function> getFunctions();

  void addFunction(Function function);

  Function getFunction(String name);

  LoopContext getLoopContext();

  void setLoopContext(LoopContext loopContext);
}
