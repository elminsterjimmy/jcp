package com.elminster.jcp.eval.context;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.ast.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.ast.statement.Function;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EvalContextImpl implements EvalContext {

  private Map<String, FlowData> variables = new HashMap<>();
  private Map<String, Function> functions = new HashMap<>();
  private LoopContext loopContext;

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, FlowData> getVariables() {
    return variables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FlowData getVariable(String name) {
    return variables.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addVariable(FlowData variable) {
    String id = variable.getName();
    if (variables.containsKey(id)) {
      AlreadyDeclaredException.throwAlreadyDeclaredVariableException(id);
    }
    variables.put(id, variable);
  }

  @Override
  public void setVariables(Map<String, FlowData> variables) {
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
    String id = function.getId();
    if (functions.containsKey(id)) {
      AlreadyDeclaredException.throwAlreadyDeclaredFunctionException(id);
    }
    functions.put(id, function);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Function getFunction(String name) {
    return functions.get(name);
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
