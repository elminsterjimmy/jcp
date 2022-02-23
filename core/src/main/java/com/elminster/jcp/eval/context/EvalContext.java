package com.elminster.jcp.eval.context;

import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.ast.statement.Function;

import javax.lang.model.type.TypeVariable;
import java.util.Map;

public interface EvalContext {

  Map<String, Data> getVariables();

  Data getVariable(String name);

  void addVariable(Data variable);

  void setVariables(Map<String, Data> variables);

  Map<String, Function> getFunctions();

  void addFunction(Function function);

  Function getFunction(String name);

  void addDataType(DataType dataType);

  DataType getDataType(String name);

  LoopContext getLoopContext();

  void setLoopContext(LoopContext loopContext);
}
