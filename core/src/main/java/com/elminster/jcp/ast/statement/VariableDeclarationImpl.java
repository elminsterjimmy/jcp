package com.elminster.jcp.ast.statement;

import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;
import com.elminster.jcp.ast.excpetion.CannotCastException;
import com.elminster.jcp.ast.Expression;
import com.elminster.jcp.ast.excpetion.AlreadyDeclaredException;
import com.elminster.jcp.eval.context.EvalContext;

public class VariableDeclarationImpl extends AbstractStatement implements VariableDeclaration {

  private String id;
  private Expression initExpress;
  private DataType dataType;

  public VariableDeclarationImpl(String id, DataType dataType) {
    this(id, dataType, null);
  }

  public VariableDeclarationImpl(String id, DataType dataType, Expression initExpress) {
    this.id = id;
    this.dataType = dataType;
    this.initExpress = initExpress;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    return dataType;
  }

  @Override
  public Expression getInit() {
    return initExpress;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    FlowData flowData = evalContext.getVariable(id);
    if (null != flowData) {
      AlreadyDeclaredException.throwAlreadyDeclaredVariableException(id);
    }
    FlowData variable = FlowDataFactory.INSTANCE.createFlowDataVariable(id, dataType);
    evalContext.addVariable(variable);
    if (null != initExpress) {
      FlowData data = initExpress.eval(evalContext);
      if (!data.getDataType().isCastableTo(variable.getDataType())) {
        throw new CannotCastException(data.getDataType(), variable.getDataType());
      }
      variable.set(data.get());
    }
    return AnyFlowData.EMPTY;
  }

  @Override
  public String getName() {
    return "VARIABLE_DECLARATION";
  }
}
