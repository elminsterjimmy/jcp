package com.elminster.jcp.ast.statement;

import com.elminster.common.util.CollectionUtil;
import com.elminster.jcp.ast.Statement;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.eval.context.EvalContext;

public class FunctionDeclarationImpl extends BlockImpl implements FunctionDeclaration {

  private String id;
  private DataType returnType;
  private FlowData[] parameterDefines;

  @Override
  public FlowData[] getParameterDefines() {
    return parameterDefines;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public DataType getDataType() {
    return returnType;
  }

  @Override
  public FlowData eval(EvalContext evalContext) {
    Function function = new AbstractFunction(id, parameterDefines, returnType,
        (Statement[]) CollectionUtil.collection2Array(getBody()));
    evalContext.addFunction(function);
    return AnyFlowData.EMPTY;
  }

  @Override
  public String getName() {
    return "FUNCTION_DECLARATION";
  }
}
