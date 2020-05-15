package com.elminster.jcp.ast.func.test;

import com.elminster.common.util.Assert;
import com.elminster.jcp.ast.statement.AbstractFunction;
import com.elminster.jcp.ast.statement.FunctionDef;
import com.elminster.jcp.ast.data.AnyFlowData;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.ast.data.DataType;
import com.elminster.jcp.ast.data.FlowData;
import com.elminster.jcp.ast.data.FlowDataFactory;
import com.elminster.jcp.module.AbstractModuleFunction;
import com.elminster.jcp.module.ModuleFunction;

import javax.xml.crypto.Data;

public class LogFunction extends AbstractModuleFunction {

  public LogFunction() {
  }

  @Override
  public String getName() {
    return "LOG";
  }

  @Override
  protected FlowData doFunction(FlowData[] parameters) {
    Assert.isTrue(getParameters().length > 0);
    FlowData msg = getParameters()[0];
    System.out.println(msg.get());
    return AnyFlowData.EMPTY;
  }

  @Override
  public String getId() {
    return "log";
  }

  @Override
  public FlowData[] getParameterDefs() {
    return new FlowData[]{
        FlowDataFactory.INSTANCE.createFlowDataVariable("msg", DataType.STRING)
    };
  }

  @Override
  public DataType getResultDataType() {
    return DataType.VOID;
  }

  @Override
  public String getModule() {
    return null;
  }
}
