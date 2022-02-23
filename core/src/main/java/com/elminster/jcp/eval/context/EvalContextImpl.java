package com.elminster.jcp.eval.context;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.eval.data.Data;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.ast.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.ast.statement.Function;
import com.elminster.jcp.module.vb.ValueBuffer;
import com.elminster.jcp.util.ClassConverter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EvalContextImpl implements EvalContext {

  private Map<String, Data> variables = new HashMap<>();
  private Map<String, Function> functions = new HashMap<>();
  private Map<String, DataType> dataTypes = new HashMap<>();
  private LoopContext loopContext;

  public EvalContextImpl() {
    init();
  }

  private void init() {
    registerSystemDataTypes();
    registerSystemFunctions();
  }

  private void registerSystemFunctions() {
    ClassConverter.registerClass(ValueBuffer.class, this, "base");
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
  public Map<String, Data> getVariables() {
    return variables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Data getVariable(String name) {
    return variables.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addVariable(Data variable) {
    String id = variable.getIdentifier().getId();
    if (variables.containsKey(id)) {
      AlreadyDeclaredException.throwAlreadyDeclaredVariableException(variable.getIdentifier());
    }
    variables.put(id, variable);
  }

  @Override
  public void setVariables(Map<String, Data> variables) {
    Assert.notNull(variables);
    this.variables = variables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Function> getFunctions() {
    return Collections.unmodifiableMap(functions);
  }

  @Override
  public void addFunction(Function function) {
    Identifier id = function.getId();
    if (functions.containsKey(id.getId())) {
      AlreadyDeclaredException.throwAlreadyDeclaredFunctionException(id);
    }
    functions.put(id.getId(), function);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Function getFunction(String name) {
    return functions.get(name);
  }

  @Override
  public void addDataType(DataType dataType) {
    String name = dataType.getName();
    if (dataTypes.containsKey(name)) {
      AlreadyDeclaredException.throwAlreadyDeclaredDataTypeException(Identifier.fromName(name));
    }
    dataTypes.put(name, dataType);
  }

  @Override
  public DataType getDataType(String name) {
    String convertedName = convertSystemDataTypeName(name);
    return dataTypes.get(convertedName);
  }

  private String convertSystemDataTypeName(String name) {
    if ("Object".equals(name)) {
      return DataType.SystemDataType.ANY.getName();
    } else if ("int".equals(name)) {
      return DataType.SystemDataType.INT.getName();
    } else if ("boolean".equals(name)) {
      return DataType.SystemDataType.BOOLEAN.getName();
    } else if ("void".equals(name)) {
      return DataType.SystemDataType.VOID.getName();
    }
    return name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LoopContext getLoopContext() {
    return loopContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLoopContext(LoopContext loopContext) {
    this.loopContext = loopContext;
  }

  @Override
  public String toString() {
    return "FlowContextImpl{" +
        ", variables=" + variables +
        ", functions=" + functions +
        ", loopContext=" + loopContext +
        '}';
  }
}
